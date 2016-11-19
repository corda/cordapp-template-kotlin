package com.example.protocol

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.DealState
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Party
import net.corda.core.crypto.composite
import net.corda.core.crypto.signWithECDSA
import net.corda.core.protocols.ProtocolLogic
import net.corda.core.seconds
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.protocols.NotaryProtocol

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
 * practice we would recommend splitting up the various stages of the protocol into sub-routines.
 *
 * NB. All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 *
 * The flows below have been heavily commented to aid your understanding. It may also be worth reading the CorDapp
 * tutorial documentation on the Corda docsite (https://docs.corda.net) which includes a sequence diagram which clearly
 * explains each stage of the flow.
 */
object ExampleFlow {
    class Initiator(val po: DealState,
                    val otherParty: Party,
                    override val progressTracker: ProgressTracker = Initiator.tracker()): ProtocolLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the protocol and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object CONSTRUCTING_OFFER : ProgressTracker.Step("Constructing proposed purchase order.")
            object SENDING_OFFER : ProgressTracker.Step("Sending purchase order to seller for review.")
            object RECEIVED_PARTIAL_TRANSACTION : ProgressTracker.Step("Received partially signed transaction from seller.")
            object VERIFYING : ProgressTracker.Step("Verifying signatures and contract constraints.")
            object SIGNING : ProgressTracker.Step("Signing transaction with our private key.")
            object NOTARY : ProgressTracker.Step("Obtaining notary signature.")
            object RECORDING : ProgressTracker.Step("Recording transaction in vault.")
            object SENDING_FINAL_TRANSACTION : ProgressTracker.Step("Sending fully signed transaction to seller.")

            fun tracker() = ProgressTracker(
                    CONSTRUCTING_OFFER,
                    SENDING_OFFER,
                    RECEIVED_PARTIAL_TRANSACTION,
                    VERIFYING,
                    SIGNING,
                    NOTARY,
                    RECORDING,
                    SENDING_FINAL_TRANSACTION
            )
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Prep.
            // Obtain a reference to our key pair. Currently, the only key pair used is the one which is registered with
            // the NetWorkMapService. In a future milestone release we'll implement HD key generation such that new keys
            // can be generated for each transaction.
            val myKeyPair = serviceHub.legalIdentityKey
            // Obtain a reference to the notary we want to use and its public key.
            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
            val notaryPubKey = notary.owningKey
            // Stage 1.
            progressTracker.currentStep = CONSTRUCTING_OFFER
            // Construct a state object which encapsulates the PurchaseOrder object.
            // We add the public keys for us and the counterparty as well as a reference to the contract code.
            val offerMessage = TransactionState(po, notary)
            // Stage 2.
            progressTracker.currentStep = SENDING_OFFER
            // Send the state across the wire to the designated counterparty.
            // -----------------------
            // Flow jumps to Acceptor.
            // -----------------------
            progressTracker.currentStep = RECEIVED_PARTIAL_TRANSACTION
            val ptx = sendAndReceive<SignedTransaction>(otherParty, offerMessage).unwrap {
                // Stage 7.
                // Receive the partially signed transaction off the wire from the other party.
                // Check that the signature of the other party is valid.
                // Our signature and the Notary's signature are allowed to be omitted at this stage as this is only a
                // partially signed transaction.
                progressTracker.currentStep = VERIFYING
                val wtx: WireTransaction = it.verifySignatures(myKeyPair.public.composite, notaryPubKey)
                // Run the contract's verify function.
                // We want to be sure that the PurchaseOrderState agreed upon is a valid instance of an PurchaseOrderContract, to do
                // this we need to run the contract's verify() function.
                wtx.toLedgerTransaction(serviceHub).verify()
                it
            }
            // Stage 8.
            progressTracker.currentStep = SIGNING
            // Sign the transaction with our key pair and add it to the transaction.
            // We now have 'validation consensus'. We still require uniqueness consensus.
            // Technically validation consensus for this type of agreement implicitly provides uniqueness consensus.
            val mySig = myKeyPair.signWithECDSA(ptx.id.bytes)
            // '+' in this case is just an overloaded operator defined in 'signedTransaction.kt'.
            val vtx = ptx + mySig
            // Stage 9.
            progressTracker.currentStep = NOTARY
            // Obtain the notary's signature.
            // We do this by firing-off a sub-flow. This illustrates the power of protocols as reusable workflows.
            val notarySignature = subProtocol(NotaryProtocol.Client(vtx))
            // Add the notary signature to the transaction.
            val ntx = vtx + notarySignature
            // Stage 10.
            progressTracker.currentStep = RECORDING
            // Record the transaction in our vault.
            serviceHub.recordTransactions(listOf(ntx))
            // Stage 11.
            progressTracker.currentStep = SENDING_FINAL_TRANSACTION
            // Send a copy of the transaction to our counter-party.
            send(otherParty, ntx)
            return ntx
        }
    }

    class Acceptor(val otherParty: Party,
                   override val progressTracker: ProgressTracker = Acceptor.tracker()): ProtocolLogic<SignedTransaction>() {
        companion object {
            object WAITING_FOR_PROPOSAL : ProgressTracker.Step("Receiving proposed purchase order from buyer.")
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on proposed purchase order.")
            object SIGNING : ProgressTracker.Step("Signing proposed transaction with our private key.")
            object SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE : ProgressTracker.Step("Sending partially signed transaction to buyer and wait for a response.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying signatures and contract constraints.")
            object RECORDING : ProgressTracker.Step("Recording transaction in vault.")

            fun tracker() = ProgressTracker(
                    WAITING_FOR_PROPOSAL,
                    GENERATING_TRANSACTION,
                    SIGNING,
                    SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE,
                    VERIFYING_TRANSACTION,
                    RECORDING
            )
        }

        @Suspendable
        override fun call(): SignedTransaction {
            // Prep.
            // Obtain a reference to our key pair.
            val keyPair = serviceHub.legalIdentityKey
            // Stage 3.
            progressTracker.currentStep = WAITING_FOR_PROPOSAL
            // All messages come off the wire as UntrustworthyData. You need to 'unwrap' it. This is an appropriate
            // place to perform some validation over what you have just received.
            val message = receive<TransactionState<DealState>>(otherParty).unwrap { it }
            // Stage 4.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction. See PurchaseOrderState for further details.
            val utx = message.data.generateAgreement(message.notary)
            // Add a timestamp as the contract code in PurchaseOrderContract mandates that ExampleStates are timestamped.
            val currentTime = serviceHub.clock.instant()
            // As we are running in a distributed system, we allocate a 30 second time window for the transaction to
            // be timestamped by the Notary service.
            utx.setTime(currentTime, 30.seconds)
            // Stage 5.
            progressTracker.currentStep = SIGNING
            val stx = utx.signWith(keyPair).toSignedTransaction(checkSufficientSignatures = false)
            // Stage 6.
            // ------------------------
            // Flow jumps to Initiator.
            // ------------------------
            progressTracker.currentStep = SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE
            // Stage 12.
            // Receive the notarised transaction off the wire.
            val ntx = sendAndReceive<SignedTransaction>(otherParty, stx).unwrap {
                progressTracker.currentStep = VERIFYING_TRANSACTION
                // Validate transaction.
                // No need to allow for any omited signatures as everyone should have signed.
                it.verifySignatures()
                // Check it's valid.
                it.toLedgerTransaction(serviceHub).verify()
                it
            }
            // Record the transaction.
            progressTracker.currentStep = RECORDING
            serviceHub.recordTransactions(listOf(ntx))
            return ntx
        }
    }
}