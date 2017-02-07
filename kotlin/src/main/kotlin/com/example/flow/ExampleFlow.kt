package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.PurchaseOrderState
import com.example.flow.ExampleFlow.Acceptor
import com.example.flow.ExampleFlow.Initiator
import net.corda.core.contracts.DealState
import net.corda.core.crypto.Party
import net.corda.core.crypto.composite
import net.corda.core.crypto.signWithECDSA
import net.corda.core.flows.FlowLogic
import net.corda.core.seconds
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.flows.FinalityFlow

/**
 * This is the "Hello World" of flows!
 *
 * It is a generic flow which facilitates the workflow required for two parties; an [Initiator] and an [Acceptor],
 * to come to an agreement about some arbitrary data (in this case, a [PurchaseOrder]) encapsulated within a [DealState].
 *
 * As this is just an example there's no way to handle any counter-proposals. The [Acceptor] always accepts the
 * proposed state assuming it satisfies the referenced [Contract]'s issuance constraints.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * NB. All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 *
 * The flows below have been heavily commented to aid your understanding. It may also be worth reading the CorDapp
 * tutorial documentation on the Corda docsite (https://docs.corda.net) which includes a sequence diagram which clearly
 * explains each stage of the flow.
 */
object ExampleFlow {
    class Initiator(val po: PurchaseOrderState,
                    val otherParty: Party): FlowLogic<ExampleFlowResult>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new purchase order.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object SENDING_TRANSACTION : ProgressTracker.Step("Sending proposed transaction to seller for review.")

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    SENDING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): ExampleFlowResult {
            // Naively, wrapped the whole flow in a try ... catch block so we can
            // push the exceptions back through the web API.
            try {
                // Prep.
                // Obtain a reference to our key pair. Currently, the only key pair used is the one which is registered with
                // the NetWorkMapService. In a future milestone release we'll implement HD key generation such that new keys
                // can be generated for each transaction.
                val keyPair = serviceHub.legalIdentityKey
                // Obtain a reference to the notary we want to use.
                val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity

                // Stage 1.
                progressTracker.currentStep = GENERATING_TRANSACTION
                // Generate an unsigned transaction.
                val unsignedTx = po.generateAgreement(notary)
                // Add a timestamp (the contract code in PurchaseOrderContract mandates that PurchaseOrderStates are timestamped).
                val currentTime = serviceHub.clock.instant()
                // As we are running in a distributed system, we allocate a 30-second time window for the transaction to
                // be timestamped by the Notary service. This is because there is no true time in a distributed system, and
                // because the process of agreeing and notarising the transaction is not instantaneous.
                unsignedTx.setTime(currentTime, 30.seconds)

                // Stage 2.
                progressTracker.currentStep = SIGNING_TRANSACTION
                val partSignedTx = unsignedTx.signWith(keyPair).toSignedTransaction(checkSufficientSignatures = false)

                // Stage 3.
                progressTracker.currentStep = SENDING_TRANSACTION
                // Send the state across the wire to the designated counterparty.
                // -----------------------
                // Flow jumps to Acceptor.
                // -----------------------
                send(otherParty, partSignedTx)

                return ExampleFlowResult.Success("Transaction id ${partSignedTx.id} committed to ledger.")

            } catch(ex: Exception) {
                // Catch all exception types.
                return ExampleFlowResult.Failure(ex.message)
            }
        }
    }

    class Acceptor(val otherParty: Party): FlowLogic<ExampleFlowResult>() {
        companion object {
            object RECEIVING_TRANSACTION : ProgressTracker.Step("Receiving proposed transaction from buyer.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying signatures and contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing proposed transaction with our private key.")
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")

            fun tracker() = ProgressTracker(
                    RECEIVING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): ExampleFlowResult {
            try {
                // Prep.
                // Obtain a reference to our key pair.
                val keyPair = serviceHub.legalIdentityKey
                // Obtain a reference to the notary we want to use and its public key.
                val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
                val notaryPubKey = notary.owningKey

                // Stage 4.
                progressTracker.currentStep = RECEIVING_TRANSACTION
                // All messages come off the wire as UntrustworthyData. You need to 'unwrap' them. This is where you
                // validate what you have just received.
                val partSignedTx = receive<SignedTransaction>(otherParty).unwrap { partSignedTx ->
                    // Stage 5.
                    progressTracker.currentStep = VERIFYING_TRANSACTION
                    // Check that the signature of the other party is valid.
                    // Our signature and the notary's signature are allowed to be omitted at this stage as this is only a
                    // partially signed transaction.
                    val wireTx = partSignedTx.verifySignatures(keyPair.public.composite, notaryPubKey)
                    // Run the contract's verify function.
                    // We want to be sure that the PurchaseOrderState agreed upon is a valid instance of an
                    // PurchaseOrderContract. To do this we need to run the contract's verify() function.
                    wireTx.toLedgerTransaction(serviceHub).verify()
                    // We've verified the signed transaction and return it.
                    partSignedTx
                }

            // Stage 6.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction with our key pair and add it to the transaction.
            // We now have 'validation consensus'. We still require uniqueness consensus.
            // Technically validation consensus for this type of agreement implicitly provides uniqueness consensus.
            val mySig = keyPair.signWithECDSA(partSignedTx.id.bytes)
            // Add our signature to the transaction.
            val signedTx = partSignedTx + mySig

            // Stage 7.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // FinalityFlow() notarises the transaction and records it in each party's vault.
            subFlow(FinalityFlow(signedTx, setOf(serviceHub.myInfo.legalIdentity, otherParty)))

            return ExampleFlowResult.Success("Transaction id ${signedTx.id} committed to ledger.")

            } catch (ex: Exception) {
                return ExampleFlowResult.Failure(ex.message)
            }
        }
    }
}

/**
 * Helper class for returning a result from the flows.
 */
sealed class ExampleFlowResult {
    class Success(val message: String?): ExampleFlowResult() {
        override fun toString(): String = "Success($message)"
    }

    class Failure(val message: String?): ExampleFlowResult() {
        override fun toString(): String = "Failure($message)"
    }
}