package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.IOUContract
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*
import java.util.stream.Collectors

// NOTE: For iouAmount, it may be better to use Amount type to avoid confusion.
//  When converted to Amount from Long, the value gets denominated by 100. e.g) 200 (Long) -> 2 (Amount).
//  If the amount type becomes Amount<Currency> then user should provide the currency type along with the value.
//  e.g) £200
@InitiatingFlow
@StartableByRPC
class IssueFlow(
    private val receiver: Party,
    private val iouAmount: Long
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("IssueFlow starts.")

        // NoteAssumption is that we are the lender.
        val lender = ourIdentity
        val borrower = receiver

        // Step 1. Get a reference to the notary service on our network and our key pair.
        // Note: ongoing work to support multiple notary identities is still in progress.
        logger.info("Get the reference of a notary")
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        // Issue a new state with lender, borrower and amount
        val output = IOUState(
            "Issuance of an IOU state",
            Amount(iouAmount, Currency.getInstance(Locale.UK)),
            Amount(0, Currency.getInstance(Locale.UK)),
            lender, borrower,
            linearId = UniqueIdentifier(null, UUID.randomUUID())
        )

        // Step 2. Create a new TransactionBuilder object.
        logger.info("Create a TransactionBuilder instance")
        val tnxBuilder = TransactionBuilder(notary)
            .addCommand(IOUContract.Commands.Issue(), listOf(lender.owningKey, borrower.owningKey))
            .addOutputState(output)

        // Step 3. Verify and sign it with our KeyPair.
        tnxBuilder.verify(serviceHub)
        val signedTnx = serviceHub.signInitialTransaction(tnxBuilder)

        // Step 4. Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> =
            output.participants.stream().map { el: AbstractParty? -> el as Party? }.collect(Collectors.toList())
        otherParties.remove(ourIdentity)
        val sessions = otherParties.stream().map { el: Party? -> initiateFlow(el!!) }.collect(Collectors.toList())
        val stx = subFlow(CollectSignaturesFlow(signedTnx, sessions))

        // Step 5. Assuming no exceptions, we can now finalise the transaction
        return subFlow<SignedTransaction>(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(IssueFlow::class)
class IssueFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                "This must be an IOU state.".using(stx.tx.outputs.single().data is IOUState)
            }
        }
        logger.info("Responder received a proposed transaction.")
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

