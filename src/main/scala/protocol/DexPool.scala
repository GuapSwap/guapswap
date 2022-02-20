package protocol

import org.ergoplatform.ErgoBox.{TokenId}

/**
  * Class to represent a dex pool.
  *
  * @param PoolId
  * @param assetX
  * @param assetY
  */
case class DexPool(
  val poolId: String,
  val assetX: DexAsset,
  val assetY: DexAsset
)
