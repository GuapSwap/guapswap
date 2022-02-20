package dex.ergodex

import org.ergoplatform.appkit.{ErgoValue, InputBox, ErgoToken, Address, JavaHelpers, Iso}
import special.collection.Coll
import special.sigma.SigmaProp
import configs.parameters.GuapSwapParameters
import configs.parameters.protocol_settings.GuapSwapProtocolSettings
import configs.parameters.dex_settings.GuapSwapErgoDexSettings
import protocol.GuapSwapUtils
import sigmastate.serialization.ErgoTreeSerializer
import sigmastate.{SType, Values}
import org.ergoplatform.validation.{SigmaValidationSettings, ValidationRules}

/**
  * Class representing the parameters to be provided to the ErgoDexSwapSell contract.
  *
  * @param paramPK
  * @param paramFeeNum
  * @param paramQuoteId
  * @param paramMinQuoteAmount
  * @param paramBaseAmount
  * @param paramDexFeePerTokenNum
  * @param paramDexFeePerTokenDenom
  * @param paramMaxMinerFee
  * @param paramPoolNFTId
  */
case class ErgoDexSwapSellParams(
    val paramPK: ErgoValue[SigmaProp],
    val paramFeeNum: ErgoValue[Long],
    val paramQuoteId: ErgoValue[Coll[Byte]],
    val paramMinQuoteAmount: ErgoValue[Long],
    val paramBaseAmount: ErgoValue[Long],
    val paramDexFeePerTokenNum: ErgoValue[Long],
    val paramDexFeePerTokenDenom: ErgoValue[Long],
    val paramMaxMinerFee: ErgoValue[Long],
    val paramPoolNFTId: ErgoValue[Coll[Byte]]
)

/**
  * Object representing methods associated with an ErgoDex SwapSell.
  */
object ErgoDexSwapSellParams {

  /**
    * Calculate the swap parameters to be inserted as context variables.
    * 
    * @param proxyBox
    * @param poolBox
    * @param parameters
    * @return Parameters to be inserted as context variables with correct ErgoScript types.
    */
  def swapSellParams(parameters: GuapSwapParameters, poolBox: InputBox, proxyBoxes: List[InputBox]): ErgoDexSwapSellParams = {

    // Get the PoolNFT token and the yAsset token
    val poolNFT: ErgoToken = poolBox.getTokens().get(0)
    val yAsset: ErgoToken = poolBox.getTokens().get(2)

    // PoolFeeNum: Long => R4 value of poolBox
    val poolFeeNum: Long = poolBox.getRegisters().get(0).getValue().toString().toLong

    // xAmount of ERG and yAmount of token in LP
    val xAmount: Long = poolBox.getValue()
    val yAmount: Long = yAsset.getValue()

    // Get access to the parameter settings determined in the guapswap_config.json file
    val guapswapSettings: GuapSwapProtocolSettings = parameters.guapswapProtocolSettings
    val ergodexSettings: GuapSwapErgoDexSettings = parameters.dexSettings.ergodexSettings

    // Calculate total payout
    val payout: Long = proxyBoxes.foldLeft(0L)((acc, proxybox) => acc + proxybox.getValue())
        
    // Calculate the guapswap fees
    val guapswapTotalProtocolFee: Long = GuapSwapUtils.calculateTotalProtocolFee(guapswapSettings.serviceFees.protocolFeePercentage, guapswapSettings.serviceFees.protocolUIFeePercentage, payout)
    val guapswapMinerFee: Long = GuapSwapUtils.convertMinerFee(guapswapSettings.serviceFees.protocolMinerFee)
    val guapswapServiceFee: Long = GuapSwapUtils.calculateServiceFee(guapswapTotalProtocolFee, guapswapMinerFee)
        
    // Calculate the ergodex fees
    val ergodexMinerFee: Long = GuapSwapUtils.convertMinerFee(ergodexSettings.ergodexMinerFee)
    val ergodexMinExecutionFee: Long = ErgoDexUtils.calculateMinExecutionFee(ergodexMinerFee)
    val minValueOfTotalErgoDexFees: Long = ErgoDexUtils.minValueOfTotalErgoDexFees(ergodexMinExecutionFee, ergodexMinerFee)
        
    // Minimum value of total fees
    val minValueOfTotalFees: Long = GuapSwapUtils.minValueOfTotalFees(guapswapServiceFee, minValueOfTotalErgoDexFees)
        
    // Caluclate the base amount to be swapped
    val baseAmount: Long = GuapSwapUtils.calculateBaseAmount(payout, minValueOfTotalFees)

    // Calculate the minimum quote amount for the given input swap base amount
    val minQuoteAmount: Long = ErgoDexSwap.calculateMinOutputAmount(baseAmount, ergodexSettings.slippageTolerancePercentage, xAmount, yAmount, poolFeeNum, ErgoDexUtils.POOL_FEE_DENOM)

    // Calculate the swap extremum values
    val swapExtemums: (Long, (Long, Long, Long, Long)) = ErgoDexSwap.swapExtremums(ergodexMinExecutionFee, ergodexSettings.nitro, minQuoteAmount)

    // Calculate execution fee numerator and denominator
    val dexFeePerTokenFraction: (Long, Long) = GuapSwapUtils.decimalToFraction(swapExtemums._1)

    // Calculate the new PK with the added hex prefix
    //val newPK: String = ErgoDexUtils.addPKAddressPrefix(guapswapSettings.userAddress)

    // Converts all value types into ErgoValue types
    val paramPK:                    ErgoValue[SigmaProp]    =   ErgoValue.of(Address.create(guapswapSettings.userAddress).asP2PK().pubkey)
    val paramPoolFeeNum:            ErgoValue[Long]          =   ErgoValue.of(poolFeeNum)
    val paramQuoteId:               ErgoValue[Coll[Byte]]   =   ErgoValue.of(yAsset.getId().getBytes())
    val paramMinQuoteAmount:        ErgoValue[Long]          =   ErgoValue.of(minQuoteAmount)
    val paramBaseAmount:            ErgoValue[Long]          =   ErgoValue.of(baseAmount)
    val paramDexFeePerTokenNum:     ErgoValue[Long]          =   ErgoValue.of(dexFeePerTokenFraction._1)
    val paramDexFeePerTokenDenom:   ErgoValue[Long]          =   ErgoValue.of(dexFeePerTokenFraction._2)
    val paramMaxMinerFee:           ErgoValue[Long]          =   ErgoValue.of(ergodexMinerFee)
    val paramPoolNFTId:             ErgoValue[Coll[Byte]]   =   ErgoValue.of(poolNFT.getId().getBytes())

    val swapsellparams = ErgoDexSwapSellParams(paramPK, paramPoolFeeNum, paramQuoteId, paramMinQuoteAmount, paramBaseAmount, paramDexFeePerTokenNum, paramDexFeePerTokenDenom, paramMaxMinerFee, paramPoolNFTId)
    swapsellparams
  }

