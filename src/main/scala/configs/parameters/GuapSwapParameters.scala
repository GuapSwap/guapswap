package configs.parameters

import configs.parameters.protocol_settings._
import configs.parameters.dex_settings._

/**
 *  Class representing all of the parameters in guapswap_conf.json
 * 
 * @param guapSwapProtocolSettings
 * @param dex_settings
 */
case class GuapSwapParameters(
    val guapSwapProtocolSettings: GuapSwapProtocolSettings,
    val dexSettings: GuapSwapDexSettings
)

