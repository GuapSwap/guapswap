package configs.parameters.dex_settings

import dex.ergodex.ErgoDexUtils

/**
  * Class representing the ErgoDex settings.
  *
  * @param swapAssetTicker
  * @param slippageTolerancePercentage
  * @param nitro
  * @param minerFee
  */
case class GuapSwapErgoDexSettings(
    val swapAssetTicker: String,
    val slippageTolerancePercentage: Double,
    val nitro: Double,
    val ergodexMinerFee: Double
)