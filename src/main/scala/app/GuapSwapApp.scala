import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._

import protocol.GuapSwapUtils
import configs.GuapSwapConfig
import configs.node.GuapSwapNodeConfig
import app.GuapSwapAppCommands
import app.GuapSwapAppCommands.{GuapSwapCli, GuapSwapInteractions}
import scala.util.{Try, Success, Failure}
import org.ergoplatform.appkit.{RestApiErgoClient, ErgoClient}
import configs.parameters.GuapSwapParameters

object GuapSwapApp extends CommandIOApp(
    name = "guapswap",
    header = "GuapSwap CLI for the everyday Ergo miner.",
    version = "0.1.0"
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

            // Setup Ergo Client
            val nodeConfig: GuapSwapNodeConfig = configLoadResult.get.node
            val parameters: GuapSwapParameters = configLoadResult.get.parameters
            val explorerURL: String = RestApiErgoClient.getDefaultExplorerUrl(nodeConfig.networkType)
            val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConfig.nodeApi.apiUrl, nodeConfig.networkType, nodeConfig.nodeApi.apiKey, explorerURL)
            
            // Parse commands from the command line
            (GuapSwapCli.generateProxyAddressSubCommandOpts orElse GuapSwapCli.swapSubCommandOpts orElse GuapSwapCli.refundSubCommandOpts orElse GuapSwapCli.listSubCommandOpts).map {
                
                case GuapSwapCli.GenerateProxyAddress() => {

                    // Print status message
                    println(Console.YELLOW + "========== GUAPSWAP PROXY ADDRESS BEING GENERATED ==========" + Console.RESET)
                   
                    // Generate proxy address
                    val proxyScript: String = GuapSwapInteractions.generateProxyAddress(ergoClient, parameters)
                    
                    // Print proxy address generation result message
                    println(Console.GREEN + "========== GUAPSWAP PROXY ADDRESS SUCCESSFULLY GENERATED ==========" + Console.RESET)
                    
                    // TODO: Add proxy script to guapswap_proxy.json

                    // Print out proxy address for user
                    println(Console.BLUE + "========== INSERT GUAPSWAP PROXY (P2S) ADDRESS BELOW INTO YOUR MINER ==========" + Console.RESET)
                    println(proxyScript)

                    // Return successful exit code
                    IO(ExitCode.Success)
                }
                
                case GuapSwapCli.Swap(proxyAddress, onetime) => {

                    if (onetime) {

                        // Print guapswap initiating status message
                        println(Console.YELLOW + "========== GUAPSWAP ONETIME INITIATED ==========" + Console.RESET)
                        val onetimeSwapTx: String = GuapSwapInteractions.guapswapOneTime(ergoClient, nodeConfig, parameters, proxyAddress)

                        // TODO: check if tx is even possible
                        // Print out guapswap initiated status message
                        println(Console.GREEN + "========== GUAPSWAP ONETIME SUCCEEDED ==========" + Console.RESET)
                        
                        // Print tx link to user
                        println(Console.BLUE + "========== VIEW GUAPSWAP ONETIME TX IN THE ERGO-EXPLORER WITH THE LINK BELOW ==========" + Console.RESET)
                        println(onetimeSwapTx)
                        
                    } else {
                        // TODO: initiate indefinite swap
                    }
                    
                    // Return successful exit code
                    IO(ExitCode.Success)
                }

                case GuapSwapCli.Refund(proxyAddress) => {
                    println(s"refunded box at address: ${proxyAddress}")
                    
                    // Return successful exit code
                    IO(ExitCode.Success)
                }

                case GuapSwapCli.List(proxyAddress) => {
                    println(s"list boxes at address: ${proxyAddress}")
                    
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