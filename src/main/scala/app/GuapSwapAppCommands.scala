package app

import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._

import org.ergoplatform.appkit.{ErgoClient, BlockchainContext, ConstantsBuilder}
import configs.node.GuapSwapNodeConfig
import configs.parameters.GuapSwapParameters
import contracts.GuapSwapErgoDexSwapSellProxyContract
import dex.ergodex.ErgoDexUtils

/**
  * Object that defines the commands availble for the CLI interface and for interacting with the Ergo blockchain
  */
object GuapSwapAppCommands {

    /**
      * Object constains definitions for the GuapSwap CLI commands and arguments
      */
    object GuapSwapCli {

        // Type representation of the different commands available for the CLI
        case class GenerateProxyAddress()
        case class Swap(proxyAddress: String, onetime: Boolean)
        case class Refund(proxyAddress: String)
        case class List(proxyAddress: String)
        case class Logs()

        // Defining proxy_address argument, which is used by different commands
        val proxyAddressArgumentOpts: Opts[String] = {
            Opts.argument[String](metavar = "proxy_address")
        }

        // Generate proxy address command
        val generateProxyAddressSubCommandOpts: Opts[GenerateProxyAddress] = {
            Opts.subcommand(name = "generate", help = "Generate proxy address based on config file info.") {
                Opts(GenerateProxyAddress())
            }
        }

        // Swap command
        val swapSubCommandOpts: Opts[Swap] = {
            Opts.subcommand(name = "swap", help = "Loops indefinitely, querying Ergo blockchain once every 60 min to check for a payout and performs the swap automatically.") {
                
                // Defining --onetime flag for running swap once instead of continuously
                val onetimeFlagOpts: Opts[Boolean] = {
                    Opts.flag(long = "onetime", help = "Perform a one time swap with all eUTXOs at this address").orFalse
                }
                
                (proxyAddressArgumentOpts, onetimeFlagOpts).mapN(Swap)
            }
        }

        // Refund command
        val refundSubCommandOpts: Opts[Refund] = {
            Opts.subcommand(name = "refund", help = "Refund payouts containded in all eUTXOs at the proxy address.") {
                (proxyAddressArgumentOpts).map(Refund)
            }
        }

        // List command
        val listSubCommandOpts: Opts[List] = {
            Opts.subcommand(name = "list", help = "List all eUTXOs at the proxy address.") {
                (proxyAddressArgumentOpts).map(List)
            }
        }

        // Logs command => just print logs file???

        // GuapSwap CLI title
        val title: String = {
            """|    
               |    
               |      /$$$$$$  /$$   /$$  /$$$$$$  /$$$$$$$   /$$$$$$  /$$      /$$  /$$$$$$  /$$$$$$$         /$$$$$$  /$$      /$$$$$$
               |     /$$__  $$| $$  | $$ /$$__  $$| $$__  $$ /$$__  $$| $$  /$ | $$ /$$__  $$| $$__  $$       /$$__  $$| $$     |_  $$_/
               |    | $$  \__/| $$  | $$| $$  \ $$| $$  \ $$| $$  \__/| $$ /$$$| $$| $$  \ $$| $$  \ $$      | $$  \__/| $$       | $$  
               |    | $$ /$$$$| $$  | $$| $$$$$$$$| $$$$$$$/|  $$$$$$ | $$/$$ $$ $$| $$$$$$$$| $$$$$$$/      | $$      | $$       | $$  
               |    | $$|_  $$| $$  | $$| $$__  $$| $$____/  \____  $$| $$$$_  $$$$| $$__  $$| $$____/       | $$      | $$       | $$  
               |    | $$  \ $$| $$  | $$| $$  | $$| $$       /$$  \ $$| $$$/ \  $$$| $$  | $$| $$            | $$    $$| $$       | $$  
               |    |  $$$$$$/|  $$$$$$/| $$  | $$| $$      |  $$$$$$/| $$/   \  $$| $$  | $$| $$            |  $$$$$$/| $$$$$$$$/$$$$$$
               |     \______/  \______/ |__/  |__/|__/       \______/ |__/     \__/|__/  |__/|__/             \______/ |________/______/
               |
               |""".stripMargin
        } 
    }

    
    /**
      * Object containts the function used to interact with the Ergo blockchain based on the GuapSwap CLI input command and arguments.
      */
    object GuapSwapInteraction {

        // Command to generate a proxy address
        def generateProxyAddress(ergoClient: ErgoClient, parameters: GuapSwapParameters): Unit = {

            // get the proxy contract ErgoScript
            val swapSellProxyScript: String = GuapSwapErgoDexSwapSellProxyContract.getScript

            // generate blockchain context
            val compiledSwapSellProxyScript = ergoClient.execute((ctx: BlockchainContext) => {
                
                // Calculate the variables
                val PK = JavaHelpers.decodeStringToErgoTree(parameters.guapswapProtocolSettings.userAddress).getPublicKey()

                // compile the script
                ctx.compileContract(
                    ConstantsBuilder.create()
                        .item("PK", PK)
                        .item("ErgoDexSwapSellContract", ErgoDexUtils.ERGODEX_SWAPSELL_SAMPLE_CONTRACT)
                        .item("GuapSwapServiceFeePercentageNum", )
                        .item("", )
                        .item("", )
                        .itme("MinErgoDexExecutionFee", ErgoDexUtils.calculateMinExecutionFee())
                        .build(),
                        swapSellProxyScript
                )
            })
            
            
        }
    }

}