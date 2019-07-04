package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class IssueInitiator(val owner: Party, val description: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // We get a reference to our own identity.
        val creator = ourIdentity

        // We create our new TokenState.
        val tokenState = TokenState(creator, owner, description, 0)

        // We build our transaction.
        val transactionBuilder = TransactionBuilder(notary)
        transactionBuilder.addOutputState(tokenState, TokenContract.ID)
        transactionBuilder.addCommand(TokenContract.Commands.Issue(), creator.owningKey)

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        // We sign the transaction with our private key, making it immutable.
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val flowSessionList: MutableList<FlowSession> = mutableListOf()
        if(owner != creator) {
            flowSessionList.add(initiateFlow(owner))
        }

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(FinalityFlow(signedTransaction, flowSessionList))
    }
}

@InitiatedBy(IssueInitiator::class)
class IssueResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterpartySession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        // Responder flow logic goes here.
    }
}
