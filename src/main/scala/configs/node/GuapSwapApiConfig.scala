package configs.node

import protocol.GuapSwapUtils

/**
  * Class representing the node Api.
  *
  * @param apiUrl
  * @param apiKey
  */
case class GuapSwapApiConfig(
    val apiUrl: String = GuapSwapUtils.DEFAULT_PUBLIC_NODE_URL,
    val apiKey: String
)

