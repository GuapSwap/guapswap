package configs.parameters.dex_settings

/**
  * Class representing the ErgoDex settings.
  *
  * @param swapAssetTicker
  * @param slippageTolerancePercentage
  * @param nitro
  * @param minerFee
  */
case class GuapSwapErgoDexSettings(
  val swapAssets: GuapSwapAssets
)