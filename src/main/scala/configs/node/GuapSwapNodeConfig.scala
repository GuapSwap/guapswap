package configs.node

import org.ergoplatform.appkit.NetworkType

case class GuapSwapNodeConfig(
    val nodeApi: GuapSwapApiConfig,
    val wallet: GuapSwapWalletConfig,
    val networkType: NetworkType
)
