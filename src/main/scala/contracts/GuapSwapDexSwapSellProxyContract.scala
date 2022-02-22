package contracts

import special.sigma.Box

/**
  * Object describing the GuapSwap Dex Swap Sell proxy contract.
  */
object GuapSwapDexSwapSellProxyContract {
    def getScript: String = {
        val script: String = s"""
        {
            // ===== Contract Info ===== //
            // Description: Proxy contract which holds the miner's payout from the mining pool, and will perform a swap-sell with the dex.
            // Author: Luca D’Angelo

            // ===== Contract Hard-Coded Constants ===== //
            // val UserPK:                              SigmaProp
            // val GuapSwapProtocolFeePercentageNum:    Long  
            // val GuapSwapProtocolFeePercentageDenom:  Long
            // val GuapSwapProtocolFeeContract:         Coll[Byte]
            // val DexSwapSellContractSample:           Coll[Byte]

            // ===== Contract Context Variables: Dex Settings Variables ===== //
            // First column of indicies: Index of "getVar[T](tag: Int): Option[T]" corresponding to the appropriate ContextVariable.
            // Second column of indicies: Index of "positions: Coll[Int]" parameter of "substContants[T](scriptBytes: Coll[Bytes], positions: Coll[Int], newValues: Coll[T]): Coll[Byte]" corresponding to the associated variable that is to be inserted into the Dex swap contract sample.
            // 0  => FeeNum:                Int         => 14, 18
            // 1  => QuoteId:               Coll[Byte]  => 9
            // 2  => MinQuoteAmount:        Long        => 10
            // 3  => BaseAmount:            Long        => 2, 17
            // 4  => DexFeePerTokenNum:     Long        => 11
            // 5  => DexFeePerTokenDenom:   Long        => 12
            // 6  => MaxMinerFee:           Long        => 22
            // 7  => PoolNFT:               Coll[Byte]  => 8
            // 8  => GuapSwapMinerFee:      Long 
            // 9  => TotalDexFee:           Long

            // Assigning the corresponding Dex variables their value from the transaction context.
            val FeeNum:                 Int         =   getVar[Int](0).get
            val QuoteId:                Coll[Byte]  =   getVar[Coll[Byte]](1).get
            val MinQuoteAmount:         Long        =   getVar[Long](2).get
            val BaseAmount:             Long        =   getVar[Long](3).get
            val DexFeePerTokenNum:      Long        =   getVar[Long](4).get
            val DexFeePerTokenDenom:    Long        =   getVar[Long](5).get
            val MaxMinerFee:            Long        =   getVar[Long](6).get
            val PoolNFT:                Coll[Byte]  =   getVar[Coll[Byte]](7).get
            val GuapSwapMinerFee:       Long        =   getVar[Long](8).get
            val TotalDexFee:            Long        =   getVar[Long](9).get

            // Replacing the Dex variable values in the SwapSell template with their corresponding value from the transaction context.
            val positions_Int:          Coll[Int]   =    Coll(14, 18)
            val positions_Long:         Coll[Int]   =    Coll(2, 10, 11, 12, 17, 22)
            val positions_Coll_Byte:    Coll[Int]   =    Coll(8, 9)
            val positions_SigmaProp:    Coll[Int]   =    Coll(0)

            // Values representing Int types.
            val newValues_Int: Coll[Int] = Coll(
                FeeNum,
                FeeNum
            )

            // Values representing Long types.
            val newValues_Long: Coll[Long] = Coll(
                BaseAmount,
                MinQuoteAmount,
                DexFeePerTokenNum,
                DexFeePerTokenDenom,
                BaseAmount,
                MaxMinerFee
            )

            // Values representing Coll[Byte] types.
            val newValues_Coll_Byte: Coll[Coll[Byte]] = Coll(
                PoolNFT,
                QuoteId
            )

            // Values representing SigmaProp types.
            val newValues_SigmaProp: Coll[SigmaProp] = Coll(
                UserPK
            )

            // Will insert the new values based on the following order: Long => Coll[Byte] => SigmaProp
            val newDexSwapSellContractSample_Int:       Coll[Byte]  =   substConstants(DexSwapSellContractSample, positions_Int, newValues_Int) 
            val newDexSwapSellContractSample_Long:      Coll[Byte]  =   substConstants(newDexSwapSellContractSample_Int, positions_Long, newValues_Long)
            val newDexSwapSellContractSample_Coll_Byte: Coll[Byte]  =   substConstants(newDexSwapSellContractSample_Long, positions_Coll_Byte, newValues_Coll_Byte)
            val newDexSwapSellContractSample_SigmaProp: Coll[Byte]  =   substConstants(newDexSwapSellContractSample_Coll_Byte, positions_SigmaProp, newValues_SigmaProp)
            val newDexSwapSellContractSample:           Coll[Byte]  =   newDexSwapSellContractSample_SigmaProp

            // ===== GuapSwap SwapSell Proxy Contract Conditions ===== //
            // Some useful calculations
            val totalPayout:    Long    =   INPUTS.fold(0L, {(acc: Long, input: Box) => acc + input.value})
            val protocolFee:    Long    =   (GuapSwapProtocolFeePercentageNum * totalPayout) / GuapSwapProtocolFeePercentageDenom
            val serviceFee:     Long    =   protocolFee + GuapSwapMinerFee
            val totalFees:      Long    =   serviceFee + TotalDexFee
            
            // Check that a valid Dex Swap Sell Box in an output.
            val validDexSwapBox = {
                val dexSwapBox: Box = OUTPUTS(0)
                (SELF.value >= totalFees) && (dexSwapBox.value >= totalPayout - TotalDexFee) && (dexSwapBox.propositionBytes == newDexSwapSellContractSample)
            }

            // Check that a valid GuapSwap Protocol Fee Box is an output.
            val validProtocolFeeBox = {
                val protocolFeeBox: Box = OUTPUTS(1)
                (protocolFeeBox.value >= protocolFee) && (protocolFeeBox.propositionBytes == GuapSwapProtocolFeeContract)
            }

            // Check that a valid Refund Box is an output if initiated by user.
            val validRefundBox = {
                val refundBox: Box = OUTPUTS(0)
                (refundBox.value >= totalPayout - GuapSwapMinerFee) && (refundBox.propositionBytes == UserPK.propBytes)
            }

            // For a valid swap to occur, the following conditions must be met.
            val validGuapSwap = {
                validDexSwapBox && validProtocolFeeBox && (OUTPUTS.size == 3) // swapbox, feebox, minerbox (entire box is spent, so no changebox is created)
            }

            // For a valid refund to occur, the following conditions must be met.
            val validRefund = {
                validRefundBox 
            }

            // One of these conditions must be met in order to validate the script and execute the transaction with the corresponding action.
            sigmaProp(validGuapSwap || validRefund)
        }
        """.stripMargin
        script
    }
    
}