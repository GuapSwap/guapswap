package protocol

/**
  * Class to represent a dex asset.
  *
  * @param ticker
  * @param id
  * @param decimals
  */
case class DexAsset(
  val ticker: String,
  val id: String,
  val decimals: Int
)