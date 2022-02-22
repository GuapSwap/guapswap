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
object ErgoDexSwap {

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
    def calculateMinOutputAmount(baseAmount: Long, maxSlippagePercentage: Double, xAssetAmount: Long, yAssetAmount: Long, feeNumerator: Long, feeDenominator: Long): Long = {
        val swapInputAmount:  BigInt = BigInt.apply(baseAmount)
        val xAmount:          BigInt = BigInt.apply(xAssetAmount)
        val yAmount:          BigInt = BigInt.apply(yAssetAmount)
        val feeNum:           BigInt = BigInt.apply(feeNumerator)
        val feeDenom:   BigInt = BigInt.apply(feeDenominator)

        val slippage: BigInt = BigInt.apply((maxSlippagePercentage * 100D).toInt)
        val outputAmount: BigInt = (yAmount * swapInputAmount * feeNum) / ((xAmount + (xAmount * slippage) / (BigInt.apply(100) * BigInt.apply(100))) * feeDenom + swapInputAmount * feeNum)
        val outputAmountLong: Long = outputAmount.toLong
        outputAmountLong
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
        val exFeePerToken: Long = minExecutionFee / minOutputAmount 
        val adjustedMinExecutionFee: Double = Math.floor(exFeePerToken * minOutputAmount)
        val maxExecutionFee: Double = Math.floor(minExecutionFee * nitro)
        val maxOutputAmount: Double = Math.floor(maxExecutionFee / exFeePerToken)
        val extremums = (exFeePerToken, (adjustedMinExecutionFee.toLong, maxExecutionFee.toLong, minOutputAmount, maxOutputAmount.toLong))
        extremums
    }

}