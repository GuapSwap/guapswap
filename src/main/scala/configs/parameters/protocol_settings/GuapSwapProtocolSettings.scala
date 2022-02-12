package configs.parameters.protocol_settings

/**
  * Class reresenting the protocol settings.
  * 
  * @param minderAddress
  * @param serviceFees
  */
case class GuapSwapProtocolSettings(
    val minderAddress: String,
    val serviceFees: GuapSwapServiceFees
)
