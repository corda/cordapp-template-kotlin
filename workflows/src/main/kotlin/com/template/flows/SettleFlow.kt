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
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*
import java.util.stream.Collectors


@InitiatingFlow
@StartableByRPC
class SettleFlow(
    private val lender: Party,
    private val stateId: UniqueIdentifier,
    private val toPay: Amount<Currency>
) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("SettleFlow starts.")

        // NoteAssumption is that we are the lender.
        val borrower = ourIdentity

        // Step 1: Get a reference to the notary service on our network and our key pair.
        // Note: ongoing work to support multiple notary identities is still in progress.
        logger.info("Get the reference of a notary")
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        // Step 2: Read the existing (unconsumed) state from the vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
            null,
            listOf(stateId),
            Vault.StateStatus.UNCONSUMED,
            null
        )
        val results = serviceHub.vaultService.queryBy<IOUState>(queryCriteria)
        val iouStateAndRef = results.states.last()
        val iouState = results.states.last().state

        // Step 3: Issue a new state after the payment
        val paid = iouState.data.paid + toPay
        val newIouState = IOUState(
            "New IOU state after payment",
            amount = iouState.data.amount, paid = paid,
            lender = lender, borrower = borrower,
            linearId = stateId
        )

        // Step 4. Create a new TransactionBuilder object.
        logger.info("Create a TransactionBuilder instance")
        val tnxBuilder = TransactionBuilder(notary)
            .addCommand(IOUContract.Commands.Settle(), listOf(lender.owningKey, borrower.owningKey))
            .addInputState(iouStateAndRef)
            .addOutputState(newIouState)

        // Step 5. Verify and sign it with our KeyPair.
        tnxBuilder.verify(serviceHub)
        val signedTnx = serviceHub.signInitialTransaction(tnxBuilder)

        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> = newIouState.participants.map { el -> el as Party }.toMutableList()
        otherParties.remove(ourIdentity)
        val sessions = otherParties.stream().map { el: Party? -> initiateFlow(el!!) }.collect(Collectors.toList())
        val stx = subFlow(CollectSignaturesFlow(signedTnx, sessions))

        // Step 7. Assuming no exceptions, we can now finalise the transaction
        return subFlow<SignedTransaction>(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(SettleFlow::class)
class SettleFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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

