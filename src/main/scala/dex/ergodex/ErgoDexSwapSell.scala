package dex.ergodex

import protocol.DexPool
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.ErgoId
import org.ergoplatform.appkit.ErgoValue
import special.collection.Coll
import configs.parameters.dex_settings.GuapSwapErgoDexSettings
import configs.parameters.GuapSwapParameters

object ErgoDexSwapSell {

    /**
      * Calculate min output amount
      * 
      * @param swapInputAmount
      * @param maxSlippagePercentage
      * @param xAmount
      * @param yAmount
      * @param feeNum
      * @param feeDenom
      * @return 
      */
    def calcMinOutputAmount(swapInputAmount: Long, maxSlippagePercentage: Double, xAmount: Long, yAmount: Long, feeNum: Long, feeDenom: Long): Long = {
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
      * @return
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
      * @return 
      */
    def swapSellParams(proxyBox: InputBox, poolBox: InputBox, parameters: GuapSwapParameters): ErgoDexSwapSellParams = {
        val poolNFT = poolBox.getTokens().get(0)
        val yAsset = poolBox.getTokens().get(2)

        val poolNFTId: ErgoValue[Coll[Byte]] = ErgoValue.of(poolNFT.getId().getBytes())
        val QuoteId: ErgoValue[Coll[Byte]] = ErgoValue.of(yAsset.getId().getBytes())

    }

}