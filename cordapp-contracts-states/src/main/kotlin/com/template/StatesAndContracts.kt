package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class TemplateContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.TemplateContract"
    }
    
    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Action : Commands
    }
}

// *********
// * State *
// *********
data class TemplateState(val data: String) : ContractState {
    override val participants: List<AbstractParty> = listOf()
}
