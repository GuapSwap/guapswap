package dex.ergodex

import org.ergoplatform.appkit.ErgoValue
import special.collection.Coll
import special.sigma.SigmaProp

/**
  * Class representing the parameters to be provided to the ErgoDexSwapSell contract
  *
  * @param paramPK
  * @param paramFeeNum
  * @param paramQuoteId
  * @param paramMinQuoteAmount
  * @param paramBaseAmount
  * @param paramDexFeePerTokenNum
  * @param paramDexFeePerTokenDenom
  * @param paramMaxMinerFee
  * @param paramPoolNFTId
  */
case class ErgoDexSwapSellParams(
    val paramPK: ErgoValue[SigmaProp],
    val paramFeeNum: ErgoValue[Long],
    val paramQuoteId: ErgoValue[Coll[Byte]],
    val paramMinQuoteAmount: ErgoValue[Long],
    val paramBaseAmount: ErgoValue[Long],
    val paramDexFeePerTokenNum: ErgoValue[Long],
    val paramDexFeePerTokenDenom: ErgoValue[Long],
    val paramMaxMinerFee: ErgoValue[Long],
    val paramPoolNFTId: ErgoValue[Coll[Byte]]
)