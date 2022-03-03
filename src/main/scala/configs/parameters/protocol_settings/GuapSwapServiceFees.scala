package configs.parameters.protocol_settings

import protocol.GuapSwapUtils

/**
  * Class representing the protocol service fees.
  *
  * @param protocolFeePercentage
  * @param uiFeePercentage
  * @param minerFee
  */
case class GuapSwapServiceFees(
    val protocolFeePercentage: Double,
    val protocolUIFeePercentage: Double,
    val protocolMinerFee: Double
)