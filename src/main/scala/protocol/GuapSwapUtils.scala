package protocol

import scala.collection.immutable.HashMap
import scala.util.{Try, Success, Failure}
import java.io.{File, FileNotFoundException}

import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit._

import configs.parameters.GuapSwapParameters
import configs.parameters.protocol_settings.GuapSwapProtocolSettings
import configs.parameters.dex_settings.GuapSwapDexSettings
import contracts.{GuapSwapDexSwapSellProxyContract, GuapSwapProtocolFeeContract}

import special.sigma.SigmaProp
import special.collection.Coll
import sigmastate.{Values}
import java.nio.file.Files
import java.io.FileWriter
import org.bouncycastle.util.encoders.UTF8
import java.nio.charset.Charset
import org.apache.commons.io.input.CharSequenceReader
import java.util.Date
import java.time.LocalDateTime
import java.time.ZoneId

/**
  * Object representing constants and methods relevant to GuapSwap.
  */
object GuapSwapUtils {

  // Constant representing the storage location within the project repository of the guapswap_config.json file and guapswap_proxy.json file
  final val GUAPSWAP_CONFIG_FILE_PATH:  String = "storage/guapswap_config.json"
  final val GUAPSWAP_PROXY_FILE_PATH:   String = "storage/guapswap_proxy.log"
  final val GUAPSWAP_SWAP_FILE_PATH:   String = "storage/guapswap_swap.log"
  final val GUAPSWAP_REFUND_FILE_PATH: String = "storage/guapswap_refund.log"

  // Ergo Explorer URL
  final val ERGO_EXPLORER_TX_URL_PREFIX: String = "https://explorer.ergoplatform.com/en/transactions/"

  // Default public node URL, from Ergo Platform
  final val DEFAULT_ERGOPLATFORM_MAINNET_API_URL: String = "http://213.239.193.208:9053/"
  final val DEFAULT_ERGOPLATFORM_TESTNET_API_URL: String = "http://213.239.193.208:9052/"

  // Default GetBlok TESTNET node URL
  final val DEFAULT_GETBLOK_TESTNET_STRATUM_URL: String = "http://ergo-testnet.getblok.io:3056"
  final val DEFAULT_GETBLOK_TESTNET_API_URL: String = "http://ergo-node-devnet.getblok.io:9052/"
  
  // Default secret storage directory
  final val DEFAULT_SECRET_STORAGE_DIRECTORY: String = "storage/.secretStorage"

  // Default protocol fee constants
  final val DEFAULT_PROTOCOL_FEE_PERCENTAGE:    Double = 0.0025D  // 0.0 for GuapSwap-Ronin CLI only
  final val DEFAULT_PROTOCOL_UI_FEE_PERCENTAGE: Double = 0.0D     // 0.0 for all CLI versions, only charged for web version with UI.
  final val DEFAULT_PROTOCOL_MINER_FEE:         Double = 0.002D

  // Test address
  final val MAINNET_TEST_ADDRESS: String = "9g462nhKH6kzQetBobihiD9x225SZcVz2cbJabgUzXRHNmTRb52"

  // Founder PKs
  final val JESPER_PK:  String = "9hy9jt1Vuq3fZr4rSYAUqo1r2dAJBBdazV6cL8FNuBQEvM6wXfR"
  final val GEORGE_PK:  String = "9hA5gTKrx1YsTDjYiSnYqsAWawMq1GbvaemobybpCZ8qyHFBXKF"
  final val LUCA_PK:    String = "9ej8AEGCpNxPaqfgisJTU2RmYG91bWfK1hu2xT34i5Xdw4czidX"

  // Context var check
  final val CONTEXT_VAR_CHECK: ErgoValue[Long] = ErgoValue.of(42069.toLong)
  final val CONTEXT_VAR_CHECK_FAIL: ErgoValue[Long] = ErgoValue.of(666.toLong)
  final val CONTEXT_VAR_PLACEHOLDER_LONG: ErgoValue[Long] = ErgoValue.of(42.toLong)
  final val CONTEXT_VAR_PLACEHOLDER_COLL_BYTES: ErgoValue[Coll[Byte]] = ErgoValue.of(Array(0.toByte))

