package com.template.accountsUtilities

import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.getOrThrow


@StartableByRPC
@StartableByService
@InitiatingFlow
class CreateNewAccount(private val acctName:String) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        //Create a new account
        val newAccount = accountService.createAccount(name = acctName).toCompletableFuture().getOrThrow()
        val acct = newAccount.state.data
        return ""+acct.name + " team's account was created. UUID is : " + acct.identifier
    }
}



