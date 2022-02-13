package protocol

import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.Address
import scala.collection.immutable.HashMap

object GuapSwapUtils {

    // HashMap of possible Ergo Assets
    final val validErgoAssets: HashMap[String, DexAsset] = HashMap(
        "ERG" -> DexAsset("0", "ERG", 9),
        "SigUSD" -> DexAsset("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", "SigUSD", 2),
        "Erdoge" -> DexAsset("36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", "Erdoge", 0),
        "WT_ERG" -> DexAsset("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b", "WT_ERG", 9)
    )

    // Will make this the actual service fee contract P2S address in the future, for now just my TESTNET test_wallet P2PK address
    final val GuapSwapServiceFeeContractSample: String = "9ej8AEGCpNxPaqfgisJTU2RmYG91bWfK1hu2xT34i5Xdw4czidX"

    // Default service fee constants
    final val DEFAULT_PROTOCOL_FEE_PERCENTAGE: Double = 0.0025D
    final val DEFAULT_PROTOCOL_UI_FEE_PERCENTAGE: Double = 0.0D
    final val DEFAULT_PROTOCOL_MINER_FEE: Double = 0.002D

    // Minimum box value in nanoErgs
    final val MIN_BOX_VALUE: Long = 1000000L

    /**
      * Convert from ERGs to nanoERGs
      *
      * @param erg
      * @return
      */
    def ergToNanoErg(erg: Double): Long = (erg * 1000000000L).toLong

    /**
      * Calculate the service fee, this include the ui fee and the procotol fee
      *
      * @param protocolFee
      * @param protocolUIFee
      * @return
      */
    def calculateServiceFee(protocolFee: Double, protocolUIFee: Double): Long = ergToNanoErg(protocolFee) + ergToNanoErg(protocolUIFee)

    /**
      * Calculate the miner fee in nanoERGs
      *
      * @param minerFee
      * @return
      */
    def calculateMinerFee(minerFee: Double): Long = {
        val minerFeeNanoErgs = ergToNanoErg(minerFee)
        if (minerFeeNanoErgs < MIN_BOX_VALUE) {
            ergToNanoErg(DEFAULT_PROTOCOL_MINER_FEE)
        } else {
            minerFeeNanoErgs
        }
    }

    /**
      * Method to calculate the minValue for the Guap Swap transaction to occur, including interaction with the dex.

      * @param serviceFee
      * @param protocolMinerFee
      * @param totalDexFee The minium total fees charged by the dex, including mining fees at that stage.
      * @return
      */
    def minValueOfGuapSwapFees(serviceFee: Long, protocolMinerFee: Long, totalDexFee: Long): Long = serviceFee + protocolMinerFee + totalDexFee
    
    /**
      * Method to convert a decimal number to a rational fraction.
      *
      * @param number
      * @return
      */
    def decimalToFraction(number: Double): (Long, Long) = {
      number.toString().split(".").toList match {
        case List(whole, fractional) => {
          val numDecimals = fractional.length()
          val denominator = Math.pow(10, numDecimals).toLong
          val numerator = whole.toLong * denominator + fractional.toLong
          (numerator, denominator)
        }
      }
    }
    
    
}
