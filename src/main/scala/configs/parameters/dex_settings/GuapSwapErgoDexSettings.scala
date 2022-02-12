package configs.parameters.dex_settings

import dex.ergodex.ErgoDexConstants

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
    val slippageTolerancePercentage: Double = ErgoDexConstants.DEFAULT_ERGODEX_SLIPPAGE_TOLERANCE_PERCENTAGE,
    val nitro: Double = ErgoDexConstants.DEFAULT_ERGODEX_NITRO,
    val minerFee: Double = ErgoDexConstants.DEFAULT_ERGODEX_MINER_FEE
)



