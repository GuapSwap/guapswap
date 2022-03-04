package configs.parameters

import configs.parameters.protocol_settings._
import configs.parameters.dex_settings._

/**
 *  Class representing all of the configuration parameters.
 * 
 * @param guapSwapProtocolSettings
 * @param dexSettings
 */
case class GuapSwapParameters(
    val guapswapProtocolSettings: GuapSwapProtocolSettings,
    val dexSettings: GuapSwapDexSettings
)

