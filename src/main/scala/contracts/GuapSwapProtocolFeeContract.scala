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
      val THRESHOLD:         Long = 1000000000.toLong // 1 ERG in nanoERG
      val FeeSplitNum:       Long = 1.toLong          // Split total value fees     
      val FeeSplitDenom:     Long = 3.toLong          // into equal thirds (1/3)
      val GuapSwapMinerFee:  Long = 2000000.toLong    // 0.002 ERG in nanoERG

      // ===== GuapSwap Protocol Fee Contract Conditions ===== //
      val totalFees:  Long = INPUTS.fold(0L, {(acc: Long, input: Box) => acc + input.value})
      val splitValue: Long = (FeeSplitNum * (totalFees - GuapSwapMinerFee)) / FeeSplitDenom

      // Check that a valid Jesper Fee Withdrawal Box is created.
      val validJesperFeeWithdrawalBox = {
        val jesperBox = OUTPUTS(0)
        allOf(Coll(
        (jesperBox.value == splitValue), 
        (jesperBox.propositionBytes == JesperPK.propBytes)
        ))
      }

      // Check that a valid George Fee Withdrawal Box is created.
      val validGeorgeFeeWithdrawalBox = {
        val georgeBox = OUTPUTS(1)
        allOf(Coll(
        (georgeBox.value == splitValue), 
        (georgeBox.propositionBytes == GeorgePK.propBytes)
        ))
      }

      // Check that a valid Luca Fee Withdrawal Box is created.
      val validLucaFeeWithdrawalBox = {
        val lucaBox = OUTPUTS(2)
        allOf(Coll(
        (lucaBox.value == splitValue), 
        (lucaBox.propositionBytes == LucaPK.propBytes)
        ))
      }

      // Check that a valid group fee withdrawal is initiated
      val validGroupFeeWithdrawal = {
        allOf(Coll(
        validJesperFeeWithdrawalBox, 
        validGeorgeFeeWithdrawalBox, 
        validLucaFeeWithdrawalBox, 
        (totalFees >= THRESHOLD), 
        ))
      }

      // One of these conditions must be met in order to validate the script and execute the transaction with the corresponding action.
      sigmaProp(validGroupFeeWithdrawal)

    }
    """.stripMargin
    script
  }

}