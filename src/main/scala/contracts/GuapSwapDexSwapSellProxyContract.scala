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
            // val GuapSwapMinerFee:                    Long

            // ===== GuapSwap SwapSell Proxy Contract Conditions ===== //
            val totalPayout: Long = INPUTS.fold(0L, {(acc: Long, input: Box) => acc + input.value})
            val protocolFee: Long = (GuapSwapProtocolFeePercentageNum * totalPayout) / GuapSwapProtocolFeePercentageDenom
            val serviceFee:  Long = protocolFee + GuapSwapMinerFee

            // ===== Contract Context Variables: Dex Settings Variables ===== //
            // First column of indicies: Index of "getVar[T](tag: Int): Option[T]" corresponding to the appropriate ContextVariable.
            // Second column of indicies: Index of "positions: Coll[Int]" parameter of "substContants[T](scriptBytes: Coll[Bytes], positions: Coll[Int], newValues: Coll[T]): Coll[Byte]" corresponding to the associated variable that is to be inserted into the Dex swap contract sample.
            //    => FeeNum:                            Int         => 14, 18
            //    => QuoteId:                           Coll[Byte]  => 9
            //    => MinQuoteAmount:                    Long        => 10
            //    => BaseAmount:                        Long        => 2, 17
            //    => DexFeePerTokenNum:                 Long        => 11
            //    => DexFeePerTokenDenom:               Long        => 12
            //    => MaxMinerFee:                       Long        => 22
            //    => PoolNFT:                           Coll[Byte]  => 8
            // 1  => TotalDexFee:                       Long
            // 2  => DexSwapSellContractSapleWithoutPK: Coll[Byte]

            // Check if the context variables exists in the transaction context
            if (getVar[Long](0).get == 42069.toLong) {
                
                // Assign the context variables
                val TotalDexFee: Long = getVar[Long](1).get
                val DexSwapSellContractSampleWithoutPK: Coll[Byte] = getVar[Coll[Byte]](2).get
                
                // Replacing the Dex variable values in the SwapSell template with their corresponding value from the transaction context.
                val positions_SigmaProp: Coll[Int] = Coll(0)

                // Values representing SigmaProp types.
                val newValues_SigmaProp: Coll[SigmaProp] = Coll(
                    UserPK
                )

                // Insert the new constants into the ErgoTree
                val newDexSwapSellContractSample_SigmaProp: Coll[Byte] = substConstants(DexSwapSellContractSampleWithoutPK, positions_SigmaProp, newValues_SigmaProp)
                val newDexSwapSellContractSample:           Coll[Byte] = newDexSwapSellContractSample_SigmaProp

                val totalFees: Long = serviceFee + TotalDexFee
                
                // Check that a valid Dex Swap Sell Box in an output.
                val validDexSwapBox = {
                    val dexSwapBox: Box = OUTPUTS(0)
                    allOf(Coll(
                        (totalPayout >= totalFees), 
                        (dexSwapBox.value >= totalPayout - TotalDexFee), 
                        (dexSwapBox.propositionBytes == newDexSwapSellContractSample)
                    ))
                }

                // Check that a valid GuapSwap Protocol Fee Box is an output.
                val validProtocolFeeBox = {
                    val protocolFeeBox: Box = OUTPUTS(1)
                    allOf(Coll(
                        (protocolFeeBox.value >= protocolFee), 
                        (protocolFeeBox.propositionBytes == GuapSwapProtocolFeeContract)
                    ))
                }

                // For a valid swap to occur, the following conditions must be met.
                val validGuapSwap = {
                    allOf(Coll(
                        validDexSwapBox, 
                        validProtocolFeeBox, 
                        (OUTPUTS.size == 3)
                    )) // swapbox, feebox, minerbox (entire box is spent, so no changebox is created)
                }

                sigmaProp(validGuapSwap)

            } else if (getVar[Long](0).get == 666.toLong) {
            
                // Check that a valid Refund Box is an output if initiated by the user.
                val validRefundBox = {
                    val refundBox: Box = OUTPUTS(0)
                    allOf(Coll(
                        (refundBox.value >= totalPayout - GuapSwapMinerFee), 
                        (refundBox.propositionBytes == UserPK.propBytes)
                    ))
                }

                // For a valid refund to occur, the following conditions must be met.
                val validRefund = {
                    validRefundBox 
                }

                sigmaProp(validRefund)

            } else {
                sigmaProp(false)
            }


        }
        """.stripMargin
        script
    }
    
}