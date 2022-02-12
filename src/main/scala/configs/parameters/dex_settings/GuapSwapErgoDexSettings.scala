package configs.parameters.dex_settings

/**
  * Class representing the ErgoDex settings.
  *
  * @param swapTokenTicker
  * @param slippageTolerancePercentage
  * @param nitro
  * @param minerFee
  */
case class GuapSwapErgoDexSettings(
    val swapTokenTicker: String,
    val slippageTolerancePercentage: Double = 0.001,
    val nitro: Double = 1.2,
    val minerFee: Double = 0.002
)



