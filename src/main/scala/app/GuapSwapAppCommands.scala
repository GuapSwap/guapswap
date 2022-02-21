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
import contracts.{GuapSwapErgoDexSwapSellProxyContract, GuapSwapServiceFeeContract}
import dex.ergodex.{ErgoDexUtils, ErgoDexSwap, ErgoDexSwapSellParams}
import sigmastate.{Values, SType}
import sigmastate.Values.{EvaluatedValue, ErgoTree}
import sigmastate.serialization.ErgoTreeSerializer
import sigmastate.interpreter.ContextExtension
import sigmastate.utxo.Deserialize
import special.collection.Coll
import special.sigma.SigmaProp
import java.{util => ju}
import sigmastate.eval.Extensions
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.ErgoBoxCandidate
import org.ergoplatform.wallet.boxes.ErgoBoxSerializer
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.impl.ErgoScriptContract
import org.ergoplatform.wallet.mnemonic

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
            val protocolFeePercentageFraction:  (Long, Long)            =   GuapSwapUtils.calculateTotalProtocolFeePercentage(parameters.guapswapProtocolSettings.serviceFees.protocolFeePercentage, parameters.guapswapProtocolSettings.serviceFees.protocolUIFeePercentage)
            val protocolFeePercentageNum:       ErgoValue[Long]         =   ErgoValue.of(protocolFeePercentageFraction._1)
            val protocolFeePercentageDenom:     ErgoValue[Long]         =   ErgoValue.of(protocolFeePercentageFraction._2)
            val protocolFeeContract:            ErgoValue[Coll[Byte]]   =   ErgoValue.of(JavaHelpers.collFrom(Address.create(GuapSwapUtils.GUAPSWAP_PROTOCOL_FEE_CONTRACT_SAMPLE).getErgoAddress().script.bytes), ErgoType.byteType())

            // Generate blockchain context
            val p2sSwapSellProxyScript: String = ergoClient.execute((ctx: BlockchainContext) => {

                // Compile the script into an ErgoContract
                val ergocontract: ErgoContract = ctx.compileContract(
                    ConstantsBuilder.create()
                        .item("PK",                                 userPublicKey.getValue())
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

        def guapswapOneTime(ergoClient: ErgoClient, nodeConfig: GuapSwapNodeConfig, parameters: GuapSwapParameters, proxyAddress: String): String = {
            
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
            val guapswapOneTimeTx: String = ergoClient.execute((ctx: BlockchainContext) => {

                // Search for the pool box based on the pool Id => THERE SHOULD ONLY BE ONE SUCH BOX
                val poolBoxes: List[InputBox] = ctx.getUnspentBoxesFor(poolContractAddress, 0, 20).asScala.toList
                val poolBox: InputBox = poolBoxes.filter(poolbox => poolbox.getTokens().get(0).getId().toString.equals(poolIdSearch))(0)
                
                // Search for all the proxy boxes => assumes a max payout of 1/hr for one week
                val proxyBoxes: List[InputBox] = ctx.getUnspentBoxesFor(proxyContractAddress, 0, 168).asScala.toList
                val totalPayout: Long = proxyBoxes.foldLeft(0L)((acc, proxybox) => acc + proxybox.getValue())

                // Generate the swap sell parameters
                val swapSellParams: ErgoDexSwapSellParams = ErgoDexSwapSellParams.swapSellParams(parameters, poolBox, proxyBoxes)

                // Get protocol fee contract
                val protocolFeeContractAddress: ErgoAddress = Address.create(GuapSwapUtils.GUAPSWAP_PROTOCOL_FEE_CONTRACT_SAMPLE).asP2PK() // TODO: Change this to a P2S with proper protocol fee contract
                val protocolFeeContract: ErgoTree = protocolFeeContractAddress.script
                val protocolFee: Long = GuapSwapUtils.calculateTotalProtocolFee(parameters.guapswapProtocolSettings.serviceFees.protocolFeePercentage, parameters.guapswapProtocolSettings.serviceFees.protocolUIFeePercentage, totalPayout)
                
                // Get context variables
                val newSwapSellContractSample:  ErgoValue[Coll[Byte]]   = ErgoDexSwapSellParams.getSubstSwapSellContractWithParams(swapSellParams)
                val swapBoxContract:            ErgoTree                = ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(newSwapSellContractSample.getValue().toArray)
                val protocolMinerFee:           ErgoValue[Long]         = ErgoValue.of(GuapSwapUtils.convertMinerFee(parameters.guapswapProtocolSettings.serviceFees.protocolMinerFee))
                val dexMinerFee:                Long                    = GuapSwapUtils.convertMinerFee(parameters.dexSettings.ergodexSettings.ergodexMinerFee)
                val minValueOfTotalErgoDexFees: ErgoValue[Long]         = ErgoValue.of(ErgoDexUtils.minValueOfTotalErgoDexFees(ErgoDexUtils.calculateMinExecutionFee(dexMinerFee), dexMinerFee))
                val serviceFee:                 Long                    = GuapSwapUtils.calculateServiceFee(protocolFee, protocolMinerFee.getValue())
                val swapBoxValue:               Long                    = totalPayout - serviceFee
               
                // Create context extension variables
                var cVar1: ContextVar = ContextVar.of[Coll[Byte]](0, newSwapSellContractSample)
                var cVar2: ContextVar = ContextVar.of[Long](1, protocolMinerFee)
                var cVar3: ContextVar = ContextVar.of[Long](2, minValueOfTotalErgoDexFees)

                // Create input boxs with context variables
                val extendedProxyInputBoxes: List[InputBox] = proxyBoxes.map(proxybox => proxybox.withContextVars(cVar1, cVar2, cVar3))
                val extendedInputs: ju.List[InputBox] = seqAsJavaList(extendedProxyInputBoxes)
                
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
                    .contract(ctx.newContract(protocolFeeContract))
                    .build();

                // Create temp prover
                val userMnemonicSecretString: SecretString = SecretString.create(nodeConfig.wallet.mnemonic)
                val userAddress: Address = Address.create(parameters.guapswapProtocolSettings.userAddress)
                
                // val tempProver: ErgoProver = ctx.newProverBuilder()
                //     .withMnemonic(
                //         userMnemonicSecretString,
                //         SecretString.empty()
                //     )
                //     .withEip3Secret(0)
                //     .build();

                // Find EIP3 index using temporary prover
                // val userEIP3Addresses: ju.List[Address] = tempProver.getEip3Addresses()
                // val userEIP3Index: Int = userEIP3Addresses.indexOf(userAddress)

                // Create real prover with EIP3 index
                val prover: ErgoProver = ctx.newProverBuilder()
                    .withMnemonic(
                        userMnemonicSecretString,
                        SecretString.empty()
                    )
                    .build();

                // Create unsigned transaction
                val unsignedGuapSwapTx: UnsignedTransaction = txBuilder.boxesToSpend(extendedInputs)
                    .outputs(swapBox, protocolFeeBox)
                    .fee(protocolMinerFee.getValue())
                    .sendChangeTo(protocolFeeContractAddress)
                    .build();

                // Sign transaction
                val signedGuapSwapTx: SignedTransaction = prover.sign(unsignedGuapSwapTx)
                val guapswapTxId: String = ctx.sendTransaction(signedGuapSwapTx)
                signedGuapSwapTx.toJson(true)

            })

            guapswapOneTimeTx
        }

    }

}