  // HashMap of possible Ergo Assets
  final val validErgoAssets: HashMap[String, DexAsset] = HashMap(
    "ERG"     -> DexAsset("0", "ERG", 9),
    "SigUSD"  -> DexAsset("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", "SigUSD", 2),
    "SigRSV"  -> DexAsset("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", "SigRSV", 0),
    "NETA"    -> DexAsset("472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8", "NETA", 6),
    "ergopad" -> DexAsset("d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413", "ergopad", 2),
    "Erdoge"  -> DexAsset("36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", "Erdoge", 0),
    "LunaDog" -> DexAsset("5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", "LunaDog", 8),
    "kushti"  -> DexAsset("fbbaac7337d051c10fc3da0ccb864f4d32d40027551e1c3ea3ce361f39b91e40", "kushti", 0),
    "WT_ERG"  -> DexAsset("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b", "WT_ERG", 9),
    "WT_ADA"  -> DexAsset("30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e", "WT_ADA", 8)
  )

  /**
    * Convert from ERGs to nanoERGs
    *
    * @param erg
    * @return Converted ERG value in nanoErgs.
    */
  def ergToNanoErg(erg: Double): Long = {
    val fraction: (Long, Long) = decimalToFraction(erg)
    val nanoergs: Long = (fraction._1 * Parameters.OneErg) / fraction._2
    nanoergs
  }

  /**
    * Calculate the miner fee in nanoERGs
    *
    * @param minerFee
    * @return Miner fee in nanoERGs.
    */
  def convertMinerFee(minerFee: Double): Long = {
    val minerFeeNanoErgs = ergToNanoErg(minerFee)
    
    // Force miner fee to be > minimum box value required by Ergo blockchain.
    if (minerFeeNanoErgs < Parameters.MinFee) {
        ergToNanoErg(DEFAULT_PROTOCOL_MINER_FEE)
    } else {
        minerFeeNanoErgs
    }
  }

  /**
    * Calculate the total protocol fee percentage. 
    *
    * @param protocolFeePercentage
    * @param protocolUIFeePercentage
    * @return The numerator and denominator of the total protocol fee percentage
    */
  def calculateTotalProtocolFeePercentage(protocolFeePercentage: Double, protocolUIFeePercentage: Double): (Long, Long) = {
    val percentageSum: Double = protocolFeePercentage + protocolUIFeePercentage
    val fraction: (Long, Long) = decimalToFraction(percentageSum)
    fraction
  }

  /**
    * Calculate the total protocol fees, this include the ui fee and the procotol fee, but NOT the protocol miner fee
    *
    * @param protocolFee
    * @param protocolUIFee
    * @param payout
    * @return Total protocol fee in nanoErgs.
    */
  def calculateTotalProtocolFee(protocolFeePercentage: Double, protocolUIFeePercentage: Double, payout: Long): Long = ((protocolFeePercentage + protocolUIFeePercentage) * payout.toDouble).toLong

  /**
    * Calculate the service fee, this include the total protocol fee and the protocol miner fee
    *
    * @param totalProtocolFee
    * @param protocolMinerFee
    * @return Total service fee in nanoErgs.
    */
  def calculateServiceFee(totolProtocolFee: Long, protocolMinerFee: Long): Long = totolProtocolFee + protocolMinerFee

  /**
    * Method to calculate the minValue for the Guap Swap transaction to occur, including interaction with the dex.
    * 
    * @param serviceFee
    * @param totalDexFee The minium total fees charged by the dex, including mining fees at that stage.
    * @return The minimum value in nanoErgs that the transaction can cost, on both the GuapSwap stage and dex stage.
    */
  def minValueOfTotalFees(serviceFee: Long, totalDexFee: Long): Long = serviceFee + totalDexFee


  /**
    * Method to calculate the base amount to be swapped at the dex.
    *
    * @param payout Amount received from mining pool as reward
    * @param totalFee Total fees charged on payout.
    * @return Base amount to be swapped in nanoErgs.
    */
  def calculateBaseAmount(payout: Long, totalFee: Long): Long = payout - totalFee
  
  /**
    * Method to convert a decimal number to a rational fraction.
    *
    * @param number
    * @return Tuple of the numerator and denominator representing the decimal number.
    */
  def decimalToFraction(number: Double): (Long, Long) = {
    
    // Ignore if zero
    if (number == 0.0) {
      (0.toLong, 1.toLong)
    } else {
      // Format the number correctly for calculation such that there are no trailing zeros. 
      val bigdecimalNumber: BigDecimal = BigDecimal.apply(number).underlying().stripTrailingZeros()
      val bigdecimalToDouble: Double = bigdecimalNumber.doubleValue()
      val listMatch: List[String] = bigdecimalToDouble.toString.split("\\.").toList
      
      // Get the fractional representation of the decimal number
      val fractionTuple: (Long, Long) = listMatch match {
        case List(whole, fractional) => {
          val numDecimals: Double = fractional.length().toDouble
          val denominator: Long = Math.pow(10D, numDecimals).toLong
          val numerator: Long = whole.toLong * denominator + fractional.toLong
          (numerator, denominator)
        }
      }

      fractionTuple
    }
  }

