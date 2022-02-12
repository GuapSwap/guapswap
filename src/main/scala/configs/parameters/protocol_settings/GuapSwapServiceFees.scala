package configs.parameters.protocol_settings

/**
  * Class representing the protocol service fees.
  *
  * @param protocolFeePercentage
  * @param uiFeePercentage
  * @param minerFee
  */
case class GuapSwapServiceFees(
    val protocolFeePercentage: Double = 0.0025,
    val uiFeePercentage: Double = 0.0,
    val minerFee: Double = 0.002
)