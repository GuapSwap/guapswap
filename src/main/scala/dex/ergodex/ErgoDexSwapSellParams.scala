package dex.ergodex

import special.sigma.SigmaProp
import special.collection.Coll

case class ErgoDexSwapSellParams(
    val PK: SigmaProp,
    val FeeNum: Long,
    val QuoteId: Coll[Byte],
    val MinQuoteAmount: Long,
    val BaseAmount: Long,
    val DexFeePerTokenNum: Long,
    val DexFeePerTokenDenom: Long,
    val MaxMinerFee: Long,
    val PoolNFT: Coll[Byte]
)