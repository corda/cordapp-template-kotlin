package com.template.flows.utilities

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.utilities.getOrThrow

/**
 * Flow for creating a new account.
 */

@StartableByRPC
@StartableByService
@InitiatingFlow
class AddAccount(val name: String) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val newAccount = accountService.createAccount(name = name).toCompletableFuture().getOrThrow()
        val acct = newAccount.state.data
        return "Account successfully created: Name: ${acct.name} UUID: ${acct.identifier}"
    }
}