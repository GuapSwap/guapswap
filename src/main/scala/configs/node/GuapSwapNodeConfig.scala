package configs.node

import org.ergoplatform.appkit.NetworkType

/**
  * Class representing the node configuration.
  *
  * @param nodeApi
  * @param wallet
  * @param networkType
  */
case class GuapSwapNodeConfig(
    val nodeApi: GuapSwapApiConfig,
    val wallet: GuapSwapWalletConfig,
    val networkType: NetworkType
)
