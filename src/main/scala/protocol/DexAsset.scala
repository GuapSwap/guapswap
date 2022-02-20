package protocol

/**
  * Class to represent a dex asset.
  *
  * @param id
  * @param ticker
  * @param decimals
  */
case class DexAsset(
  val id: String,
  val ticker: String,
  val decimals: Int
)