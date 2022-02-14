package contracts

object GuapSwapErgoDexSwapSellProxyContract {
    def getScript: String = {
        val script: String = s"""
        {
            // ====== Contract Info ====== //
            // Description: Proxy contract which holds the miner's payout from the mining pool, and will perform a swap-sell with ErgoDex.
            // Author: Luca D’Angelo

            // ====== Contract Hard-Coded Constants ====== //
            val PK: SigmaProp
            val ErgoDexSwapSellContractSample: Coll[Byte]
            val GuapSwapServiceFeePercentageNum: Long = 5  
            val GuapSwapServiceFeePercentageDenom: Long = 1000
            val GuapSwapServiceFeeContract: Coll[Byte]
            val GuapSwapMinerFee: Long
            val MinErgoDexExecutionFee: Long

            // ====== ErgoDex Settings Variables ====== //
            // First column of indicies: Index of "getVar[T](tag: Int): Option[T]" corresponding to the appropriate ContextVariable.
            // Second column of indicies: Index of "positions: Coll[Int]" parameter of "substContants[T](scriptBytes: Coll[Bytes], positions: Coll[Int], newValues: Coll[T]): Coll[Byte]" corresponding to the associated variable that is to be inserted into the ErgoDex swap contract template.
            // 0  => FeeNum: Long                   => 14, 18
            // 1  => QuoteId: Coll[Byte]            => 9
            // 2  => MinQuoteAmount: Long           => 10
            // 3  => BaseAmount: Long               => 2, 17
            // 4  => DexFeePerTokenNum: Long        => 11
            // 5  => DexFeePerTokenDenom: Long      => 12
            // 6  => MaxMinerFee: Long              => 22
            // 7  => PoolNFT: Coll[Byte]            => 8
            // 9  => NewPK: SigmaProp               => 0 (Includes the SigmaPropConstPrefixHex added to the original miner PK)

            // Assigning the corresponding ErgoDex variables their value from the transaction context.
            val FeeNum: Long                = getVar[Long](0).get
            val QuoteId: Coll[Byte]         = getVar[Coll[Byte]](1).get
            val MinQuoteAmount: Long        = getVar[Long](2).get
            val BaseAmount: Long            = getVar[Long](3).get
            val DexFeePerTokenNum: Long     = getVar[Long](4).get
            val DexFeePerTokenDenom: Long   = getVar[Long](5).get
            val MaxMinerFee: Long           = getVar[Long](6).get
            val PoolNFT: Coll[Byte]         = getVar[Coll[Byte]](7).get
            val NewPK: SigmaProp            = getVar[SigmaProp](9).get

            // Replacing the ErgoDex variable values in the SwapSell template with their corresponding value from the transaction context.
            // val positions_Long: Coll[Int] = Coll(2, 10, 11, 12, 14, 17, 18, 22)
            // val positions_Coll_Byte: Coll[Int] = Coll(8, 9)
            // val positions_SigmaProp: Coll[Int] = Coll(0)

            // val newValues_Long: Coll[Long] = Coll(
            //     BaseAmount,
            //     MinQuoteAmount,
            //     DexFeePerTokenNum,
            //     DexFeePerTokenDenom,
            //     FeeNum,
            //     BaseAmount,
            //     FeeNum,
            //     MaxMinerFee
            // )

            // val newValues_Coll_Byte: Coll[Coll[Bytes]] = Coll(
            //     PoolNFT,
            //     QuoteId
            // )

            // val newValues_SigmaProp: Coll[SigmaProp] = Coll(
            //     NewPK
            // )

            // Alternatively
            val positions: Coll[Int] = Coll(0, 2, 8, 9, 10, 11, 12, 14, 17, 18, 22)

            val newValues: Coll[Any] = Coll(
                NewPK.toSigmaProp,
                BaseAmount.toLong,
                PoolNFT.toCollOfByte,
                QuoteId.toCollOfByte,
                MinQuoteAmount.toLong,
                DexFeePerTokenNum.toLong,
                DexFeePerTokenDenom.toLong,
                FeeNum.toLong,
                BaseAmount.toLong,
                FeeNum.toLong,
                MaxMinerFee.toLong
            )

            // Will insert new values based on the following order: Long => Coll[Byte] => SigmaProp
            // val newErgoDexSwapSellTemplate_Long: Coll[Byte] = substConstants(ErgoDexSwapSellTemplate, positions_Long, newValues_Long)
            // val newErgoDexSwapSellTemplate_Coll_Byte: Coll[Byte] = substConstants(newErgoDexSwapSellTemplate_Long, positions_Coll_Byte, newValues_Coll_Byte)
            // val newErgoDexSwapSellTemplate_SigmaProp: Coll[Byte] = substConstants(newErgoDexSwapSellTemplate_Coll_Byte, positions_SigmaProp, newValues_SigmaProp)
            // val newErgoDexSwapSellTemplate: Coll[Byte] = newErgoDexSwapSellTemplate_SigmaProp

            val newErgoDexSwapSellTemplate: Coll[Byte] = substConstants(ErgoDexSwapSellContractTemplate, positions, newValues)

            // ====== GuapSwap ErgoDex SwapSell Proxy Contract Conditions ====== //
            // Check that a valid ErgoDex SwapSell Box is an output.
            val validErgoDexSwapBox = {
                val userSwapBox: Box = OUTPUTS(0)
                val minValueOfFees: Long = (GuapSwapService FeeNum * SELF.value / GuapSwapServiceFeeDenom) + GuapSwapMinerFee + MinErgoDexExecutionFee + MaxMinerFee
                BaseAmount >= minValueOfFees &&
                SELF.value >= BaseAmount &&
                userSwapBox.value >= SELF.value - BaseAmount - minValueOfFees && 
                userSwapBox.propositionBytes == newErgoDexSwapSellTemplatePropBytes
            }

            // Check that a valid GuapSwap Service Fee Box is an output.
            val validGuapSwapServiceFeeBox = {
                val serviceFeeBox: Box = OUTPUTS(1)
                serviceFeeBox.value == (GuapSwapServiceFeeNum * SELF.value / GuapSwapServiceFeeDenom) &&
                serviceFeeBox.propositionBytes == GuapSwapServiceFeeContractTemplate
            }

            // Check that a valid Refund Box is an output if initiated by user.
            val validRefundBox = {
                val refundBox: Box = OUTPUTS(0)
                refundBox.value == SELF.value - MinBoxValue - (GuapSwapServiceFeeNum * SELF.value / GuapSwapServiceFeeDenom) - GuapSwapMinerFee &&
                refundBox.propositionBytes == PK
            }

            // For a valid swap to occur, the following conditions must be met.
            val validGuapSwap = {
                validErgoDexSwapBox &&
                validGuapSwapServiceFeeBox &&
                OUTPUTS.size == 3
            }

            // For a valid refund to occur, the following conditions must be met.
            val validRefund = {
                validRefundBox &&
                validGuapSwapServiceFeeBox &&
                OUTPUTS.size == 3
            }

            // One of these three conditions must be met in order to validate the script and execute the transaction with the corresponding action.
            SigmaProp(validGuapSwap || validRefund || PK)
        }
        """.stripMargin
        script
    }
    
}