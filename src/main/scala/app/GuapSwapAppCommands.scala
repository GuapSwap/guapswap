package app

import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._

import scala.collection.JavaConverters._

import org.ergoplatform.{ErgoAddress, Pay2SAddress, P2PKAddress}
import org.ergoplatform.appkit._

import configs.node.GuapSwapNodeConfig
import configs.parameters.GuapSwapParameters
import protocol.GuapSwapUtils
import contracts.{GuapSwapDexSwapSellProxyContract, GuapSwapProtocolFeeContract}
import dex.ergodex.{ErgoDexUtils, ErgoDexSwap, ErgoDexSwapSellParams}

import sigmastate.{Values, SType}
import sigmastate.Values.{EvaluatedValue, ErgoTree}
import sigmastate.serialization.ErgoTreeSerializer
import sigmastate.interpreter.ContextExtension
import sigmastate.utxo.Deserialize

import special.collection.Coll
import special.sigma.SigmaProp

import java.{util => ju}
import scala.util.{Try, Success, Failure}
import _root_.org.bouncycastle.jcajce.provider.symmetric.ARC4.Base
import org.apache.commons.math3.analysis.function.Add

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

            // Generate blockchain context
            val dexSwapSellProxyAddressString: String = ergoClient.execute((ctx: BlockchainContext) => {

                // Get compiled ErgoContract of proxy contract
                val dexSwapSellProxyErgoContract: ErgoContract = GuapSwapUtils.getDexSwapSellProxyErgoContract(ctx, parameters, ErgoDexUtils.ERGODEX_SWAPSELL_CONTRACT_SAMPLE)
                
                // Convert the ErgoContract into a P2S ErgoAddress 
                val dexSwapSellProxyAddress: ErgoAddress = Address.fromErgoTree(dexSwapSellProxyErgoContract.getErgoTree(), ctx.getNetworkType()).getErgoAddress()
                dexSwapSellProxyAddress.toString()

            })

            dexSwapSellProxyAddressString
            
        }

        def guapswapOneTime(ergoClient: ErgoClient, nodeConfig: GuapSwapNodeConfig, parameters: GuapSwapParameters, proxyAddress: String, unlockedSecretStorage: SecretStorage): String = {
            
            // TODO: Check the parameters to make sure it corresponds to the appropriate DEX
            // Get the dex proxy script
            val swapSellSellProxyScript: String = GuapSwapDexSwapSellProxyContract.getScript

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
            val guapswapOneTimeTx: String = ergoClient.execute((ctx: BlockchainContext) => {

                // Search for the pool box based on the pool Id => THERE SHOULD ONLY BE ONE SUCH BOX
                var poolBoxes: List[InputBox] = List.empty[InputBox]
                try {
                    poolBoxes = ctx.getUnspentBoxesFor(poolContractAddress, 0, 20).asScala.toList
                } catch {
                    case exception: IllegalArgumentException => {
                        println("No proxy boxes exists.")
                        throw exception
                    }
                }

                val poolBox: InputBox = poolBoxes.filter(poolbox => poolbox.getTokens().get(0).getId().toString.equals(poolIdSearch))(0)
                
                // Search for all the proxy boxes => assumes a max payout of 1/hr for one week
                val proxyBoxes: List[InputBox] = ctx.getUnspentBoxesFor(proxyContractAddress, 0, 168).asScala.toList
                val totalPayout: Long = proxyBoxes.foldLeft(0L)((acc, proxybox) => acc + proxybox.getValue())

                // Generate the swap sell parameters
                val swapSellParams: ErgoDexSwapSellParams = ErgoDexSwapSellParams.swapSellParams(parameters, poolBox, proxyBoxes)
                val newSwapSellContractSamples: (ErgoValue[Coll[Byte]], ErgoValue[Coll[Byte]]) = ErgoDexSwapSellParams.getSubstSwapSellContractWithParams(swapSellParams)

                // Get context variables
                val GuapSwapMinerFee:                   ErgoValue[Long]         =   ErgoValue.of(GuapSwapUtils.convertMinerFee(parameters.guapswapProtocolSettings.serviceFees.protocolMinerFee))
                val minValueOfTotalErgoDexFees:         ErgoValue[Long]         =   ErgoValue.of(ErgoDexUtils.minValueOfTotalErgoDexFees(ErgoDexUtils.calculateMinExecutionFee(swapSellParams.paramMaxMinerFee.getValue()), swapSellParams.paramMaxMinerFee.getValue()))
                val newSwapSellContractSampleWithoutPK: ErgoValue[Coll[Byte]]   =   newSwapSellContractSamples._1
                
                // Create context extension variables
                var cVar0: ContextVar = ContextVar.of(0.toByte, GuapSwapMinerFee)
                var cVar1: ContextVar = ContextVar.of(1.toByte, minValueOfTotalErgoDexFees)
                var cVar2: ContextVar = ContextVar.of(2.toByte, newSwapSellContractSampleWithoutPK)

                // Create input boxs with context variables
                val extendedProxyInputBoxes: List[InputBox] = proxyBoxes.map(proxybox => proxybox.withContextVars(cVar0, cVar1, cVar2))
                val extendedInputs: ju.List[InputBox] = seqAsJavaList(extendedProxyInputBoxes)

                 // Protocol fee compiled ErgoContract and ErgoAddress
                val protocolFeeErgoContract:    ErgoContract    =   GuapSwapUtils.getProtocolFeeErgoContract(ctx, parameters)
                val protocolFeeAddress:         ErgoAddress     =   Address.fromErgoTree(protocolFeeErgoContract.getErgoTree(), ctx.getNetworkType()).getErgoAddress()
                val protocolFee:                Long            =   GuapSwapUtils.calculateTotalProtocolFee(parameters.guapswapProtocolSettings.serviceFees.protocolFeePercentage, parameters.guapswapProtocolSettings.serviceFees.protocolUIFeePercentage, totalPayout)
                val serviceFee:                 Long            =   GuapSwapUtils.calculateServiceFee(protocolFee, GuapSwapUtils.convertMinerFee(parameters.guapswapProtocolSettings.serviceFees.protocolMinerFee))
                
                // Swap box contract and values
                val newSwapSellContractSample:  ErgoValue[Coll[Byte]]   =   newSwapSellContractSamples._2
                val swapBoxContract:            ErgoTree                =   ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(newSwapSellContractSample.getValue().toArray)
                val swapBoxValue:               Long                    =   totalPayout - serviceFee

                // Create tx builder
                val txBuilder: UnsignedTransactionBuilder = ctx.newTxBuilder();
                
                // Create output swap box
                val swapBox: OutBox = txBuilder.outBoxBuilder()
                    .value(swapBoxValue)
                    .contract(ctx.newContract(swapBoxContract))
                    .build();
                
                // Create output protocol fee box
                val protocolFeeBox: OutBox = txBuilder.outBoxBuilder()
                    .value(protocolFee)
                    .contract(protocolFeeErgoContract)
                    .build();

                // Create prover
                val prover: ErgoProver = ctx.newProverBuilder()
                    .withSecretStorage(unlockedSecretStorage)
                    .build();

                // Create unsigned transaction
                val unsignedGuapSwapTx: UnsignedTransaction = txBuilder.boxesToSpend(extendedInputs)
                    .outputs(swapBox, protocolFeeBox)
                    .fee(GuapSwapMinerFee.getValue())
                    .sendChangeTo(protocolFeeAddress)
                    .build();
                
                // Sign transaction
                val signedGuapSwapTx: SignedTransaction = prover.sign(unsignedGuapSwapTx)
                val guapswapTxId: String = ctx.sendTransaction(signedGuapSwapTx)
                guapswapTxId

            })

            guapswapOneTimeTx.replaceAll("\"", "")
        }

    }

}