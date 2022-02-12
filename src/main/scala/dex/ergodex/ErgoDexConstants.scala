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
    final val ErgoDexSwapSellSampleContract: String = "ferf"
    
    // Prefix added to user PK before insertion into swap swell contract ErgoScript.
    final val SigmaPropConstPrefixHex: String = "08cd"
}