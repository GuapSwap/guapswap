package configs.parameters.dex_settings

/**
  * Class representing the dex swap assets, a maximum of three available swap assets.
  *
  * @param swapAsset1
  * @param swapAsset2
  * @param swapAsset3
  */
case class GuapSwapDexAssets(
  val swapAsset1: GuapSwapDexAsset,
  val swapAsset2: GuapSwapDexAsset,
  val swapAsset3: GuapSwapDexAsset
)