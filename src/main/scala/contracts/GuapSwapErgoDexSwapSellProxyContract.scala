package contracts

object GuapSwapErgoDexSwapSellProxyContract {
    def getScript: String = {
        val script: String = s"""
        {
            // ====== Contract Info ====== //
            // Filename: GuapSwapErgoDexSwapSellProxyContract.scala
            // Description: Proxy contract which holds the miner's payout from the mining pool, and will perform a swap-sell with ErgoDex.
            // Author: Luca D’Angelo

            // ====== Contract Hard-Coded Constants ====== //
            val PK: SigmaProp
            val ErgoDexSwapSellContractTemplate: Coll[Byte]
            val GuapSwapServiceFeePercentageNum: Long = 5  
            val GuapSwapServiceFeePercentageDen: Long = 1000
            val GuapSwapServiceFeeContractTemplate: Coll[Byte]
            val GuapSwapMinerFee: Long

            // ====== ErgoDex Settings Variables ====== //
            // First column of indicies: Index of "getVar[T](tag: Int): Option[T]" corresponding to the appropriate ContextVariable.
            // Second column of indicies: Index of "positions: Coll[Int]" parameter of "substContants[T](scriptBytes: Coll[Bytes], positions: Coll[Int], newValues: Coll[T]): Coll[Byte]" corresponding to the associated variable that is to be inserted into the ErgoDex swap contract template.
            //
            // 0  => FeeNum: Long                   => 14, 18
            // 1  => QuoteId: Coll[Byte]            => 9
            // 2  => MinQuoteAmount: Long           => 10
            // 3  => BaseAmount: Long               => 2, 17
            // 4  => DexFeePerTokenNum: Long        => 11
            // 5  => DexFeePerTokenDenom: Long      => 12
            // 6  => MaxErgoDexMinerFee: Long       => 22
            // 7  => PoolNFT: Coll[Byte]            => 8
            // 9  => NewPK: SigmaProp               => 0 (Includes the SigmaPropConstPrefixHex added to the original miner PK)

            // Assigning the corresponding ErgoDex variables their value from the transaction context.
            val FeeNum: Long                = getVar[Long](0).get
            val QuoteId: Coll[Byte]         = getVar[Coll[Byte]](1).get
            val MinQuoteAmount: Long        = getVar[Long](2).get
            val BaseAmount: Long            = getVar[Long](3).get
            val DexFeePerTokenNum: Long     = getVar[Long](4).get
            val DexFeePerTokenDenom: Long   = getVar[Long](5).get
            val MaxErgoDexMinerFee: Long    = getVar[Long](6).get
            val PoolNFT: Coll[Byte]         = getVar[Coll[Byte]](7).get
            val NewPK: SigmaProp            = getVar[SigmaProp](9).get

            // Replacing the ErgoDex variable values in the SwapSell template with their corresponding value from the transaction context.
            val positions: Coll[Int] = Coll(0, 2, 8, 9, 10, 11, 12, 14, 17, 18, 22)

            val newValues: Coll[T] = Coll( // Do I need to call substConstants multiple times with different Coll[T] for the different types?
                NewPK,
                BaseAmount,
                PoolNFT,
                QuoteId,
                MinQuoteAmount,
                DexFeePerTokenNum,
                DexFeePerTokenDenom,
                FeeNum,
                BaseAmount,
                FeeNum,
                MaxErgoDexMinerFee
            )

            val newErgoDexSwapSellTemplate: Coll[Byte] = substConstants[T](ErgoDexSwapSellTemplate, positions, newValues)

            // ====== GuapSwap ErgoDex SwapSell Proxy Contract Conditions ====== //
            // Check that a valid ErgoDex SwapSell Box is an output.
            val validErgoDexSwapBox = {
                val userSwapBox = OUTPUTS(0)
                val validSwapBoxValue = SELF.value - ((GuapSwapServiceFeeNum / GuapSwapServiceFeeDen) * SELF.value) - GuapSwapMinerFee
                BaseAmount == validSwapBoxValue &&
                userSwapBox.value == validSwapBoxValue && 
                userSwapBox.propositionBytes == newErgoDexSwapSellTemplate
            }

            // Check that a valid GuapSwap Service Fee Box is an output.
            val validGuapSwapServiceFeeBox = {
                val serviceFeeBox = OUTPUTS(1)
                serviceFeeBox.value == (GuapSwapServiceFeeNum / GuapSwapServiceFeeDen) * SELF.value &&
                serviceFeeBox.propositionBytes == GuapSwapServiceFeeContract
            }

            // Check that a valid Refund Box is an output if initiated by user.
            val validRefundBox = {
                val refundBox = OUTPUTS(0)
                refundBox.value == SELF.value - ((GuapSwapServiceFeeNum / GuapSwapServiceFeeDen) * SELF.value) - GuapSwapMinerFee &&
                refundBox.propositionBytes == PK
            }

            val validGuapSwap = {
                validErgoDexSwapBox &&
                validGuapSwapServiceFeeBox &&
                OUTPUTS.size == 3
            }

            val validRefund = {
                validRefundBox &&
                OUTPUTS.size == 3
            }

            SigmaProp(validGuapSwap || validRefund || PK)
        }
        """.stripMargin
        script
    }
    
}