package configs.parameters.protocol_settings

import protocol.GuapSwapConstants

/**
  * Class representing the protocol service fees.
  *
  * @param protocolFeePercentage
  * @param uiFeePercentage
  * @param minerFee
  */
case class GuapSwapServiceFees(
    val protocolFeePercentage: Double = GuapSwapConstants.DEFAULT_PROTOCOL_FEE_PERCENTAGE,
    val uiFeePercentage: Double = GuapSwapConstants.DEFAULT_PROTOCOL_UI_FEE_PERCENTAGE,
    val minerFee: Double = GuapSwapConstants.DEFAULT_PROTOCOL_MINER_FEE
)