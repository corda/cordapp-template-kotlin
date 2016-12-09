package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.PurchaseOrderState;
import com.example.model.PurchaseOrder;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.CryptoUtilities;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.NotaryFlow;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static kotlin.collections.CollectionsKt.single;

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
public class ExampleFlow {
    public static class Initiator extends FlowLogic<ExampleFlowResult> {

        private final PurchaseOrderState purchaseOrderState;
        private final Party otherParty;
        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                CONSTRUCTING_OFFER,
                SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION,
                VERIFYING,
                SIGNING,
                NOTARY,
                RECORDING,
                SENDING_FINAL_TRANSACTION
        );

        private static final ProgressTracker.Step CONSTRUCTING_OFFER = new ProgressTracker.Step(
                "Constructing proposed purchase order.");
        private static final ProgressTracker.Step SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION = new ProgressTracker.Step(
                "Sending purchase order to seller for review, and receiving partially signed transaction from seller in return.");
        private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing transaction with our private key.");
        private static final ProgressTracker.Step NOTARY = new ProgressTracker.Step(
                "Obtaining notary signature.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");
        private static final ProgressTracker.Step SENDING_FINAL_TRANSACTION = new ProgressTracker.Step(
                "Sending fully signed transaction to seller.");

        public Initiator(PurchaseOrderState purchaseOrderState, Party otherParty) {
            this.purchaseOrderState = purchaseOrderState;
            this.otherParty = otherParty;
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override public ExampleFlowResult call() {
            // Naively, wrapped the whole flow in a try ... catch block so we can
            // push the exceptions back through the web API.
            try {
                // Prep.
                // Obtain a reference to our key pair. Currently, the only key pair used is the one which is registered with
                // the NetWorkMapService. In a future milestone release we'll implement HD key generation such that new keys
                // can be generated for each transaction.
                final KeyPair myKeyPair = getServiceHub().getLegalIdentityKey();
                // Obtain a reference to the notary we want to use and its public key.
                final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
                final CompositeKey notaryPubKey = notary.getOwningKey();

                // Stage 1.
                progressTracker.setCurrentStep(CONSTRUCTING_OFFER);
                // Construct a state object which encapsulates the PurchaseOrder object.
                // We add the public keys for us and the counterparty as well as a reference to the contract code.
                final TransactionState offerMessage = new TransactionState<ContractState>(purchaseOrderState, notary);

                // Stage 2.
                progressTracker.setCurrentStep(SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION);
                // Send the state across the wire to the designated counterparty.
                // -----------------------
                // Flow jumps to Acceptor.
                // -----------------------
                // Receive the partially signed transaction off the wire from the other party.
                final SignedTransaction ptx = sendAndReceive(otherParty, offerMessage, SignedTransaction.class)
                        .unwrap(data -> data);

                // Stage 7.
                progressTracker.setCurrentStep(VERIFYING);
                // Check that the signature of the other party is valid.
                // Our signature and the Notary's signature are allowed to be omitted at this stage as this is only a
                // partially signed transaction.
                final WireTransaction wtx = ptx.verifySignatures(CryptoUtilities.getComposite(myKeyPair.getPublic()), notaryPubKey);
                // Run the contract's verify function.
                // We want to be sure that the PurchaseOrderState agreed upon is a valid instance of an PurchaseOrderContract, to do
                // this we need to run the contract's verify() function.
                wtx.toLedgerTransaction(getServiceHub()).verify();

                // Stage 8.
                progressTracker.setCurrentStep(SIGNING);
                // Sign the transaction with our key pair and add it to the transaction.
                // We now have 'validation consensus'. We still require uniqueness consensus.
                // Technically validation consensus for this type of agreement implicitly provides uniqueness consensus.
                final DigitalSignature.WithKey mySig = CryptoUtilities.signWithECDSA(myKeyPair, ptx.getId().getBytes());
                final SignedTransaction vtx = ptx.plus(mySig);

                // Stage 9.
                progressTracker.setCurrentStep(NOTARY);
                // Obtain the notary's signature.
                // We do this by firing-off a sub-flow. This illustrates the power of protocols as reusable workflows.
                final DigitalSignature.WithKey notarySignature = subFlow(new NotaryFlow.Client(vtx, NotaryFlow.Client.Companion.tracker()), false);
                // Add the notary signature to the transaction.
                final SignedTransaction ntx = vtx.plus(notarySignature);

                // Stage 10.
                progressTracker.setCurrentStep(RECORDING);
                // Record the transaction in our vault.
                getServiceHub().recordTransactions(Collections.singletonList(ntx));

                // Stage 11.
                progressTracker.setCurrentStep(SENDING_FINAL_TRANSACTION);
                // Send a copy of the transaction to our counter-party.
                send(otherParty, ntx);
                return new ExampleFlowResult.Success(String.format("Transaction id %s committed to ledger.", ntx.getId()));
            } catch(Exception ex) {
                // Just catch all exception types.
                return new ExampleFlowResult.Failure(ex.getMessage());
            }
        }
    }

