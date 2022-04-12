package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.IOUContract
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*
import java.util.stream.Collectors


@InitiatingFlow
@StartableByRPC
class TransferFlow(private val stateId: UniqueIdentifier, private val newLender: Party) :
    FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("TransferFlow starts.")

        // Step 1. Get a reference to the notary service on our network and our key pair.
        // Note: ongoing work to support multiple notary identities is still in progress.
        logger.info("Get the reference of a notary")
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        // Step 2. Read the existing state from the vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, listOf(stateId))
        val results = serviceHub.vaultService.queryBy<IOUState>(queryCriteria)
        val oriIOUStateAndRef = results.states.last()
        val oriIOUState = results.states.last().state

        // Step 3. Create a new output state (the lender becomes the bank)
        val output = IOUState(
            "New IOUState with the new lender",
            oriIOUState.data.amount,
            oriIOUState.data.paid,
            newLender, oriIOUState.data.borrower,
            linearId = stateId
        )

        // Step 3. Create a new TransactionBuilder object.
        logger.info("Create a TransactionBuilder instance")
        val tnxBuilder = TransactionBuilder(notary)
            .addCommand(
                IOUContract.Commands.Transfer(),
                listOf(newLender.owningKey, oriIOUState.data.borrower.owningKey)
            )
            .addInputState(oriIOUStateAndRef)
            .addOutputState(output)

        // Step 3. Verify and sign it with our KeyPair.
        tnxBuilder.verify(serviceHub)
        val signedTnx = serviceHub.signInitialTransaction(tnxBuilder)

        // Step 4. Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> = output.participants.map { el -> el as Party }.toMutableList()
        otherParties.remove(ourIdentity)
        val sessions = otherParties.stream().map { el: Party? -> initiateFlow(el!!) }.collect(Collectors.toList())
        val stx = subFlow(CollectSignaturesFlow(signedTnx, sessions))

        // Step 5. Assuming no exceptions, we can now finalise the transaction
        return subFlow<SignedTransaction>(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(TransferFlow::class)
class TransferFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // TODO: add additional checks
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

