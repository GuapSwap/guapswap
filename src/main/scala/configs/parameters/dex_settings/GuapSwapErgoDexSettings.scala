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
    val swapAssetTicker: String = "SigUSD",
    val slippageTolerancePercentage: Double = ErgoDexUtils.DEFAULT_ERGODEX_SLIPPAGE_TOLERANCE_PERCENTAGE,
    val nitro: Double = ErgoDexUtils.DEFAULT_ERGODEX_NITRO,
    val ergodexMinerFee: Double = ErgoDexUtils.DEFAULT_ERGODEX_MINER_FEE
)