package com.template.contracts

import com.template.states.TransferState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class TransferContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val outputState = tx.getOutput(0) as TransferState
        outputState.apply {
            require(outputState.amount.quantity > 0) {"Amount cannot be zero"}
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}