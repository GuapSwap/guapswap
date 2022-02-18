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

}