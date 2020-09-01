package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class ProposalResponseContract : Contract {

    companion object {
        @JvmStatic
        val ID = "com.template.contracts.ProposalResponseContract"
    }

    interface ProposalResponseCommands : CommandData {
        class CreateResponse : TypeOnlyCommandData(), ProposalResponseCommands
    }

    override fun verify(tx: LedgerTransaction) = Unit
}