  /**
    * Substitute the swap sell parameters into the sample contract.
    *
    * @param swapsellparams
    * @return New contract with updated context variables. 
    */
  def getSubstSwapSellContractWithParams(swapsellparams: ErgoDexSwapSellParams): ErgoValue[Coll[Byte]] = {
  
    // Get the ErgoTree bytes for the swap sell contract sample
    val DexSwapSellContractSample: Array[Byte] = JavaHelpers.decodeStringToBytes(ErgoDexUtils.ERGODEX_SWAPSELL_CONTRACT_SAMPLE)

    // Define the position arrays for the different constant locations
    val positions_Long:      Array[Int] = Array(2, 10, 11, 12, 14, 17, 18, 22)
    val positions_Coll_Byte: Array[Int] = Array(8, 9)
    val positions_SigmaProp: Array[Int] = Array(0)

    // Define the values arrays for the different SType constants
    val newValues_Long: Array[Values.Value[SType]] = Array(
      Iso.isoErgoValueToSValue.to(swapsellparams.paramBaseAmount),
      Iso.isoErgoValueToSValue.to(swapsellparams.paramMinQuoteAmount),
      Iso.isoErgoValueToSValue.to(swapsellparams.paramDexFeePerTokenNum),
      Iso.isoErgoValueToSValue.to(swapsellparams.paramDexFeePerTokenDenom),
      Iso.isoErgoValueToSValue.to(ErgoValue.of(swapsellparams.paramFeeNum.getValue().toInt)),
      Iso.isoErgoValueToSValue.to(swapsellparams.paramBaseAmount),
      Iso.isoErgoValueToSValue.to(ErgoValue.of(swapsellparams.paramFeeNum.getValue().toInt)),
      Iso.isoErgoValueToSValue.to(swapsellparams.paramMaxMinerFee)
    )

    val newValues_Coll_Byte: Array[Values.Value[SType]] = Array(
      Iso.isoErgoValueToSValue.to(swapsellparams.paramPoolNFTId),
      Iso.isoErgoValueToSValue.to(swapsellparams.paramQuoteId),
    )
    
    val newValues_SigmaProp: Array[Values.Value[SType]] = Array(
      Iso.isoErgoValueToSValue.to(swapsellparams.paramPK)
    )
    
    // Replace constants in the ErgoTree
    implicit val settings: SigmaValidationSettings = ValidationRules.currentSettings
    val newErgoDexSwapSellContractSample_Long:      Array[Byte] = ErgoTreeSerializer.DefaultSerializer.substituteConstants(DexSwapSellContractSample, positions_Long, newValues_Long)._1
    val newErgoDexSwapSellContractSample_Coll_Byte: Array[Byte] = ErgoTreeSerializer.DefaultSerializer.substituteConstants(newErgoDexSwapSellContractSample_Long, positions_Coll_Byte, newValues_Coll_Byte)._1
    val newErgoDexSwapSellContractSample_SigmaProp: Array[Byte] = ErgoTreeSerializer.DefaultSerializer.substituteConstants(newErgoDexSwapSellContractSample_Coll_Byte, positions_SigmaProp, newValues_SigmaProp)._1
    val newErgoDexSwapSellContractSample:           Array[Byte] = newErgoDexSwapSellContractSample_SigmaProp
    
    ErgoValue.of(newErgoDexSwapSellContractSample)
  }

}