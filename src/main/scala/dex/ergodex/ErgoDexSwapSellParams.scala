package dex.ergodex

import org.ergoplatform.appkit.ErgoValue
import special.collection.Coll
import special.sigma.SigmaProp

/**
  * Class representing the parameters to be provided to the ErgoDexSwapSell contract
  *
  * @param PK
  * @param FeeNum
  * @param QuoteId
  * @param MinQuoteAmount
  * @param BaseAmount
  * @param DexFeePerTokenNum
  * @param DexFeePerTokenDenom
  * @param MaxMinerFee
  * @param PoolNFT
  */
case class ErgoDexSwapSellParams(
    val PK: ErgoValue[SigmaProp],
    val FeeNum: ErgoValue[Long],
    val QuoteId: ErgoValue[Coll[Byte]],
    val MinQuoteAmount: ErgoValue[Long],
    val BaseAmount: ErgoValue[Long],
    val DexFeePerTokenNum: ErgoValue[Long],
    val DexFeePerTokenDenom: ErgoValue[Long],
    val MaxMinerFee: ErgoValue[Long],
    val PoolNFT: ErgoValue[Coll[Byte]]
)