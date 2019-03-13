package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TransferInitiator(val stateRef: StateAndRef<TokenState>, val newOwner: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // We get a reference to our own identity.
        val owner = ourIdentity

        // We create our TokenState with new owner.
        val currentState = stateRef.state.data
        val count = currentState.ownerCount
        val tokenState = currentState.copy(owner = newOwner, ownerCount = count + 1)

        // We build our transaction.
        val transactionBuilder = TransactionBuilder(notary)
        transactionBuilder.addInputState(stateRef)
        transactionBuilder.addOutputState(tokenState, TokenContract.ID)
        transactionBuilder.addCommand(TokenContract.Commands.Transfer(), owner.owningKey)

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        // We sign the transaction with our private key, making it immutable.
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(FinalityFlow(signedTransaction, emptyList()))
    }
}

@InitiatedBy(TransferInitiator::class)
class TransferResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}
