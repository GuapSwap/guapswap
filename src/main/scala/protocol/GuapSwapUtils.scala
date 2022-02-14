package protocol

import org.ergoplatform.appkit.Address
import scala.collection.immutable.HashMap

/**
  * Object representing constants and methods relevant to GuapSwap.
  */
object GuapSwapUtils {

    // Constant representing the storage location within the project repository of the guapswap_config.json file and guapswap_proxy.json file
    final val GUAPSWAP_CONFIG_FILE_PATH: String = "storage/guapswap_config.json"
    final val GUAPSWAP_PROXY_FILE_PATH: String = "storage/guapswap_proxy.json"

    // Default public node URL
    final val DEFAULT_PUBLIC_NODE_URL: String = ""

    // Default service fee constants
    final val DEFAULT_PROTOCOL_FEE_PERCENTAGE: Double = 0.0025D // 0.0 for GuapSwap-Ronin CLI only
    final val DEFAULT_PROTOCOL_UI_FEE_PERCENTAGE: Double = 0.0D // 0.0 for all CLI versions, only charged for web version with UI.
    final val DEFAULT_PROTOCOL_MINER_FEE: Double = 0.002D

    // GuapSwap service fee contract sample P2S address, for now just my TESTNET test_wallet P2PK address
    final val GUAPSWAP_SERVICE_FEE_CONTRACT_SAMPLE: String = "9ej8AEGCpNxPaqfgisJTU2RmYG91bWfK1hu2xT34i5Xdw4czidX"

    // Minimum box value in nanoErgs
    final val MIN_BOX_VALUE: Long = 1000000L

    // HashMap of possible Ergo Assets
    final val validErgoAssets: HashMap[String, DexAsset] = HashMap(
        "ERG" -> DexAsset("0", "ERG", 9),
        "SigUSD" -> DexAsset("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", "SigUSD", 2),
        "SigRSV" -> DexAsset("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", "SigRSV", 0),
        "Erdoge" -> DexAsset("36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", "Erdoge", 0),
        "LunaDog" -> DexAsset("5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", "LunaDog", 8),
        "kushti" -> DexAsset("fbbaac7337d051c10fc3da0ccb864f4d32d40027551e1c3ea3ce361f39b91e40", "kushti", 0),
        "WT_ERG" -> DexAsset("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b", "WT_ERG", 9),
        "WT_ADA" -> DexAsset("30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e", "WT_ADA", 8)
    )

    /**
      * Convert from ERGs to nanoERGs
      *
      * @param erg
      * @return Converted ERG value in nanoErgs.
      */
    def ergToNanoErg(erg: Double): Long = (erg * 1000000000L).toLong

    /**
      * Calculate the miner fee in nanoERGs
      *
      * @param minerFee
      * @return Miner fee in nanoERGs.
      */
    def convertMinerFee(minerFee: Double): Long = {
        val minerFeeNanoErgs = ergToNanoErg(minerFee)
        if (minerFeeNanoErgs < MIN_BOX_VALUE) {
            ergToNanoErg(DEFAULT_PROTOCOL_MINER_FEE)
        } else {
            minerFeeNanoErgs
        }
    }

    /**
      * Calculate the total protocol fees, this include the ui fee and the procotol fee, but NOT the protocol miner fee
      *
      * @param protocolFee
      * @param protocolUIFee
      * @param payout
      * @return Total protocol fee in nanoErgs.
      */
    def calculateTotalProtocolFee(protocolFeePercentage: Double, protocolUIFeePercentage: Double, payout: Long): Long = ergToNanoErg(protocolFeePercentage * payout) + ergToNanoErg(protocolUIFeePercentage * payout)

    /**
      * Calculate the service fee, this include the total protocol fee and the protocol miner fee
      *
      * @param totalProtocolFee
      * @param protocolMinerFee
      * @return Total service fee in nanoErgs.
      */
    def calculateServiceFee(totolProtocolFee: Long, protocolMinerFee: Long): Long = totolProtocolFee + protocolMinerFee

    /**
      * Method to calculate the minValue for the Guap Swap transaction to occur, including interaction with the dex.
      * 
      * @param serviceFee
      * @param totalDexFee The minium total fees charged by the dex, including mining fees at that stage.
      * @return The minimum value in nanoErgs that the transaction can cost, on both the GuapSwap stage and dex stage.
      */
    def minValueOfTotalFees(serviceFee: Long, totalDexFee: Long): Long = serviceFee + totalDexFee


    /**
      * Method to calculate the base amount to be swapped at the dex.
      *
      * @param payout
      * @param totalFee
      * @return Base amount to be swapped in nanoErgs.
      */
    def calculateBaseAmount(payout: Long, totalFee: Long): Long = payout - totalFee
    
    /**
      * Method to convert a decimal number to a rational fraction.
      *
      * @param number
      * @return Tuple of the numerator and denominator representing the decimal number.
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
