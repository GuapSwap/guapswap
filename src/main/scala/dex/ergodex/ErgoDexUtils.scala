package dex.ergodex

import protocol.{DexPool, DexAsset, GuapSwapUtils}
import scala.collection.immutable.HashMap
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.Address

/**
  * Object representing constants and methods relevant to ErgoDex.
  */
object ErgoDexUtils {

    // Default ErgoDex settings
    final val DEFAULT_ERGODEX_SLIPPAGE_TOLERANCE_PERCENTAGE: Double = 0.001D
    final val DEFAULT_ERGODEX_NITRO: Double = 1.2D
    final val DEFAULT_ERGODEX_MINER_FEE: Double = 0.002D


    // Swap sell sample contract from ErgoDex.
    final val ERGODEX_SWAPSELL_SAMPLE_CONTRACT: String = "19e5031808cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040005e012040404060402040004000e2000000000000000000000000000000000000000000000000000000000000000000e20010101010101010101010101010101010101010101010101010101010101010105c00c05040514040404c80f06010104d00f05e01204c80f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d803d6017300d602b2a4730100d6037302eb027201d195ed93b1a4730393b1db630872027304d804d604db63087202d605b2a5730500d606b2db63087205730600d6077e8c72060206edededededed938cb2720473070001730893c27205d07201938c72060173099272077e730a06927ec172050699997ec1a7069d9c72077e730b067e730c067e720306909c9c7e8cb27204730d0002067e7203067e730e069c9a7207730f9a9c7ec17202067e7310067e9c73117e7312050690b0ada5d90108639593c272087313c1720873147315d90108599a8c7208018c72080273167317"
    
    // Prefix added to user PK before insertion into swap swell contract ErgoScript.
    final val SIGMAPROP_CONST_PREFIX_HEX: String = "08cd"

    // Pool fee denominator, determined from Swap Sell contract.
    final val POOL_FEE_DENOM: Long = 1000

    // Storing all of the valid ErgoDex assets available for trading.
    final val validErgoDexAssets: HashMap[String, DexAsset] = HashMap(
        "ERG" -> GuapSwapUtils.validErgoAssets.get("ERG").get,
        "SigUSD" -> GuapSwapUtils.validErgoAssets.get("SigUSD").get,
        "SigRSV" -> GuapSwapUtils.validErgoAssets.get("SigRSV").get,
        "Erdoge" -> GuapSwapUtils.validErgoAssets.get("Erdoge").get,
        "LunaDog" -> GuapSwapUtils.validErgoAssets.get("LunaDog").get
    )

    // Storing all of the valid ErgoDex pools available, based on the available assets.
    final val validErgoDexPools: HashMap[String, DexPool] = HashMap(
        "ERG_2_SigUSD" -> DexPool(
            poolId = "9916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec",
            assetX = validErgoDexAssets.get("ERG").get,
            assetY = validErgoDexAssets.get("SigUSD").get
        ),
        "ERG_2_SigRSV" -> DexPool(
            poolId = "1d5afc59838920bb5ef2a8f9d63825a55b1d48e269d7cecee335d637c3ff5f3f",
            assetX = validErgoDexAssets.get("ERG").get,
            assetY = validErgoDexAssets.get("SigRSV").get
        ),
        "ERG_2_Ergoge" -> DexPool(
            poolId = "3d4fdb931917647f4755ada29d13247686df84bd8aea060d22584081bd11ba69",
            assetX = validErgoDexAssets.get("ERG").get,
            assetY = validErgoDexAssets.get("Erdoge").get
        ),
        "ERG_2_LunaDog" -> DexPool(
            poolId = "c1b9c430249bd97326042fdb09c0fb6fe1455d498a20568cc64390bfeca8aff2",
            assetX = validErgoDexAssets.get("ERG").get,
            assetY = validErgoDexAssets.get("LunaDog").get
        )
     )

    /**
      * Calculate the minium execution fee.
      *
      * @param minerFee
      * @return The minimum execution fee in nanoErgs.
      */
    def calculateMinExecutionFee(minerFee: Double): Long = {
        val minerFeeNanoErgs: Long = GuapSwapUtils.convertMinerFee(minerFee)
        val minExecutionFee: Long = 3L * minerFeeNanoErgs
        minExecutionFee
    }

    /**
      * Calculate the minium value of total ErgoDex fees.
      *
      * @param minExecutionFee
      * @param minerFee
      * @return The minimum ErgoDex fee in nanoErgs.
      */
    def minValueOfTotalErgoDexFees(minExecutionFee: Long, minerFee: Long): Long = minExecutionFee + minerFee

    /**
      * Add the PK prefix to the user Pk address.
      *
      * @param pk
      * @return The user Pk address with the hex prefix.
      */
    def addPKAddressPrefix(pk: String): String = {
        val pkWithPrefix = SIGMAPROP_CONST_PREFIX_HEX + pk
        pkWithPrefix
    }

    
}