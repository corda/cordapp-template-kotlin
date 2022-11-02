package com.template.flows.utilities

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party


/**
 * Flow for sharing an account with other nodes/accounts
 */
@StartableByRPC
@StartableByService
@InitiatingFlow
class ShareAccount(val acctNameToShare: String, val party: Party) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {
        val allAccounts = accountService.ourAccounts()
        // Query account by name
        val accountId = allAccounts.single { it.state.data.name == acctNameToShare}.state.data.identifier.id

        accountService.shareAccountInfoWithParty(accountId, party)

        return "Successfully shared $acctNameToShare with $party"
    }
}