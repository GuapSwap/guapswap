package contracts

import special.sigma.Box

/**
  * Object describing the GuapSwap ErgoDex swap-sell proxy contract
  */
object GuapSwapErgoDexSwapSellProxyContract {
    def getScript: String = {
        val script: String = s"""
        {
            // ====== Contract Info ====== //
            // Description: Proxy contract which holds the miner's payout from the mining pool, and will perform a swap-sell with ErgoDex.
            // Author: Luca D’Angelo

            // ====== Contract Hard-Coded Constants ====== //
            // val PK:                                  SigmaProp
            // val GuapSwapProtocolFeePercentageNum:    Long  
            // val GuapSwapProtocolFeePercentageDenom:  Long
            // val GuapSwapProtocolFeeContract:         Coll[Byte]

            // ====== Contract Context Variables ====== //
            val NewDexSwapSellContractSample:   Coll[Byte]  = getVar[Coll[Byte]](0).get 
            val GuapSwapMinerFee:               Long        = getVar[Long](1).get                      
            val TotalDexFee:                    Long        = getVar[Long](2).get
            val TotalPayout:                    Long        = getVar[Long](3).get

            // ====== GuapSwap ErgoDex SwapSell Proxy Contract Conditions ====== //
            // Some useful calculations
            // TODO: Use INPUTS and calculate TotalPayout within contract, having trouble with map and fold functions
            val protocolFee:    Long = ((GuapSwapProtocolFeePercentageNum * TotalPayout) / GuapSwapProtocolFeePercentageDenom)
            val serviceFee:     Long = protocolFee + GuapSwapMinerFee
            val totalFees:      Long = serviceFee + TotalDexFee
            val baseAmount:     Long = TotalPayout - totalFees
            
            // Check that a valid Dex Swap Sell Box in an output.
            val validDexSwapBox = {
                val userSwapBox: Box = OUTPUTS(0)
                SELF.value >= totalFees &&
                userSwapBox.value >= TotalPayout - serviceFee && 
                userSwapBox.propositionBytes == NewDexSwapSellContractSample
            }

            // Check that a valid GuapSwap Protocol Fee Box is an output.
            val validGuapSwapProtocolFeeBox = {
                val protocolFeeBox: Box = OUTPUTS(1)
                protocolFeeBox.value >= protocolFee &&
                protocolFeeBox.propositionBytes == GuapSwapProtocolFeeContract
            }

            // Check that a valid Refund Box is an output if initiated by user.
            val validRefundBox = {
                val refundBox: Box = OUTPUTS(0)
                refundBox.value >= TotalPayout - serviceFee &&
                refundBox.propositionBytes == PK.propBytes
            }

            // For a valid swap to occur, the following conditions must be met.
            val validGuapSwap = {
                validDexSwapBox &&
                validGuapSwapProtocolFeeBox &&
                OUTPUTS.size == 3
            }

            // For a valid refund to occur, the following conditions must be met.
            val validRefund = {
                validRefundBox &&
                validGuapSwapProtocolFeeBox &&
                OUTPUTS.size == 3
            }

            // One of these three conditions must be met in order to validate the script and execute the transaction with the corresponding action.
            sigmaProp(validGuapSwap || validRefund || PK)
        }
        """.stripMargin
        script
    }
    
}