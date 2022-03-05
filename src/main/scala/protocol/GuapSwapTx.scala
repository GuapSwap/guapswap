package protocol

import configs.parameters.GuapSwapParameters
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.ErgoClient

/**
  * Class representing a GuapSwap tx, from which the tx swap sell parameters will be derived.
  *
  * @param txTypeId
  * @param inputs
  * @param outputs
  * @param ratios
  * @param parameters
  */
case class GuapSwapTx(
  val txTypeId: Int,
  val inputs: List[InputBox],
  val outputs: List[DexPool],
  val ratios: List[(Long, Long)],
  val parameters: GuapSwapParameters
)

/**
  * Companion object for abstract GuapSwap Tx
  */
case object GuapSwapTx {

  def getAbstractGuapSwapTx(ergoClient: ErgoClient, parameters: GuapSwapParameters, proxyAddress: String): GuapSwapTx = {

    // Determine tx type id: 1 => ERG -> T1 | 2 => ERG -> T1, T2 | 3 => ERG -> T1, T2, T3
    val txTypeId: Id = getTxTypeId(parameters)
    // get input boxes

    // get pools

    // get ratios

  }

  
  private def getTxTypeId(parameters: GuapSwapParameters): Int = {
    val x: Int = parameters.dexSettings.ergodexSettings.swapAssets.swapAsset1.

  }
  
}