package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.contracts.utilities.heldBy
import com.r3.corda.sdk.token.contracts.utilities.issuedBy
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.workflow.flows.shell.IssueTokens
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class ExampleFlowWithFixedToken(val currency: String, val amount: Long, val recipient: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val token = FiatCurrency.getInstance(currency)
        // Starts a new flow session.
        return subFlow(IssueTokens(amount of token issuedBy ourIdentity heldBy recipient))
    }
}
