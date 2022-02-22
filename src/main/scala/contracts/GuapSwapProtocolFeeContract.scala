package contracts

/**
  * Object describing the GuapSwap protocol-fee contract
  */
object GuapSwapProtocolFeeContract {
  def getScript: String = {
    val script: String = s"""
    {
      // ===== Contract Info ===== //
      // Description: Proxy contract which holds the GuapSwap protocol fees.
      // Author: Luca D’Angelo
      
      // ===== Contract Hard-Coded Variables ===== //
      // val JesperPK:          SigmaProp
      // val GeorgePK:          SigmaProp
      // val LucaPK:            SigmaProp
      // val THRESHOLD:         Long
      // val FeeSplitNum:       Long
      // val FeeSplitDenom:     Long
      // val GuapSwapMinerFee:  Long

      // ===== GuapSwap Protocol Fee Contract Conditions ===== //
      val totalFees:  Long = INPUTS.fold(0L, {(acc: Long, input: Box) => acc + input.value})
      val splitValue: Long = (FeeSplitNum * totalFees) / FeeSplitDenom

      // Check that a valid Jesper Fee Withdrawal Box is created.
      val validJesperFeeWithdrawalBox = {
        val jesperBox = OUTPUTS(0)
        (jesperBox.value == splitValue) && (jesperBox.propositionBytes == JesperPK.propBytes)
      }

      // Check that a valid George Fee Withdrawal Box is created.
      val validGeorgeFeeWithdrawalBox = {
        val georgeBox = OUTPUTS(1)
        (georgeBox.value == splitValue) && (georgeBox.propositionBytes == GeorgePK.propBytes)
      }

      // Check that a valid Luca Fee Withdrawal Box is created.
      val validLucaFeeWithdrawalBox = {
        val lucaBox = OUTPUTS(2)
        (lucaBox.value == splitValue) && (lucaBox.propositionBytes == LucaPK.propBytes)
      }

      // Check that a valid Group Protocol Fee Box is created.
      val validGroupProtocolFeeBox = {
        val feeBox = OUTPUTS(3)
        (feeBox.value >= totalFees - (3 * splitValue) - GuapSwapMinerFee) && (SELF.propositionBytes == feeBox.propositionBytes)
      }

      // Check that a valid group fee withdrawal is initiated
      val validGroupFeeWithdrawal = {
        validJesperFeeWithdrawalBox && validGeorgeFeeWithdrawalBox && validLucaFeeWithdrawalBox && validGroupProtocolFeeBox && (totalFees >= THRESHOLD) && (OUTPUTS.size == 5)
      }

      // One of these conditions must be met in order to validate the script and execute the transaction with the corresponding action.
      sigmaProp(validGroupFeeWithdrawal)

    }
    """.stripMargin
    script
  }

}