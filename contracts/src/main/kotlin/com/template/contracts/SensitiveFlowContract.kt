package com.template.contracts

import com.template.states.SensitiveState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SensitiveFlowContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        // NOTE: this value must match the contract package path
        const val ID = "com.template.contracts.SensitiveFlowContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        val output = tx.outputsOfType<SensitiveState>().first()
        when (command.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed when creating.".using(tx.inputStates.isEmpty())
                "Message can't be empty".using(output.msg.isNotEmpty())
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
    }
}