package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.contracts.states.EvolvableTokenType
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.workflow.flows.CreateEvolvableToken
import com.r3.corda.sdk.token.workflow.flows.IssueToken
import com.template.states.ExampleEvolvableTokenType
import net.corda.core.contracts.TransactionState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.util.*

@StartableByRPC
class ExampleFlowWithEvolvableToken(val evolvableTokenId: String, val amount: Long, val recipient: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val uuid = UUID.fromString(evolvableTokenId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<EvolvableTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()
        return subFlow(IssueToken.Initiator(token, recipient, notary, amount of token, anonymous = false))
    }
}

@StartableByRPC
class CreateExampleEvolvableToken(val data: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val evolvableTokenType = ExampleEvolvableTokenType(data, ourIdentity)
        val transactionState = TransactionState(evolvableTokenType, notary = notary)
        return subFlow(CreateEvolvableToken(transactionState))
    }
}