  /**
    * Load secret storage
    * 
    * @return A Try[SecretStorage] statement containing the secret storage or the exception to handle.
    */
  def loadSecretStorage(): Try[SecretStorage] = Try {
   
    println(Console.YELLOW + "========== LOADING SECRET STORAGE ==========" + Console.RESET)
  
    val secretDirectory: File = new File(DEFAULT_SECRET_STORAGE_DIRECTORY)
    
    // Check if directory exists
    if (secretDirectory.isDirectory()) {

      // List files in the directory
      val files: Array[File] = secretDirectory.listFiles()

      // Check if there are files that exist in the directory
      if (files.length == 0) {
        throw new FileNotFoundException
      } else {
        SecretStorage.loadFrom(files(0))
      }

    } else {
      throw new FileNotFoundException
    }
    
  }

  /**
    * Generate secret storage
    *
    * @return The generated secret storage
    */
  def generateSecretStorage(): SecretStorage = {
    println(Console.YELLOW + "========== GENERATING SECRET STORAGE ==========" + Console.RESET)
    println(Console.YELLOW + "========== PLEASE CREATE A PASSWORD FOR SECRET STORAGE ==========" + Console.RESET)
    val password: String = System.console().readPassword().mkString
    
    val mnemonicPhrase: Array[Char] = Mnemonic.generateEnglishMnemonic().toCharArray()
    val mnemonic: Mnemonic = Mnemonic.create(SecretString.create(mnemonicPhrase), SecretString.empty())
    val generatedSecretStorage: SecretStorage = SecretStorage.createFromMnemonicIn(DEFAULT_SECRET_STORAGE_DIRECTORY, mnemonic, password)
    
    println(Console.GREEN + "========== SECRET STORAGE CREATED ==========" + Console.RESET)
    println(Console.BLUE + "========== YOUR MNEMONIC AND SECRET STORAGE ARE THE FOLLOWING ==========" + Console.RESET)
    println("Mnemonic Phrase: " + mnemonic.getPhrase().toStringUnsecure())
    println("Secret Storage Directory: " + generatedSecretStorage.getFile().getPath())
    
    generatedSecretStorage
  
  }

  /**
    * Unlock the locked secret storage.
    *
    * @param lockedSecretStorage
    * @return Unlocked secret storage.
    */
  def unlockSecretStorage(lockedSecretStorage: SecretStorage): Try[SecretStorage] = Try {
    println(Console.YELLOW + "===== PLEASE ENTER YOUR ENCRYPTION PASSWORD TO UNLOCK SECRET STORAGE" + Console.RESET)
    val passPhrase: String = System.console().readPassword().mkString
    
    // Unlock secret storage
    println(Console.YELLOW + "========== UNLOCKING SECRET STORAGE ==========")
    try {
        lockedSecretStorage.unlock(passPhrase)
    } catch {
        case exception: RuntimeException => {
            println(Console.RED + "========== WRONG PASSWORD ==========" + Console.RESET)
            throw exception
        }                   
    }
    println(Console.GREEN + "========== SECRET STORAGE UNLOCKED ==========")
    lockedSecretStorage
  }

  /**
    * Check secret storage
    *
    * @return The loaded or generated secret storage
    */
  def checkSecretStorage(): SecretStorage = {
    
    // Check and load secret storage
    val checkSecretStorage: SecretStorage = GuapSwapUtils.loadSecretStorage() match {
        
      case Success(loadedSecretStorage) => {
        println(Console.GREEN + "========== SECRET STORAGE EXISTS ==========" + Console.RESET)
        loadedSecretStorage
      }

      case Failure(exception) => {
          println(Console.RED + "========== SECRET STORAGE DOES NOT EXIST ==========" + Console.RESET)
          
          // Generate secret storage
          val generatedSecretStorage: SecretStorage = generateSecretStorage()
          generatedSecretStorage
      }
    }

    checkSecretStorage
  }

