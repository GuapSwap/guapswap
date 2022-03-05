package dex.ergodex

import org.ergoplatform.appkit.ErgoValue
import special.collection.Coll
import protocol.DexPool
import org.ergoplatform.appkit.InputBox

/**
  * Class representing a GuapSwap Tx with all the necessary parameters.
  *
  * @param inputs
  */
case class ErgoDexGuapSwapTxParams(
    val inputs: List[InputBox],
    val outputs: List[DexPool],
    val ratios: List[(Long, Long)],
    val swapSellParams: List[ErgoDexSwapSellParams],
    val ergoTrees: List[ErgoValue[Coll[Byte]]]
)