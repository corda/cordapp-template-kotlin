package com.template.accountsUtilities


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party


@StartableByRPC
@StartableByService
@InitiatingFlow
class ShareAccountTo(
        private val acctNameShared: String,
        private val shareTo: Party
) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {

        //Create a new account
        val AllmyAccounts = accountService.ourAccounts()
        val SharedAccount = AllmyAccounts.single { it.state.data.name == acctNameShared }.state.data.identifier.id
        accountService.shareAccountInfoWithParty(SharedAccount,shareTo)

        return "Shared " + acctNameShared + " with " + shareTo.name.organisation
    }
}



