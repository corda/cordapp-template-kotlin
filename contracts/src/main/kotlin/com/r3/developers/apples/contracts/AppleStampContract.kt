package com.r3.developers.apples.contracts

import com.r3.developers.apples.states.AppleStamp
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class AppleStampContract : Contract {
    override fun verify(transaction: UtxoLedgerTransaction) {
        // Extract the command from the transaction
        // Verify the transaction according to the intention of the transaction
        when (val command = transaction.commands.first()) {
            is AppleCommands.Issue -> {
                val output = transaction.getOutputStates(AppleStamp::class.java).first()
                require(transaction.outputContractStates.size == 1) {
                    "This transaction should only have one AppleStamp state as output"
                }
                require(output.stampDesc.isNotBlank()) {
                    "The output AppleStamp state should have clear description of the type of redeemable goods"
                }
            }
            is AppleCommands.Redeem -> {
                // Transaction verification will happen in BasketOfApplesContract
            }
            else -> {
                // Unrecognised Command type
                throw IllegalArgumentException("Incorrect type of AppleStamp commands: ${command::class.java.name}")
            }
        }
    }

}