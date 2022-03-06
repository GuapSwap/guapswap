package configs.parameters.dex_settings

/**
  * Class representing a dex swap asset with corresponding settings.
  *
  * @param swapAssetTicker
  * @param slippageTolerancePercentage
  * @param nitro
  * @param dexMinerFee
  * @param percentageOfPayout
  */
case class GuapSwapDexAsset(
  val swapAssetTicker: String,
  val slippageTolerancePercentage: Double,
  val nitro: Double,
  val dexMinerFee: Double,
  val percentageOfPayout: Double
)