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

            println(Console.RED + GuapSwapCli.title + Console.RESET)

            // Setup Ergo Client
            val nodeConfig: GuapSwapNodeConfig = configLoadResult.get.node
            val parametersConfig: GuapSwapParameters = configLoadResult.get.parameters
            val explorerURL: String = RestApiErgoClient.getDefaultExplorerUrl(nodeConfig.networkType)
            val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConfig.nodeApi.apiUrl, nodeConfig.networkType, nodeConfig.nodeApi.apiKey, explorerURL)
            
            // Parse commands from the command line
            (GuapSwapCli.generateProxyAddressSubCommandOpts orElse GuapSwapCli.swapSubCommandOpts orElse GuapSwapCli.refundSubCommandOpts orElse GuapSwapCli.listSubCommandOpts).map {
                
                case GuapSwapCli.GenerateProxyAddress() => {
                    println("generate address")
                    //GuapSwapInteraction.generateProxyAddress(ergoClient, parameters)
                    IO(ExitCode.Success)
                }
                
                case GuapSwapCli.Swap(proxyAddress, onetime) => {
                    println(s"swapped: ${proxyAddress} with onetime=${onetime}")
                    IO(ExitCode.Success)
                }

                case GuapSwapCli.Refund(proxyAddress) => {
                    println(s"refunded box at address: ${proxyAddress}")
                    IO(ExitCode.Success)
                }

                case GuapSwapCli.List(proxyAddress) => {
                    println(s"list boxes at address: ${proxyAddress}")
                    IO(ExitCode.Success)
                }

            } 
        } else {

            // Print Failure exeption and return error code
            println(configLoadResult.get)
            Opts(IO(ExitCode.Error))
        }

    }
}