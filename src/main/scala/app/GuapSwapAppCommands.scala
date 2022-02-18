package app

import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._

import scala.collection.JavaConverters._

import org.ergoplatform.appkit.{JavaHelpers, Address, ErgoClient, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoValue, Iso, InputBox}
import configs.node.GuapSwapNodeConfig
import configs.parameters.GuapSwapParameters
import protocol.GuapSwapUtils
import dex.ergodex.{ErgoDexUtils, ErgoDexSwap, ErgoDexSwapSellParams}
import contracts.{GuapSwapErgoDexSwapSellProxyContract, GuapSwapServiceFeeContract}
import sigmastate.Values._
import sigmastate.serialization.ErgoTreeSerializer
import special.collection.Coll
import special.sigma.SigmaProp
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.restapi.client.ApiClient
import org.ergoplatform.appkit.RestApiErgoClient
import org.ergoplatform.appkit.impl.InputBoxImpl
import dex.ergodex.ErgoDexSwapSellParams

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
        val guapswapTitle: String = {
            """|    
               |   /$$$$$$  /$$   /$$  /$$$$$$  /$$$$$$$    /$$    /$$      /$$  /$$$$$$  /$$$$$$$         /$$$$$$  /$$      /$$$$$$
               |  /$$__  $$| $$  | $$ /$$__  $$| $$__  $$ /$$$$$$ | $$  /$ | $$ /$$__  $$| $$__  $$       /$$__  $$| $$     |_  $$_/
               | | $$  \__/| $$  | $$| $$  \ $$| $$  \ $$/$$__  $$| $$ /$$$| $$| $$  \ $$| $$  \ $$      | $$  \__/| $$       | $$  
               | | $$ /$$$$| $$  | $$| $$$$$$$$| $$$$$$$/ $$  \__/| $$/$$ $$ $$| $$$$$$$$| $$$$$$$/      | $$      | $$       | $$  
               | | $$|_  $$| $$  | $$| $$__  $$| $$____/|  $$$$$$ | $$$$_  $$$$| $$__  $$| $$____/       | $$      | $$       | $$  
               | | $$  \ $$| $$  | $$| $$  | $$| $$      \____  $$| $$$/ \  $$$| $$  | $$| $$            | $$    $$| $$       | $$ 
               | |  $$$$$$/|  $$$$$$/| $$  | $$| $$      /$$  \ $$| $$/   \  $$| $$  | $$| $$            |  $$$$$$/| $$$$$$$$/$$$$$$
               |  \______/  \______/ |__/  |__/|__/     |  $$$$$$/|__/     \__/|__/  |__/|__/             \______/ |________/______/
               |                                         \_  $$_/                                                                   
               |                                           \__/    
               |                                                                 
               |""".stripMargin
        }

        val roninTitle: String = {
            """|    
               |   /$$$$$$  /$$   /$$  /$$$$$$  /$$$$$$$    /$$    /$$      /$$  /$$$$$$  /$$$$$$$         /$$$$$$  /$$      /$$$$$$
               |  /$$__  $$| $$  | $$ /$$__  $$| $$__  $$ /$$$$$$ | $$  /$ | $$ /$$__  $$| $$__  $$       /$$__  $$| $$     |_  $$_/
               | | $$  \__/| $$  | $$| $$  \ $$| $$  \ $$/$$__  $$| $$ /$$$| $$| $$  \ $$| $$  \ $$      | $$  \__/| $$       | $$  
               | | $$ /$$$$| $$  | $$| $$$$$$$$| $$$$$$$/ $$  \__/| $$/$$ $$ $$| $$$$$$$$| $$$$$$$/      | $$      | $$       | $$                                   _ 
               | | $$|_  $$| $$  | $$| $$__  $$| $$____/|  $$$$$$ | $$$$_  $$$$| $$__  $$| $$____/       | $$      | $$       | $$           _____  ____    ____    (_)   ____ 
               | | $$  \ $$| $$  | $$| $$  | $$| $$      \____  $$| $$$/ \  $$$| $$  | $$| $$            | $$    $$| $$       | $$          / ___/ / __ \  / __ \  / /   / __ \
               | |  $$$$$$/|  $$$$$$/| $$  | $$| $$      /$$  \ $$| $$/   \  $$| $$  | $$| $$            |  $$$$$$/| $$$$$$$$/$$$$$$       / /    / /_/ / / / / / / /   / / / /
               |  \______/  \______/ |__/  |__/|__/     |  $$$$$$/|__/     \__/|__/  |__/|__/             \______/ |________/______/      /_/     \____/ /_/ /_/ /_/   /_/ /_/ 
               |                                         \_  $$_/                                                                   
               |                                           \__/    
               |                                                                 
               |""".stripMargin
        }  
    }

    
    /**
      * Object containts the function used to interact with the Ergo blockchain based on the GuapSwap CLI input command and arguments.
      */
    object GuapSwapInteractions {

        // Command to generate a proxy address
        def generateProxyAddress(ergoClient: ErgoClient, parameters: GuapSwapParameters): String = {

            // TODO: Chose proxy script based type of DEXs on Ergo, right now just defaults to ErgoDex => will need to add dex param choice to guapswapParameterSettings and match statement
            // Get the proxy contract ErgoScript
            val swapSellProxyScript: String = GuapSwapErgoDexSwapSellProxyContract.getScript

            // Get the hard-coded constants for the proxy contract
            val userPublicKey:                  ErgoValue[SigmaProp]    =   ErgoValue.of(Address.create(parameters.guapswapProtocolSettings.userAddress).getPublicKey())
            val swapSellContractSample:         ErgoValue[Coll[Byte]]   =   ErgoValue.of(JavaHelpers.decodeStringToBytes(ErgoDexUtils.ERGODEX_SWAPSELL_CONTRACT_SAMPLE))
            val protocolFeePercentageFraction:  (Long, Long)            =   GuapSwapUtils.calculateTotalProtocolFeePercentage(parameters.guapswapProtocolSettings.serviceFees.protocolFeePercentage, parameters.guapswapProtocolSettings.serviceFees.protocolUIFeePercentage)
            val protocolFeePercentageNum:       ErgoValue[Long]         =   ErgoValue.of(protocolFeePercentageFraction._1)
            val protocolFeePercentageDenom:     ErgoValue[Long]         =   ErgoValue.of(protocolFeePercentageFraction._2)
            val protocolFeeContract:            ErgoValue[Coll[Byte]]   =   ErgoValue.of(Address.create(GuapSwapUtils.GUAPSWAP_PROTOCOL_FEE_CONTRACT_SAMPLE).getErgoAddress().script.bytes)

            // Generate blockchain context
            val p2sSwapSellProxyScript: String = ergoClient.execute((ctx: BlockchainContext) => {

                // Compile the script into an ErgoContract
                val ergocontract: ErgoContract = ctx.compileContract(
                    ConstantsBuilder.create()
                        .item("PK",                                 userPublicKey.getValue())
                        .item("ErgoDexSwapSellContractSample",      swapSellContractSample.getValue())
                        .item("GuapSwapProtocolFeePercentageNum",   protocolFeePercentageNum.getValue())
                        .item("GuapSwapProtocolFeePercentageDenom", protocolFeePercentageDenom.getValue())
                        .item("GuapSwapProtocolFeeContract",        protocolFeeContract.getValue())
                        .build(),
                        swapSellProxyScript
                )

                // Convert the ErgoContract into a P2S Address 
                val p2sAddress = Address.fromErgoTree(ergocontract.getErgoTree(), ctx.getNetworkType())
                p2sAddress.asP2S.toString()

            })

            p2sSwapSellProxyScript
            
        }

        def guapswapOneTime(ergoClient: ErgoClient, parameters: GuapSwapParameters, proxyAddress: String) = {
            
            // TODO: Check the parameters to make sure it corresponds to the appropriate DEX
            // Get the ergodex proxy script
            val swapSellSellProxyScript: String = GuapSwapErgoDexSwapSellProxyContract.getScript

            // Convert pool contract P2S address to an Address
            val poolContractAddress: Address = Address.create(ErgoDexUtils.ERGODEX_POOL_CONTRACT_ADDRESS)

            // Convert proxy contract P2S address to an Address
            val proxyContractAddress: Address = Address.create(proxyAddress)

            // Get the proxy box based on the token swap type
            val ticker: String = parameters.dexSettings.ergodexSettings.swapAssetTicker
            
            // Get the correct pool type
            val poolSearchName: String = ticker match {
                case "SigUSD"  => "ERG_2_SigUSD"
                case "SigRSV"  => "ERG_2_SigRSV"
                case "Erdoge"  => "ERG_2_Erdoge"
                case "LunaDog" => "ERG_2_LunaDog"
            }

            // Get the poolId based on the pool type
            val poolIdSearch: String = ErgoDexUtils.validErgoDexPools.get(poolSearchName).get.poolId

            // Generate blockchain context
            val guapswapOneTimeTxId: String = ergoClient.execute((ctx: BlockchainContext) => {

                // Search for the pool box based on the pool Id
                val poolBoxes: List[InputBox] = ctx.getUnspentBoxesFor(poolContractAddress, 0, 20).asScala.toList
                val poolBox: InputBox = poolBoxes.filter(poolbox => poolbox.getTokens().get(0).getId().toString.equals(poolIdSearch))(0)
                
                // Search for all the proxy boxes => assumes a max payout of 1/hr for one week
                val proxyBoxes: List[InputBox] = ctx.getUnspentBoxesFor(proxyContractAddress, 0, 168).asScala.toList
                
                // Generate the swap sell parameters
                val swapSellParams:         ErgoDexSwapSellParams   = ErgoDexSwapSellParams.swapSellParams(parameters, poolBox, proxyBoxes)
                val protocolMinerFee:       ErgoValue[Long]         = ErgoValue.of(GuapSwapUtils.convertMinerFee(parameters.guapswapProtocolSettings.serviceFees.protocolMinerFee))
                val minErgoDexExecutionFee: ErgoValue[Long]         = ErgoValue.of(ErgoDexUtils.calculateMinExecutionFee(GuapSwapUtils.convertMinerFee(parameters.dexSettings.ergodexSettings.ergodexMinerFee)))

                // Insert swap parameters into contract and substitute the constants
                val substSwapSellContractSample: ErgoValue[Coll[Byte]] = ErgoDexSwapSellParams.getSubstSwapSellContractWithParams(swapSellParams)
                
                
                
                
                ""
            })


            ""
        }
    }

}