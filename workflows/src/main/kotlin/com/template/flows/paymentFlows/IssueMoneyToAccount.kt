package com.template.flows.paymentFlows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler
import net.corda.core.contracts.Amount
import net.corda.core.identity.AnonymousParty
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.flows.utilities.GenerateKey
import net.corda.core.flows.*
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import net.corda.core.transactions.SignedTransaction

/**
 * Flow for issuing tokens to an account by the host Node
 */

@InitiatingFlow
@StartableByRPC
@StartableByService
class IssueMoneyToAccount(val acctName: String, val amount: Long, val currency: String) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        val token = getInstance(currency)

        val allAccounts = accountService.ourAccounts()
        // Get Account Info
        val account = allAccounts.single { it.state.data.name == acctName}.state.data
        val key = serviceHub.createKeyForAccount(account).owningKey

        val issuedTokenType =  IssuedTokenType(ourIdentity, token)

        // Create an instance of FungibleToken for the fiat currency to be issued
        val fungibleToken = FungibleToken(Amount(amount, issuedTokenType), AnonymousParty(key), null)

        val stx = subFlow(IssueTokens(listOf(fungibleToken)))

        return "Done $stx"
    }

    @InitiatedBy(IssueMoneyToAccount::class)
    class MoneyIssueFlowResponse (private val flowSession: FlowSession): FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            subFlow(IssueTokensFlowHandler(flowSession))
        }
    }
}