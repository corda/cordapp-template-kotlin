package com.gitcoins.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class GitTokenContract : Contract {

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
    }

    override fun verify(tx: LedgerTransaction) {
    }
}
