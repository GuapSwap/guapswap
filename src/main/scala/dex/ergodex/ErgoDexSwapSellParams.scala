package dex.ergodex

import org.ergoplatform.appkit.{ErgoValue, InputBox, ErgoToken, Address, JavaHelpers, Iso}
import org.ergoplatform.validation.{SigmaValidationSettings, ValidationRules}

import special.collection.{Coll, Colls}
import special.sigma.SigmaProp

import configs.parameters.GuapSwapParameters
import configs.parameters.protocol_settings.GuapSwapProtocolSettings
import configs.parameters.dex_settings.GuapSwapErgoDexSettings
import protocol.GuapSwapUtils

import sigmastate.{SType, Values}
import sigmastate.Values.ErgoTree
import sigmastate.utxo.Deserialize
import sigmastate.serialization.ErgoTreeSerializer
import org.ergoplatform.appkit.Constants
import sigmastate.interpreter.ErgoTreeEvaluator
import org.ergoplatform.appkit.ErgoType
import org.ergoplatform.restapi.client.SigmaBoolean

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
    val paramFeeNum: ErgoValue[Int],
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
    val poolFeeNum: Int = poolBox.getRegisters().get(0).getValue().toString().toInt

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
    val swapExtemums: (Double, (Long, Long, Long, Long)) = ErgoDexSwap.swapExtremums(ergodexMinExecutionFee, ergodexSettings.nitro, minQuoteAmount)

    // Calculate execution fee numerator and denominator
    val dexFeePerTokenFraction: (Long, Long) = GuapSwapUtils.decimalToFraction(swapExtemums._1)

    // Calculate the new PK with the added hex prefix
    //val pkString: String = ErgoDexUtils.addPKAddressPrefix(guapswapSettings.userAddress)
    val newPK: Values.SigmaBoolean = Address.create(guapswapSettings.userAddress).asP2PK().pubkey

    // Converts all value types into ErgoValue types
    val paramPK:                    ErgoValue[SigmaProp]    =   ErgoValue.of(newPK)
    val paramPoolFeeNum:            ErgoValue[Int]          =   ErgoValue.of(poolFeeNum)
    val paramQuoteId:               ErgoValue[Coll[Byte]]   =   ErgoValue.of(JavaHelpers.collFrom(yAsset.getId().getBytes()), ErgoType.byteType())
    val paramMinQuoteAmount:        ErgoValue[Long]         =   ErgoValue.of(minQuoteAmount)
    val paramBaseAmount:            ErgoValue[Long]         =   ErgoValue.of(baseAmount)
    val paramDexFeePerTokenNum:     ErgoValue[Long]         =   ErgoValue.of(dexFeePerTokenFraction._1)
    val paramDexFeePerTokenDenom:   ErgoValue[Long]         =   ErgoValue.of(dexFeePerTokenFraction._2)
    val paramMaxMinerFee:           ErgoValue[Long]         =   ErgoValue.of(ergodexMinerFee)
    val paramPoolNFTId:             ErgoValue[Coll[Byte]]   =   ErgoValue.of(JavaHelpers.collFrom(poolNFT.getId().getBytes()), ErgoType.byteType())

    val swapsellparams = ErgoDexSwapSellParams(paramPK, paramPoolFeeNum, paramQuoteId, paramMinQuoteAmount, paramBaseAmount, paramDexFeePerTokenNum, paramDexFeePerTokenDenom, paramMaxMinerFee, paramPoolNFTId)
    swapsellparams
  }

  /**
    * Substitute the swap sell parameters into the sample contract.
    *
    * @param swapsellparams
    * @return New contract with updated context variables. 
    */
  def getSubstSwapSellContractWithParams(swapsellparams: ErgoDexSwapSellParams): (ErgoValue[Coll[Byte]], ErgoValue[Coll[Byte]]) = {
  
    // Get the ErgoTree bytes for the swap sell contract sample => constantSegregationFlag == True
    val dexSwapSellContractSample: ErgoTree = JavaHelpers.decodeStringToErgoTree(ErgoDexUtils.ERGODEX_SWAPSELL_CONTRACT_SAMPLE)
    
    // Get the constants from the ErgoTree
    val constants: IndexedSeq[Values.Constant[SType]] = dexSwapSellContractSample.constants
    
    // Update constants by index in increasing order
    val const_0   = constants.updated(0, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramPK)))
    val const_2   = const_0.updated(2, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramBaseAmount)))
    val const_8   = const_2.updated(8, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramPoolNFTId)))
    val const_9   = const_8.updated(9, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramQuoteId)))
    val const_10  = const_9.updated(10, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramMinQuoteAmount)))
    val const_11  = const_10.updated(11, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramDexFeePerTokenNum)))
    val const_12  = const_11.updated(12, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramDexFeePerTokenDenom)))
    val const_14  = const_12.updated(14, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramFeeNum)))
    val const_17  = const_14.updated(17, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramBaseAmount)))
    val const_18  = const_17.updated(18, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramFeeNum)))
    val const_22  = const_18.updated(22, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(swapsellparams.paramMaxMinerFee)))
    val newConstants = const_22
    val newConstantsWithoutPK = const_22.updated(0, Iso.isoEvaluatedValueToSConstant.to(Iso.isoErgoValueToSValue.to(ErgoValue.of(Address.create(GuapSwapUtils.MAINNET_TEST_ADDRESS).asP2PK().pubkey))))

    // Substitute the constants 
    val newDexSwapSellContractSample: Values.SValue = ErgoTree.substConstants(dexSwapSellContractSample.root.right.get, newConstants)
    val newDexSwapSellContractSampleWithoutPK: Values.SValue = ErgoTree.substConstants(dexSwapSellContractSample.root.right.get, newConstantsWithoutPK)

    // Return the ErgoTree contract as ErgoValue[Coll[Byte]]
    val substContract: ErgoValue[Coll[Byte]] = ErgoValue.of(ErgoTree.fromProposition(newDexSwapSellContractSample.asInstanceOf[Values.SigmaPropValue]).bytes)
    val substContractWithoutPK: ErgoValue[Coll[Byte]] = ErgoValue.of(ErgoTree.fromProposition(newDexSwapSellContractSampleWithoutPK.asInstanceOf[Values.SigmaPropValue]).bytes)
    (substContractWithoutPK, substContract)
  }

}