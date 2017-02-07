package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.PurchaseOrderState;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.CryptoUtilities;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.FinalityFlow;

import java.io.FileNotFoundException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

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
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                SENDING_TRANSACTION
        );

        private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step(
                "Generating transaction based on new purchase order.");
        private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step(
                "Signing transaction with our private key.");
        private static final ProgressTracker.Step SENDING_TRANSACTION = new ProgressTracker.Step(
                "Sending proposed transaction to seller for review.");

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
                final KeyPair keyPair = getServiceHub().getLegalIdentityKey();
                // Obtain a reference to the notary we want to use.
                final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();

                // Stage 1.
                progressTracker.setCurrentStep(GENERATING_TRANSACTION);
                // Generate an unsigned transaction.
                final TransactionBuilder utx = purchaseOrderState.generateAgreement(notary);
                // Add a timestamp (the contract code in PurchaseOrderContract mandates that PurchaseOrderStates are timestamped).
                final Instant currentTime = getServiceHub().getClock().instant();
                // As we are running in a distributed system, we allocate a 30-second time window for the transaction to
                // be timestamped by the Notary service. This is because there is no true time in a distributed system, and
                // because the process of agreeing and notarising the transaction is not instantaneous.
                utx.setTime(currentTime, Duration.ofSeconds(30));

                // Stage 2.
                progressTracker.setCurrentStep(SIGNING_TRANSACTION);
                final SignedTransaction partSignedTx = utx.signWith(keyPair).toSignedTransaction(false);

                // Stage 3.
                progressTracker.setCurrentStep(SENDING_TRANSACTION);
                // Send the state across the wire to the designated counterparty.
                // -----------------------
                // Flow jumps to Acceptor.
                // -----------------------
                this.send(otherParty, partSignedTx);

                return new ExampleFlowResult.Success(String.format("Transaction id %s committed to ledger.", partSignedTx.getId()));

            } catch(Exception ex) {
                // Just catch all exception types.
                return new ExampleFlowResult.Failure(ex.getMessage());
            }
        }
    }

    public static class Acceptor extends FlowLogic<ExampleFlowResult> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(
                RECEIVING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        private static final ProgressTracker.Step RECEIVING_TRANSACTION = new ProgressTracker.Step(
                "Receiving proposed transaction from buyer.");
        private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step(
                "Signing proposed transaction with our private key.");
        private static final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step(
                "Obtaining notary signature and recording transaction.");

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
                final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
                // Obtain a reference to the notary we want to use and its public key.
                final CompositeKey notaryPubKey = notary.getOwningKey();

                // Stage 4.
                progressTracker.setCurrentStep(RECEIVING_TRANSACTION);
                // All messages come off the wire as UntrustworthyData. You need to 'unwrap' them. This is where you
                // validate what you have just received.
                final SignedTransaction partSignedTx = receive(SignedTransaction.class, otherParty)
                        .unwrap(tx ->
                        {
                            // Stage 5.
                            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
                            try {
                                // Check that the signature of the other party is valid.
                                // Our signature and the notary's signature are allowed to be omitted at this stage as this is only a
                                // partially signed transaction.
                                final WireTransaction wireTx = tx.verifySignatures(CryptoUtilities.getComposite(keyPair.getPublic()), notaryPubKey);
                                // Run the contract's verify function.
                                // We want to be sure that the PurchaseOrderState agreed upon is a valid instance of an
                                // PurchaseOrderContract. To do this we need to run the contract's verify() function.
                                wireTx.toLedgerTransaction(getServiceHub()).verify();
                            } catch (SignatureException | AttachmentResolutionException | TransactionResolutionException ex) {
                                throw new RuntimeException(ex);
                            }
                            return tx;
                        });

                // Stage 6.
                progressTracker.setCurrentStep(SIGNING_TRANSACTION);
                // Sign the transaction with our key pair and add it to the transaction.
                // We now have 'validation consensus'. We still require uniqueness consensus.
                // Technically validation consensus for this type of agreement implicitly provides uniqueness consensus.
                final DigitalSignature.WithKey mySig = partSignedTx.signWithECDSA(keyPair);
                // Add our signature to the transaction.
                final SignedTransaction signedTx = partSignedTx.plus(mySig);

                // Stage 7.
                progressTracker.setCurrentStep(FINALISING_TRANSACTION);
                final Set<Party> participants = ImmutableSet.of(getServiceHub().getMyInfo().getLegalIdentity(), otherParty);
                // FinalityFlow() notarises the transaction and records it in each party's vault.
                subFlow(new FinalityFlow(signedTx, participants));

                return new ExampleFlowResult.Success(String.format("Transaction id %s committed to ledger.", signedTx.getId()));

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
