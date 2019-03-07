package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.workflow.flows.IssueToken
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class ExampleFlow(val currency: String, val amount: Long, val recipient: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val token = FiatCurrency.getInstance(currency)
        return subFlow(IssueToken.Initiator(token, recipient, notary, amount of token, anonymous = false))
    }
}
