package configs.node

import org.ergoplatform.appkit.SecretString

/**
  * Class to represent the wallet configuration.
  *
  * @param mnemonic
  * @param password
  * @param mnemonicPassword
  */
case class GuapSwapWalletConfig(
    val mnemonic: String,
    val password: String,
    val mnemonicPassword: String
)