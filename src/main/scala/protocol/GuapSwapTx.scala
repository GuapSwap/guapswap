package protocol

import scala.collection.JavaConverters._

import configs.parameters.GuapSwapParameters
import org.ergoplatform.appkit.{ErgoClient, InputBox, BlockchainContext}
import org.ergoplatform.appkit.Address
import dex.ergodex.ErgoDexUtils

/**
  * Class representing a GuapSwap tx, from which the tx swap sell parameters will be derived.
  *
  * @param txTypeId
  * @param inputsProxyBoxes
  * @param outputs
  * @param pools
  * @param ratios
  */
case class GuapSwapTx(
  val txTypeId: Int,
  val inputProxyBoxes: List[InputBox],
  val outputs: List[DexAsset],
  val pools: List[InputBox],
  val ratioFractions: List[(Long, Long)],
)

/**
  * Companion object for abstract GuapSwap Tx
  */
case object GuapSwapTx {

  /**
    * Get an object for the abstract representation of a GuapSwap tx. 
    *
    * @param ergoClient
    * @param parameters
    * @param proxyAddress
    * @return
    */
  def getAbstractGuapSwapTx(ergoClient: ErgoClient, parameters: GuapSwapParameters, proxyAddress: String): GuapSwapTx = {

    // Determine tx type id: 
    val txTypeId: Int = getTxTypeId(parameters)

    // Get input proxy boxes
    val inputProxyBoxes: List[InputBox] = getInputs(ergoClient, proxyAddress)

    // Get output dex assets, to keep track of assets being swapped using swap asset ticker
    val outputs: List[DexAsset] = getDexAssets(parameters, txTypeId)
        
    // Get pools
    val pools: List[InputBox] = getPools(ergoClient, parameters, txTypeId)

    // Get Ratios
    val ratioFractions: List[(Long, Long)] = getRatiosFractional(parameters)
    
    // Return abstract GuapSwap tx
    GuapSwapTx(txTypeId, inputProxyBoxes, outputs, pools, ratioFractions)
  }

  /**
    * Get the valid dex asset to store as an output of the abstract GuapSwap tx.
    *
    * @param ticker
    * @return A valid DexAsset
    */
  private def getDexAsset(ticker: String): DexAsset = {

    // Check if a valid ergo asset
    val asset: DexAsset = GuapSwapUtils.validErgoAssets.get(ticker) match {

      case Some(asset) => asset
      
      case None => throw new IllegalArgumentException("Invalid swap asset ticker.")
    }

    asset
  }

  /**
    * Get a list of the dex assets representing the output tokens.
    *
    * @param parameters
    * @param txTypeId
    * @return A list of DexAssets
    */
  private def getDexAssets(parameters: GuapSwapParameters, txTypeId: Int): List[DexAsset] = {

    val dexAssets: List[DexAsset] = txTypeId match {

      // ERG -> (T1) | T1 /= ERG
      case 1 => {

        // Get the ticker
        val ticker: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.swapAssetTicker
        
        if (ticker.equals("ERG")) {
          throw new IllegalArgumentException("ERG cannot be chosen for single token swap, please do a refund or pick a valid swap token instead.")
        }
        
        // Get the asset
        val asset: DexAsset = getDexAsset(ticker) 

        List(asset)

      }

      // ERG -> (T1, T2) | T1 == ERG or T?
      case 2 => {

        // Get the tickers
        val ticker1: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.swapAssetTicker
        val ticker2: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset2.swapAssetTicker

        // Get the assets
        val asset1: DexAsset = getDexAsset(ticker1)
        val asset2: DexAsset = getDexAsset(ticker2)

        List(asset1, asset2)
      }

      // ERG -> (T1, T2, T3) | T1 == ERG or T?
      case 3 => {

        // Get the tickers
        val ticker1: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.swapAssetTicker
        val ticker2: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset2.swapAssetTicker
        val ticker3: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset3.swapAssetTicker

        // Get the assets
        val asset1: DexAsset = getDexAsset(ticker1)
        val asset2: DexAsset = getDexAsset(ticker2)
        val asset3: DexAsset = getDexAsset(ticker3)

        List(asset1, asset2, asset3)
      }
    }

    // Return the dex assets representing the output tokens
    dexAssets

  }

  /**
    * Get the input proxy boxes from the Ergo blockchain at the given proxy address.
    *
    * @param ergoClient
    * @param proxyAddress
    * @return A list of all the input proxy boxes.
    */
  def getInputs(ergoClient: ErgoClient, proxyAddress: String): List[InputBox] = {

    // Get proxy boxes from blockchain at the proxy address
    ergoClient.execute((ctx: BlockchainContext) => {
      val proxyAddressContract: Address = Address.create(proxyAddress)
      val inputs = ctx.getUnspentBoxesFor(proxyAddressContract, 0, 100).asScala.toList
      inputs
    })
  
  }

  /**
    * Get the pool search name.
    *
    * @param ticker
    * @return The pool search name string
    */
  def getPoolSearchName(ticker: String) = {

    // Get the correct pool type
    val poolSearchName: String = ticker match {
        case "SigUSD"  => "ERG_2_SigUSD"
        case "SigRSV"  => "ERG_2_SigRSV"
        case "NETA"    => "ERG_2_NETA"
        case "ergopad" => "ERG_2_ergopad"
        case "Erdoge"  => "ERG_2_Erdoge"
        case "LunaDog" => "ERG_2_LunaDog"
    }

    poolSearchName

  }

  /**
    * Get the pool box from the Ergo blockchain using the pool search id, based on the asset ticker.
    *
    * @param ergoClient
    * @param poolAddressContract
    * @param poolSearchId
    * @return Corresponding pool box for chosen token swap.
    */
  def getPoolBox(ergoClient: ErgoClient, poolAddressContract: Address, ticker: String): InputBox = {

    // Get the pool search name
    val poolSearchName: String = getPoolSearchName(ticker)
    
    // Get the pool search id
    val poolSearchId: String = ErgoDexUtils.validErgoDexPools.get(poolSearchName).get.poolId
    
    // Get the pool boxes from the Ergo blockchain
    val poolBoxes: List[InputBox] = ergoClient.execute((ctx: BlockchainContext) => {
      ctx.getUnspentBoxesFor(poolAddressContract, 0, 20).asScala.toList
    })

    // Get the correct pool box with corresponding pool search ids
    val poolBox: InputBox = poolBoxes.filter(poolbox => poolbox.getTokens().get(0).getId().toString.equals(poolSearchId))(0)
    poolBox

  }

  /**
    * Get the pools for the corresponding asset tickers.
    *
    * @param ergoClient
    * @param parameters
    * @param txTypeId
    * @return A list of all the swap liquidity pools.
    * @throws IllegalArgumentExcption
    */
  def getPools(ergoClient: ErgoClient, parameters: GuapSwapParameters, txTypeId: Int): List[InputBox] = {
    
    val poolAddressContract: Address = Address.create(ErgoDexUtils.ERGODEX_POOL_CONTRACT_ADDRESS)

    val pools: List[InputBox] = txTypeId match {
      
      // ERG -> (T1) | T1 /= ERG
      case 1 => {

        // Get the proxy box based on the token swap type
        val ticker: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.swapAssetTicker

        if (ticker.equals("ERG")) {
          throw new IllegalArgumentException("ERG cannot be chosen for single token swap, please do a refund or pick a valid swap token instead.")
        }
        
        // Get the pool box
        val poolBox: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker)
        
        List(poolBox)

      }
      
      // ERG -> (T1, T2) | T1 == ERG or T?
      case 2 => {

        // Get the tickers
        val ticker1: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.swapAssetTicker
        val ticker2: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset2.swapAssetTicker

        // Only compute pool for ticker2 if ticker1 == ERG
        if (ticker1.equals("ERG")) {

          // Get pool for ticker 2
          val poolBox: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker2)
          
          List(poolBox)

        } else {

          // Get pools for ticker1 and ticker2
          val poolBox1: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker1)
          val poolBox2: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker2)

          List(poolBox1, poolBox2)

        }

      }

      // ERG -> (T1, T2, T3) | T1 == ERG or T?
      case 3 => {

        // Get the tickers
        val ticker1: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.swapAssetTicker
        val ticker2: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset2.swapAssetTicker
        val ticker3: String = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset3.swapAssetTicker

        // Only compute pool for ticker2 and ticker3 if ticker1 == ERG
        if (ticker1.equals("ERG")) {

          // Get pool for ticker 2
          val poolBox2: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker2)
          val poolBox3: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker3)
          
          List(poolBox2, poolBox3)

        } else {

          // Get pools for ticker1, ticker2, and ticker3
          val poolBox1: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker1)
          val poolBox2: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker2)
          val poolBox3: InputBox = getPoolBox(ergoClient, poolAddressContract, ticker3)

          List(poolBox1, poolBox2, poolBox3)
        }

      }

    }

    // Return pool
    pools

  }

  /**
    * Get the payout percentage ratios for the three swap tokens.s
    *
    * @param parameters
    * @return Tuple of the payout percentage ratios.
    */
  private def getRatios(parameters: GuapSwapParameters): (Double, Double, Double) = {
    val x: Double = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.percentageOfPayout
    val y: Double = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset2.percentageOfPayout
    val z: Double = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset3.percentageOfPayout

    (x, y, z)
  }

  /**
    * Get the payout percentage ratios for the three swap tokens as fractions.
    *
    * @param parameters
    * @return
    */
  private def getRatiosFractional(parameters: GuapSwapParameters): List[(Long, Long)] = {
    
    val ratios: (Double, Double, Double) = getRatios(parameters)
    
    val x: Double = ratios._1
    val y: Double = ratios._2
    val z: Double = ratios._3

    val xFrac: (Long, Long) = GuapSwapUtils.decimalToFraction(x)
    val yFrac: (Long, Long) = GuapSwapUtils.decimalToFraction(y)
    val zFrac: (Long, Long) = GuapSwapUtils.decimalToFraction(z)

    List(xFrac, yFrac, zFrac)

  }
  
  /**
    * Get the abstract transaction type id.
    * 
    * @param parameters
    * @return Abstract transaction type integer: 1 => (ERG -> T1) | 2 => (ERG -> T1, T2) | 3 => (ERG -> T1, T2, T3)
    * @throws IllegalArgumentException
    */
  private def getTxTypeId(parameters: GuapSwapParameters): Int = {
    
    val ratios: (Double, Double, Double) = getRatios(parameters)
    
    val x: Double = ratios._1
    val y: Double = ratios._2
    val z: Double = ratios._3

    if (x == 1 && y == 0 && z == 0) {
      1
    } else if (x > 0 && y > 0 && z == 0 && x + y == 1) {
      2
    } else if (x > 0 && y > 0 && z > 0 && x + y + z == 1) {
      3
    } else {
      throw new IllegalArgumentException("Invalid token settings in config file.")
    }

  }
  
}