package protocol

/**
  * Class to represent a dex asset.
  *
  * @param ticker
  * @param id
  * @param decimals
  */
case class DexAsset(
  val id: String,
  val ticker: String,
  val decimals: Int
)