package com.template.accountsUtilities

import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC


@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewMyAccounts() : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {
        //Create a new account
        val aAccountsQuery = accountService.ourAccounts().map { it.state.data.name }
        return aAccountsQuery
    }
}



