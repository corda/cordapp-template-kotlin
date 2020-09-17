package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokensHandler
import net.corda.core.contracts.LinearState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class IssueTokensFlow(private val issueTo: Party, private val token: Tokens) : FlowLogic<Tokens>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Tokens {
        val tokens = listOf(token.amount.toLong() of TokenType(token.currency, 2) issuedBy ourIdentity heldBy issueTo)
        subFlow(IssueTokens(tokens))
        return token
    }
}

@InitiatedBy(IssueTokensFlow::class)
class IssueTokensFlowResponder(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(IssueTokensHandler(counterpartySession))
    }
}


@CordaSerializable
data class Tokens(val amount: Int, val currency: String)