package app

import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._

import scala.collection.JavaConverters._

import org.ergoplatform.{ErgoAddress, Pay2SAddress, P2PKAddress}
import org.ergoplatform.appkit._

import configs.parameters.GuapSwapParameters
import contracts.{GuapSwapDexSwapSellProxyContract, GuapSwapProtocolFeeContract}
import dex.ergodex.{ErgoDexUtils, ErgoDexSwap, ErgoDexSwapSellParams}
import protocol.GuapSwapUtils

import sigmastate.{Values, SType}
import sigmastate.Values.{EvaluatedValue, ErgoTree}
import sigmastate.serialization.ErgoTreeSerializer
import sigmastate.interpreter.ContextExtension
import sigmastate.utxo.Deserialize

import special.collection.Coll
import special.sigma.SigmaProp

import java.{util => ju}
import scala.util.{Try, Success, Failure}

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
            Opts.subcommand(name = "swap", help = "Loops indefinitely, querying Ergo blockchain once every interval to check for a payout and perform the swap automatically.") {
                
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
                val dexSwapSellProxyErgoContract: ErgoContract = GuapSwapUtils.getDexSwapSellProxyErgoContract(ctx, parameters)
                
                // Convert the ErgoContract into a P2S ErgoAddress 
                val dexSwapSellProxyAddress: ErgoAddress = Address.fromErgoTree(dexSwapSellProxyErgoContract.getErgoTree(), ctx.getNetworkType()).getErgoAddress()
                dexSwapSellProxyAddress.toString()

            })

            dexSwapSellProxyAddressString
            
        }

        /**
          * Perform a onetime swap.
          *
          * @param ergoClient
          * @param parameters
          * @param proxyAddress
          * @param unlockedSecretStorage
          * @return Onetime GuapSwap transaction ID string.
          */
        def guapswapOneTime(ergoClient: ErgoClient, parameters: GuapSwapParameters, proxyAddress: String, unlockedSecretStorage: SecretStorage): String = {
            
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
            val guapswapOneTimeTxId: String = ergoClient.execute((ctx: BlockchainContext) => {
                
                // Search for the pool box based on the pool Id => THERE SHOULD ONLY BE ONE SUCH BOX
                var poolBoxes: List[InputBox] = List.empty[InputBox]
                try {
                    poolBoxes = ctx.getUnspentBoxesFor(poolContractAddress, 0, 20).asScala.toList
                } catch {
                    case exception: IllegalArgumentException => {
                        println("No ErgoDex pool boxes exist.")
                        throw exception
                    }
                }

                // Get the pool box based on the matching token id
                val poolBox: InputBox = poolBoxes.filter(poolbox => poolbox.getTokens().get(0).getId().toString.equals(poolIdSearch))(0)
                
                // Search for all the proxy boxes 
                val proxyBoxes: List[InputBox] = ctx.getUnspentBoxesFor(proxyContractAddress, 0, 50).asScala.toList
                proxyBoxes.foreach(proxy => println(proxy))
                val totalPayout: Long = proxyBoxes.foldLeft(0L)((acc, proxybox) => acc + proxybox.getValue())

                // Generate the swap sell parameters
                val swapSellParams: ErgoDexSwapSellParams = ErgoDexSwapSellParams.swapSellParams(parameters, poolBox, proxyBoxes)
                val newSwapSellContractSamples: (ErgoValue[Coll[Byte]], ErgoValue[Coll[Byte]]) = ErgoDexSwapSellParams.getSubstSwapSellContractWithParams(swapSellParams)

                // Get context variables
                val minValueOfTotalErgoDexFees:         ErgoValue[Long]         =   ErgoValue.of(ErgoDexUtils.minValueOfTotalErgoDexFees(ErgoDexUtils.calculateMinExecutionFee(swapSellParams.paramMaxMinerFee.getValue()), swapSellParams.paramMaxMinerFee.getValue()))
                val newSwapSellContractSampleWithoutPK: ErgoValue[Coll[Byte]]   =   newSwapSellContractSamples._1
                val contextVarCheck:                    ErgoValue[Long]         =   GuapSwapUtils.CONTEXT_VAR_CHECK
            
                // Create context extension variables
                var cVar0: ContextVar = ContextVar.of(0.toByte, contextVarCheck)
                var cVar1: ContextVar = ContextVar.of(1.toByte, minValueOfTotalErgoDexFees)
                var cVar2: ContextVar = ContextVar.of(2.toByte, newSwapSellContractSampleWithoutPK)

                // Create input boxs with context variables
                val extendedProxyInputBoxes: List[InputBox] = proxyBoxes.map(proxybox => proxybox.withContextVars(cVar0, cVar1, cVar2))
                val extendedInputs: ju.List[InputBox] = seqAsJavaList(extendedProxyInputBoxes)

                 // Protocol fee compiled ErgoContract and ErgoAddress
                val protocolFeeErgoContract:    ErgoContract    =   GuapSwapUtils.getProtocolFeeErgoContract(ctx)
                val protocolFeeAddress:         ErgoAddress     =   Address.fromErgoTree(protocolFeeErgoContract.getErgoTree(), ctx.getNetworkType()).getErgoAddress()
                val protocolFee:                Long            =   GuapSwapUtils.calculateTotalProtocolFee(parameters.guapswapProtocolSettings.serviceFees.protocolFeePercentage, parameters.guapswapProtocolSettings.serviceFees.protocolUIFeePercentage, totalPayout)
                val guapSwapMinerFee:           Long            =   GuapSwapUtils.convertMinerFee(parameters.guapswapProtocolSettings.serviceFees.protocolMinerFee)
                val serviceFee:                 Long            =   GuapSwapUtils.calculateServiceFee(protocolFee, guapSwapMinerFee)
                
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
                    .fee(guapSwapMinerFee)
                    .sendChangeTo(protocolFeeAddress)
                    .build();
                
                // Sign transaction
                val signedGuapSwapTx: SignedTransaction = prover.sign(unsignedGuapSwapTx)
                val guapswapTxId: String = ctx.sendTransaction(signedGuapSwapTx)
                guapswapTxId

            })

            // Remove quotation marks from string.
            guapswapOneTimeTxId.replaceAll("\"", "")
        }

        /**
          * Launch the automatic swap
          *
          * @param ergoClient
          * @param parameters
          * @param proxyAddress
          * @param unlockedSecretStorage
          */
        def guapswapAutomatic(ergoClient: ErgoClient, parameters: GuapSwapParameters, proxyAddress: String, unlockedSecretStorage: SecretStorage): Unit = {
            
            // Print notification statement
            println(Console.BLUE + "========== Program will run INDEFINITELY and will NOT ask for confirmation to SIGN the TX. To TERMINATE execution, close the terminal session. ==========" + Console.RESET)

            // Convert swap interval into milliseconds
            val milliSecondsPerMinute: Long = 1000 * 60
            val minutesPerHour: Long = 60
            var minutes: Long = minutesPerHour * parameters.guapswapProtocolSettings.swapIntervalInHours
            
            // Set the default swap time interval to be 60 minutes, this gives enough time for block confirmation
            if (minutes < 60) {
                minutes = 60
            }
            
            // Calculate the time based on user config settings
            val time: Long = milliSecondsPerMinute * minutes
            //val test: Long = 15000

            while (true) {

                try {
                    // Print guapswap automatic initiated status message
                    println(Console.YELLOW + "========== GUAPSWAP AUTOMATIC TX INITIATED ==========" + Console.RESET)
                    val automaticSwapTxId: String = guapswapOneTime(ergoClient, parameters, proxyAddress, unlockedSecretStorage)

                    // Perform a swap
                    println(Console.GREEN + "========== GUAPSWAP AUTOMATIC TX SUCCESSFULL ==========" + Console.RESET)

                    // Print out guapswap save tx status message
                    println(Console.GREEN + "========== GUAPSWAP AUTOMATIC TX SAVED ==========" + Console.RESET)
                    GuapSwapUtils.save(automaticSwapTxId, GuapSwapUtils.GUAPSWAP_SWAP_FILE_PATH)
                            
                    // Print tx link to the user
                    println(Console.BLUE + "========== VIEW GUAPSWAP AUTOMATIC TX IN THE ERGO-EXPLORER WITH THE LINK BELOW ==========" + Console.RESET)
                    println(GuapSwapUtils.ERGO_EXPLORER_TX_URL_PREFIX + automaticSwapTxId)

                } catch {
                    case e: Throwable => {
                        println(Console.RED + "========== NO VALID PROXY BOX FOUND FOR THE AUTOMATIC SWAP TX ==========" + Console.RESET)
                    }
                }
            
                // Print warning and put the thread to sleep for the alloted interval of time
                println(Console.BLUE + s"========== AUTOMATIC TX ATTEMPT WILL OCCUR AGAIN WITHIN THE NEXT ${minutes} MINUTES ==========" + Console.RESET)
                Thread.sleep(time)
            }   

        }


        /**
          * Refund transaction.
          *
          * @param ergoClient
          * @param parameters
          * @param proxyAddress
          * @param unlockedSecretStorage
          * @return Refund transaction ID string.
          */
        def guapswapRefund(ergoClient: ErgoClient, parameters: GuapSwapParameters, proxyAddress: String, unlockedSecretStorage: SecretStorage): String = {
            
            // Generate blockchain context
            val refundTxId: String = ergoClient.execute((ctx: BlockchainContext) => {
                
                 // Convert proxy contract P2S address to an Address
                val proxyContractAddress: Address = Address.create(proxyAddress)

                // Search for all the proxy boxes => assumes a max payout of 1/hr for one week (i.e. 168 maxmimum boxes)
                val proxyBoxes:                     List[InputBox]          =   ctx.getUnspentBoxesFor(proxyContractAddress, 0, 168).asScala.toList
                val totalPayout:                    Long                    =   proxyBoxes.foldLeft(0L)((acc, proxybox) => acc + proxybox.getValue())
                val guapSwapMinerFee:               Long                    =   GuapSwapUtils.convertMinerFee(parameters.guapswapProtocolSettings.serviceFees.protocolMinerFee)
                val refundValue:                    Long                    =   totalPayout - guapSwapMinerFee
                val contextVarCheckFail:            ErgoValue[Long]         =   GuapSwapUtils.CONTEXT_VAR_CHECK_FAIL 
                val contextVarPlaceholderLong:      ErgoValue[Long]         =   GuapSwapUtils.CONTEXT_VAR_PLACEHOLDER_LONG
                val contextVarPlaceholderCollByte:  ErgoValue[Coll[Byte]]   =   GuapSwapUtils.CONTEXT_VAR_PLACEHOLDER_COLL_BYTES

                // Create context extension variables
                var cVar0: ContextVar = ContextVar.of(0.toByte, contextVarCheckFail)
                var cVar1: ContextVar = ContextVar.of(1.toByte, contextVarPlaceholderLong)
                var cVar2: ContextVar = ContextVar.of(2.toByte, contextVarPlaceholderCollByte)

                // Create input boxs with context variables
                val extendedProxyInputBoxes: List[InputBox] = proxyBoxes.map(proxybox => proxybox.withContextVars(cVar0, cVar1, cVar2))
                val extendedInputs: ju.List[InputBox] = seqAsJavaList(extendedProxyInputBoxes)

                // User PK address
                val userPK: Address = Address.create(parameters.guapswapProtocolSettings.userAddress)
                
                // Create tx builder
                val txBuilder: UnsignedTransactionBuilder = ctx.newTxBuilder();

                // Create output refund box
                val refundBox: OutBox = txBuilder.outBoxBuilder()
                    .value(refundValue)
                    .contract(ctx.newContract(userPK.getErgoAddress().script))
                    .build();

                // Create prover
                val prover: ErgoProver = ctx.newProverBuilder()
                    .withSecretStorage(unlockedSecretStorage)
                    .build();

                // Create unsigned transaction
                val unsignedRefundTx: UnsignedTransaction = txBuilder.boxesToSpend(extendedInputs)
                    .outputs(refundBox)
                    .fee(guapSwapMinerFee)
                    .sendChangeTo(userPK.getErgoAddress())
                    .build();
                
                // Sign transaction
                val signedRefundTx: SignedTransaction = prover.sign(unsignedRefundTx)
                val refundTxId: String = ctx.sendTransaction(signedRefundTx)
                refundTxId

            })

            // Remove quotation marks from string.
            refundTxId.replaceAll("\"", "")
        }

        def guapswapList(ergoClient: ErgoClient, proxyAddress: String): Unit = {

            // Generate the blockchain context
            ergoClient.execute((ctx: BlockchainContext) => {
                
                // Convert proxy contract P2S address to an Address
                val proxyContractAddress: Address = Address.create(proxyAddress)

                try {

                    // Search for all the proxy boxes => assumes a max payout of 1/hr for one week (i.e. 168 maxmimum boxes)
                    val proxyBoxes: List[InputBox] = ctx.getUnspentBoxesFor(proxyContractAddress, 0, 168).asScala.toList
                
                    // Print the proxy boxes
                    proxyBoxes.foreach(proxy => println(proxy.toJson(true)))

                } catch {
                    case noNodeConnect: ErgoClientException => noNodeConnect 
                    case noIndex: IndexOutOfBoundsException =>  println(Console.RED + "========== NO PROXY BOXES AT THE GIVEN ADDRESS FOUND ==========" + Console.RESET)
                }
            
            })

        }

    }

}