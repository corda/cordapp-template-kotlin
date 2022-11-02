package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class TransferContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        requireThat {}
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}