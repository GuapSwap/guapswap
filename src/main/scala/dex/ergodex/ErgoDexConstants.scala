package dex.ergodex

import protocol.{DexPool, DexAsset, GuapSwapConstants}

/**
  * Object representing constants relevant to ErgoDex.
  */
object ErgoDexConstants {

    object ErgoDexPools {
        final val ERG_2_SigUSD: DexPool = new DexPool(
            poolId = "9916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec",
            assetX = ErgoDexAssets.ERG,
            assetY = ErgoDexAssets.SIG_USD
        )
    }

    object ErgoDexAssets {

        // MAINNET Assets
        final val ERG: DexAsset = GuapSwapConstants.PossibleDexAssets.ERG
        final val SIG_USD: DexAsset = GuapSwapConstants.PossibleDexAssets.SIG_USD

        // MEME Coins
        final val ERDOGE: DexAsset = GuapSwapConstants.PossibleDexAssets.ERDOGE

        // TEST Assets
        final val WT_ERG: DexAsset = GuapSwapConstants.PossibleDexAssets.WT_ERG
    }

    // Swap sell sample contract from ErgoDex.
    final val ErgoDexSwapSellSampleContract: String = "19e5031808cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040005e012040404060402040004000e2000000000000000000000000000000000000000000000000000000000000000000e20010101010101010101010101010101010101010101010101010101010101010105c00c05040514040404c80f06010104d00f05e01204c80f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d803d6017300d602b2a4730100d6037302eb027201d195ed93b1a4730393b1db630872027304d804d604db63087202d605b2a5730500d606b2db63087205730600d6077e8c72060206edededededed938cb2720473070001730893c27205d07201938c72060173099272077e730a06927ec172050699997ec1a7069d9c72077e730b067e730c067e720306909c9c7e8cb27204730d0002067e7203067e730e069c9a7207730f9a9c7ec17202067e7310067e9c73117e7312050690b0ada5d90108639593c272087313c1720873147315d90108599a8c7208018c72080273167317"
    
    // Prefix added to user PK before insertion into swap swell contract ErgoScript.
    final val SigmaPropConstPrefixHex: String = "08cd"
}