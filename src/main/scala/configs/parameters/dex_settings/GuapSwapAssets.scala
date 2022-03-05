package configs.parameters.dex_settings

/**
  * Class representing the dex swap assets, a maximum of three available swap assets.
  *
  * @param swapAsset1
  * @param swapAsset2
  * @param swapAsset3
  */
final case class GuapSwapAssets(
    val swapAsset1: GuapSwapAsset,
    val swapAsset2: GuapSwapAsset,
    val swapAsset3: GuapSwapAsset
)