  /**
    * Get the compiled ErgoContract of the dex swap sell proxy script.
    *
    * @param ctx
    * @param parameters
    * @param dexSwapSellContractSample
    * @return Compiled ErgoContract of dex swap sell contract sample.
    */
  def getDexSwapSellProxyErgoContract(ctx: BlockchainContext, parameters: GuapSwapParameters): ErgoContract = {

    // Get the proxy contract ErgoScript
    val dexSwapSellProxyScript: String = GuapSwapDexSwapSellProxyContract.getScript

    // Get the hard-coded constants for the proxy contract
    val userPublicKey:                  ErgoValue[SigmaProp]    =   ErgoValue.of(Address.create(parameters.guapswapProtocolSettings.userAddress).getPublicKey())
    val protocolFeePercentageFraction:  (Long, Long)            =   GuapSwapUtils.calculateTotalProtocolFeePercentage(parameters.guapswapProtocolSettings.serviceFees.protocolFeePercentage, parameters.guapswapProtocolSettings.serviceFees.protocolUIFeePercentage)
    val protocolFeePercentageNum:       ErgoValue[Long]         =   ErgoValue.of(protocolFeePercentageFraction._1)
    val protocolFeePercentageDenom:     ErgoValue[Long]         =   ErgoValue.of(protocolFeePercentageFraction._2)
    val guapSwapMinerFee:               ErgoValue[Long]         =   ErgoValue.of(GuapSwapUtils.convertMinerFee(parameters.guapswapProtocolSettings.serviceFees.protocolMinerFee))
    val protocolFeeContract:            ErgoValue[Coll[Byte]]   =   ErgoValue.of(JavaHelpers.collFrom(getProtocolFeeErgoContract(ctx).getErgoTree().bytes), ErgoType.byteType())
  
    // Compile the script into an ErgoContract
    val dexSwapSellErgoContract: ErgoContract = ctx.compileContract(
      ConstantsBuilder.create()
        .item("UserPK",                             userPublicKey.getValue())
        .item("GuapSwapProtocolFeePercentageNum",   protocolFeePercentageNum.getValue())
        .item("GuapSwapProtocolFeePercentageDenom", protocolFeePercentageDenom.getValue())
        .item("GuapSwapProtocolFeeContract",        protocolFeeContract.getValue())
        .item("GuapSwapMinerFee",                   guapSwapMinerFee.getValue())
        .build(), 
      dexSwapSellProxyScript
    )

    dexSwapSellErgoContract

  }

  /**
    * Get the compiled ErgoContract of the protocol fee address.
    *
    * @param ctx
    * @param parameters
    * @return Compiled ErgoContract of protocol fee contract.
    */
  def getProtocolFeeErgoContract(ctx: BlockchainContext): ErgoContract = {
    
    // Protocol fee contract hard-coded constants  
    val protocolFeeContractScript:      String                  =   GuapSwapProtocolFeeContract.getScript
    val jesperPK:                       ErgoValue[SigmaProp]    =   ErgoValue.of(Address.create(GuapSwapUtils.JESPER_PK).getPublicKey())
    val georgePK:                       ErgoValue[SigmaProp]    =   ErgoValue.of(Address.create(GuapSwapUtils.GEORGE_PK).getPublicKey())
    val lucaPK:                         ErgoValue[SigmaProp]    =   ErgoValue.of(Address.create(GuapSwapUtils.LUCA_PK).getPublicKey())

    // Protocol fee contract compiled ErgoContract
    val protocolFeeErgoContract: ErgoContract = ctx.compileContract(
        ConstantsBuilder.create()
            .item("JesperPK", jesperPK.getValue())
            .item("GeorgePK", georgePK.getValue())
            .item("LucaPK", lucaPK.getValue())
            .build(),
            protocolFeeContractScript
    )
    protocolFeeErgoContract
  }

  /**
    * Save string to file log, with date and time in UTC.
    *
    * @param string
    * @param path
    */
  def save(string: String, path: String): Unit = {

    // Get access to the file
    val file: FileWriter = new FileWriter(path, true)

    // Get the date and time in UTC format
    val dateTime: LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))

    // Format the time string 
    val date: String = dateTime.toString().split("[T]")(0)
    val time: String = dateTime.toString().split("[T]")(1).split("\\.")(0)

    // Append text to file
    file.append(System.lineSeparator())
    file.append(s"[UTC ${date} ${time}] ${string}")

    // Close the file and io-stream
    file.close()  
  }
    
}