    public static class Acceptor extends FlowLogic<ExampleFlowResult> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(
                WAIT_FOR_AND_RECEIVE_PROPOSAL,
                GENERATING_TRANSACTION,
                SIGNING,
                SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE,
                VERIFYING_TRANSACTION,
                RECORDING
        );

        private static final ProgressTracker.Step WAIT_FOR_AND_RECEIVE_PROPOSAL = new ProgressTracker.Step(
                "Receiving proposed purchase order from buyer.");
        private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step(
                "Generating transaction based on proposed purchase order.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing proposed transaction with our private key.");
        private static final ProgressTracker.Step SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE = new ProgressTracker.Step(
                "Sending partially signed transaction to buyer and wait for a response.");
        private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        @Suspendable
        @Override public ExampleFlowResult call() {
            try {
                // Prep.
                // Obtain a reference to our key pair.
                final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

                // Stage 3.
                progressTracker.setCurrentStep(WAIT_FOR_AND_RECEIVE_PROPOSAL);
                // All messages come off the wire as UntrustworthyData. You need to 'unwrap' it. This is an appropriate
                // place to perform some validation over what you have just received.
                final TransactionState<DealState> message = this.receive(otherParty, TransactionState.class)
                        .unwrap(data -> (TransactionState<DealState>) data );

                // Stage 4.
                progressTracker.setCurrentStep(GENERATING_TRANSACTION);
                // Generate an unsigned transaction. See PurchaseOrderState for further details.
                final TransactionBuilder utx = message.getData().generateAgreement(message.getNotary());
                // Add a timestamp as the contract code in PurchaseOrderContract mandates that ExampleStates are timestamped.
                final Instant currentTime = getServiceHub().getClock().instant();
                // As we are running in a distributed system, we allocate a 30 second time window for the transaction to
                // be timestamped by the Notary service.
                utx.setTime(currentTime, Duration.ofSeconds(30));

                // Stage 5.
                progressTracker.setCurrentStep(SIGNING);
                // Sign the transaction.
                final SignedTransaction stx = utx.signWith(keyPair).toSignedTransaction(false);

                // Stage 6.
                progressTracker.setCurrentStep(SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE);
                // Send the state back across the wire to the designated counterparty.
                // ------------------------
                // Flow jumps to Initiator.
                // ------------------------
                // Receive the signed transaction off the wire from the other party.
                final SignedTransaction ntx = this.sendAndReceive(otherParty, stx, SignedTransaction.class)
                        .unwrap(data -> data);

                // Stage 12.
                progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
                // Validate transaction.
                // No need to allow for any omitted signatures as everyone should have signed.
                ntx.verifySignatures();
                // Check it's valid.
                ntx.toLedgerTransaction(getServiceHub()).verify();

                // Record the transaction.
                progressTracker.setCurrentStep(RECORDING);
                getServiceHub().recordTransactions(Collections.singletonList(ntx));
                return new ExampleFlowResult.Success(String.format("Transaction id %s committed to ledger.", ntx.getId()));
            } catch (Exception ex) {
                return new ExampleFlowResult.Failure(ex.getMessage());
            }
        }
    }

    public static class ExampleFlowResult {
        public static class Success extends com.example.flow.ExampleFlow.ExampleFlowResult {
            private String message;

            private Success(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Success(%s)", message); }
        }

        public static class Failure extends com.example.flow.ExampleFlow.ExampleFlowResult {
            private String message;

            private Failure(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Failure(%s)", message); }
        }
    }
}
