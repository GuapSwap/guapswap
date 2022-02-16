package dex.ergodex

import protocol.GuapSwapUtils
import org.ergoplatform.appkit.{InputBox, ErgoValue, ErgoToken, Address}
import configs.parameters.GuapSwapParameters
import configs.parameters.dex_settings.{GuapSwapDexSettings, GuapSwapErgoDexSettings}
import configs.parameters.protocol_settings.{GuapSwapProtocolSettings, GuapSwapServiceFees}
import special.collection.Coll
import special.sigma.SigmaProp
import org.ergoplatform.ErgoAddress
import sigmastate.Values
import sigmastate.Values.{ErgoTree}
import org.ergoplatform.appkit.ErgoTreeTemplate
import org.ergoplatform.appkit.JavaHelpers
import sigmastate.SType
import org.ergoplatform.appkit.Iso

/**
  * Object to store methods relevant to an ErgoDex Swap
  */
object ErgoDexSwapSell {

    /**
      * Calculate min output amount of ErgoDex LP
      * 
      * @param swapInputAmount
      * @param maxSlippagePercentage
      * @param xAmount
      * @param yAmount
      * @param feeNum
      * @param feeDenom
      * @return Minimum output token amount from ErgoDex LP for given input amount.
      */
    def calculateMinOutputAmount(swapInputAmount: Long, maxSlippagePercentage: Double, xAmount: Long, yAmount: Long, feeNum: Long, feeDenom: Long): Long = {
        val slippage: Long = (maxSlippagePercentage * 100).toLong
        val outputAmount: Long = yAmount * swapInputAmount * feeNum / ((xAmount + (xAmount * slippage) / (100L * 100L)) * feeDenom + swapInputAmount * feeNum)
        outputAmount
    }

    /**
      * Calculate the swap extremums
      *
      * @param minExecutionFee
      * @param nitro
      * @param minOutputAmount
      * @return Tuple containing the swam extremums.
      */
    def swapExtremums(minExecutionFee: Long, nitro: Double, minOutputAmount: Long): (Long, (Long, Long, Long, Long)) = {
        val exFeePerToken = minExecutionFee / minOutputAmount 
        val adjustedMinExecutionFee = Math.floor(exFeePerToken * minOutputAmount)
        val maxExecutionFee = Math.floor(minExecutionFee * nitro)
        val maxOutputAmount = Math.floor(maxExecutionFee / exFeePerToken)
        val extremums = (exFeePerToken, (adjustedMinExecutionFee.toLong, maxExecutionFee.toLong, minOutputAmount, maxOutputAmount.toLong))
        extremums
    }

    /**
      * Calculate the swap parameters to be inserted as context variables.
      * 
      * @param proxyBox
      * @param poolBox
      * @param parameters
      * @return Parameters to be inserted as context variables with correct ErgoScript types.
      */
    def swapSellParams(proxyBox: InputBox, poolBox: InputBox, parameters: GuapSwapParameters): ErgoDexSwapSellParams = {
        
        // Get the PoolNFT token and the yAsset token
        val poolNFT: ErgoToken = poolBox.getTokens().get(0)
        val yAsset: ErgoToken = poolBox.getTokens().get(2)

        // PoolFeeNum: Long => R4 value of poolBox
        val poolFeeNum: Long = poolBox.getRegisters().get(0).getValue().asInstanceOf[Long]

        // xAmount of ERG and yAmount of token in LP
        val xAmount: Long = poolBox.getValue()
        val yAmount: Long = yAsset.getValue()

        // Payout value
        val payout: Long = proxyBox.getValue()

        // Get access to the parameter settings determined in the guapswap_config.json file
        val guapswapSettings: GuapSwapProtocolSettings = parameters.guapswapProtocolSettings
        val ergodexSettings: GuapSwapErgoDexSettings = parameters.dexSettings.ergodexSettings
        
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
        val minQuoteAmount: Long = calculateMinOutputAmount(baseAmount, ergodexSettings.slippageTolerancePercentage, xAmount, yAmount, poolFeeNum, ErgoDexUtils.POOL_FEE_DENOM)

        // Calculate the swap extremum values
        val swapExtemums: (Long, (Long, Long, Long, Long)) = swapExtremums(ergodexMinExecutionFee, ergodexSettings.nitro, minQuoteAmount)

        // Calculate execution fee numerator and denominator
        val dexFeePerTokenFraction: (Long, Long) = GuapSwapUtils.decimalToFraction(swapExtemums._1)

        // Calculate the new PK with the added hex prefix
        val newPK: String = ErgoDexUtils.addPKAddressPrefix(guapswapSettings.userAddress)

        // Converts all value types into ErgoValue types
        val paramPK: ErgoValue[SigmaProp] = ErgoValue.of(Address.create(newPK).getPublicKey())
        val paramPoolFeeNum: ErgoValue[Long] = ErgoValue.of(poolFeeNum)
        val paramQuoteId: ErgoValue[Coll[Byte]] = ErgoValue.of(yAsset.getId().getBytes())
        val paramMinQuoteAmount: ErgoValue[Long] = ErgoValue.of(minQuoteAmount)
        val paramBaseAmount: ErgoValue[Long] = ErgoValue.of(baseAmount)
        val paramDexFeePerTokenNum: ErgoValue[Long] = ErgoValue.of(dexFeePerTokenFraction._1)
        val paramDexFeePerTokenDenom: ErgoValue[Long] = ErgoValue.of(dexFeePerTokenFraction._2)
        val paramMaxMinerFee: ErgoValue[Long] = ErgoValue.of(ergodexMinerFee)
        val paramPoolNFTId: ErgoValue[Coll[Byte]] = ErgoValue.of(poolNFT.getId().getBytes())

        val swapsellparams = new ErgoDexSwapSellParams(paramPK, paramPoolFeeNum, paramQuoteId, paramMinQuoteAmount, paramBaseAmount, paramDexFeePerTokenNum, paramDexFeePerTokenDenom, paramMaxMinerFee, paramPoolNFTId)
        swapsellparams
    }

    /**
      * Substitute the swap sell parameters into the sample contract.
      *
      * @param swapsellparams
      * @return New contract with updated variables. 
      */
    // def getSubstSwapSellContractWithParams(swapsellparams: ErgoDexSwapSellParams): ErgoTree = {
      
    //   val swapSellErgoTree: ErgoTree = JavaHelpers.decodeStringToErgoTree(ErgoDexUtils.ERGODEX_SWAPSELL_SAMPLE_CONTRACT)

    //   val indexedSequenceOfConstants: IndexedSeq[Constant[SType]] 

    //   val test: Constant[SType] = Iso.
 
    //   val substitutedSwapSellErgoTree = ErgoTree.substConstants(swapSellErgoTree.root, )
      
    // }

}