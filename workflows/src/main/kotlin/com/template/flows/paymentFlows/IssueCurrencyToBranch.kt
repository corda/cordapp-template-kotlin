package com.template.flows.paymentFlows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party

/**
 * Flow to issue currency from the Main bank branch
 */

@StartableByService
@InitiatingFlow
@StartableByRPC
class IssueCurrencyToBranch(val currency: String, val amount: Long, val branch: Party) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        // Create an instance of the currency
        val token = FiatCurrency.Companion.getInstance(currency)

        // Create an instance of IssuedTokenType
        val issuedToken = token issuedBy ourIdentity

        // Create an instance of a Fungible Token
        val fungToken = FungibleToken(Amount(amount, issuedToken), branch)

        subFlow(IssueTokens(listOf(fungToken), listOf(branch)))

        return "Successfully issued $amount to Branch -> $branch"
    }

}