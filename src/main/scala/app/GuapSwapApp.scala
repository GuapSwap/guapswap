import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._

import protocol.GuapSwapUtils
import configs.GuapSwapConfig
import configs.node.GuapSwapNodeConfig
import configs.parameters.GuapSwapParameters

import app.GuapSwapAppCommands
import app.GuapSwapAppCommands.{GuapSwapCli, GuapSwapInteractions}

import scala.util.{Try, Success, Failure}
import org.ergoplatform.appkit.{RestApiErgoClient, ErgoClient, SecretStorage, Mnemonic}

import org.ergoplatform.wallet.secrets.{JsonSecretStorage}
import org.ergoplatform.wallet.settings.{SecretStorageSettings, EncryptionSettings}
import org.ergoplatform.appkit.NetworkType

/**
  * Main object of the GuapSwap CLI application.
  */
object GuapSwapApp extends CommandIOApp(
    name = "guapswap",
    header = "GuapSwap CLI for the everyday Ergo miner.",
    version = "1.0.1-beta"
) {

    override def main: Opts[IO[ExitCode]] = {
    

        // Load configuration settings from guapswap_config.json
        val configFilePath: String = GuapSwapUtils.GUAPSWAP_CONFIG_FILE_PATH
        val configLoadResult: Try[GuapSwapConfig] = GuapSwapConfig.load(configFilePath) 
        
        // Check if config file was loaded properly
        if (configLoadResult.isSuccess) {

            // Print title
            println(Console.RED + GuapSwapCli.guapswapTitle + Console.RESET)

            // Print configuration load status
            println(Console.GREEN + "========== CONFIGURATIONS LOADED SUCCESSFULLY ==========" + Console.RESET)

            // Setup Ergo Clients
            val nodeConfig: GuapSwapNodeConfig = configLoadResult.get.node
            val parameters: GuapSwapParameters = configLoadResult.get.parameters
            val explorerURL: String = RestApiErgoClient.getDefaultExplorerUrl(nodeConfig.networkType)
            val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConfig.nodeApi.apiUrl, nodeConfig.networkType, nodeConfig.nodeApi.apiKey, explorerURL)
            
            // Check secret storage
            val secretStorage: SecretStorage = GuapSwapUtils.checkSecretStorage()

            // Parse commands from the command line
            (GuapSwapCli.generateProxyAddressSubCommandOpts orElse GuapSwapCli.swapSubCommandOpts orElse GuapSwapCli.refundSubCommandOpts orElse GuapSwapCli.listSubCommandOpts).map {
                
                case GuapSwapCli.GenerateProxyAddress() => {

                    // Generate proxy address
                    println(Console.YELLOW + "========== GUAPSWAP PROXY ADDRESS BEING GENERATED ==========" + Console.RESET)
                    
                    val proxyAddress: String = GuapSwapInteractions.generateProxyAddress(ergoClient, parameters)
                    
                    println(Console.GREEN + "========== GUAPSWAP PROXY ADDRESS GENERATION SUCCESSFULL ==========" + Console.RESET)

                    // Print out guapswap save tx status message
                    println(Console.GREEN + "========== GUAPSWAP PROXY ADDRESS SAVED ==========" + Console.RESET)
                    GuapSwapUtils.save(proxyAddress, GuapSwapUtils.GUAPSWAP_PROXY_FILE_PATH)
                    
                    println(Console.BLUE + "========== INSERT GUAPSWAP PROXY (P2S) ADDRESS BELOW INTO YOUR MINER ==========" + Console.RESET)
                    println(proxyAddress)

                    // Return successful exit code
                    IO(ExitCode.Success)
                }
                
                case GuapSwapCli.Swap(proxyAddress, onetime) => {

                    // Unlock secret storage
                    val unlockedSecretStorage: SecretStorage = GuapSwapUtils.unlockSecretStorage(secretStorage) match {
                        case Success(unlockedStorage) => unlockedStorage
                        case Failure(exception) => {
                            println("Please try swap again.")
                            throw exception
                        }
                    }

                    if (onetime) {

                        // Print guapswap onetime initiated status message
                        println(Console.YELLOW + "========== GUAPSWAP ONETIME TX INITIATED ==========" + Console.RESET)
                        val onetimeSwapTxId: String = GuapSwapInteractions.guapswapOneTime(ergoClient, nodeConfig, parameters, proxyAddress, unlockedSecretStorage)

                        // Print out guapswap succeeded status message
                        println(Console.GREEN + "========== GUAPSWAP ONETIME TX SUCCESSFULL ==========" + Console.RESET)

                        // Print out guapswap save tx status message
                        println(Console.GREEN + "========== GUAPSWAP ONETIME TX SAVED ==========" + Console.RESET)
                        GuapSwapUtils.save(onetimeSwapTxId, GuapSwapUtils.GUAPSWAP_SWAP_FILE_PATH)
                        
                        // Print tx link to the user
                        println(Console.BLUE + "========== VIEW GUAPSWAP ONETIME TX IN THE ERGO-EXPLORER WITH THE LINK BELOW ==========" + Console.RESET)
                        println(GuapSwapUtils.ERGO_EXPLORER_TX_URL_PREFIX + onetimeSwapTxId)
                        
                    } else {
                        
                        // Print guapswap initiated status message
                        println(Console.YELLOW + "========== GUAPSWAP AUTOMATIC MODE STARTED ==========" + Console.RESET)
                        GuapSwapInteractions.guapswapAutomatic(ergoClient, nodeConfig, parameters, proxyAddress, unlockedSecretStorage)

                    }
                    
                    // Return successful exit code
                    IO(ExitCode.Success)
                }

                case GuapSwapCli.Refund(proxyAddress) => {

                    // Unlock secret storage
                    val unlockedSecretStorage: SecretStorage = GuapSwapUtils.unlockSecretStorage(secretStorage) match {
                        case Success(unlockedStorage) => unlockedStorage
                        case Failure(exception) => {
                            println("Please try swap again.")
                            throw exception
                        }
                    }
                    
                    // Print guapswap initiating status message
                    println(Console.YELLOW + "========== GUAPSWAP REFUND TX INITIATED ==========" + Console.RESET)
                    //val refundTxId: String = GuapSwapInteractions.guapswapRefund(ergoClient, parameters, proxyAddress, unlockedSecretStorage)
                    val refundTxId: String = GuapSwapInteractions.guapswapRefund(ergoClient, nodeConfig, parameters, proxyAddress, unlockedSecretStorage)

                    // TODO: check if tx is even possible
                    // Print out guapswap initiated status message
                    println(Console.GREEN + "========== GUAPSWAP REFUND TX SUCCEEDED ==========" + Console.RESET)

                    // Print out guapswap save tx status message
                    println(Console.GREEN + "========== GUAPSWAP REFUND TX SAVED ==========" + Console.RESET)
                    GuapSwapUtils.save(refundTxId, GuapSwapUtils.GUAPSWAP_REFUND_FILE_PATH)
                    
                    // Print tx link to user
                    println(Console.BLUE + "========== VIEW GUAPSWAP REFUND TX IN THE ERGO-EXPLORER WITH THE LINK BELOW ==========" + Console.RESET)
                    println(GuapSwapUtils.ERGO_EXPLORER_TX_URL_PREFIX + refundTxId)
                    
                    // Return successful exit code
                    IO(ExitCode.Success)
                }

                case GuapSwapCli.List(proxyAddress) => {
                    
                    println(Console.YELLOW + "========== GUAPSWAP LIST INITIATED ==========" + Console.RESET)
                    println(Console.YELLOW + "========== LISTING ALL PROXY BOXES WITH THE GIVEN ADDRESS ==========" + Console.RESET)

                    // List boxes at the proxy addres
                    GuapSwapInteractions.guapswapList(ergoClient, proxyAddress)
                    
                    println(Console.GREEN + "========== LISTING COMPLETE ==========" + Console.RESET)

                    // Return successful exit code
                    IO(ExitCode.Success)
                }

            } 

        } else {

            // Print configuration load status
            println(Console.RED + "========== CONFIGURATIONS LOADED UNSUCCESSFULLY ==========" + Console.RESET)

            // Print Failure exeption
            println(configLoadResult.get)

            // Return error exit code
            Opts(IO(ExitCode.Error))
        }
        
    }
}