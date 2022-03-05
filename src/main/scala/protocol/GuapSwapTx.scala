package protocol

import configs.parameters.GuapSwapParameters
import org.ergoplatform.appkit.InputBox

/**
  * Class representing a GuapSwap tx, from which the tx swap sell parameters will be derived.
  *
  * @param inputs
  */
case class GuapSwapTx(
    val inputs: List[InputBox],
    val outputs: List[DexPool],
    val ratios: List[(Long, Long)],
    val parameters: GuapSwapParameters
)