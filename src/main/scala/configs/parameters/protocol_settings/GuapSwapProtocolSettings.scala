package configs.parameters.protocol_settings

/**
  * Class reresenting the protocol settings.
  * 
  * @param userAddress
  * @param serviceFees
  * @param swapIntervalInHours
  * @param dexChoice
  */
case class GuapSwapProtocolSettings(
    val userAddress: String,
    val serviceFees: GuapSwapServiceFees,
    val swapIntervalInHours: Long,
    val dexChoice: String
)
