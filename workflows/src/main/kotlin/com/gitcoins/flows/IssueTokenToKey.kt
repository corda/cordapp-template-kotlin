package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.contracts.commands.IssueTokenCommand
import com.r3.corda.sdk.token.contracts.states.AbstractToken
import com.r3.corda.sdk.token.contracts.types.IssuedTokenType
import com.r3.corda.sdk.token.contracts.types.TokenType
import com.r3.corda.sdk.token.contracts.utilities.heldBy
import com.r3.corda.sdk.token.contracts.utilities.issuedBy
import com.r3.corda.sdk.token.contracts.utilities.withNotary
import com.r3.corda.sdk.token.workflow.flows.RequestConfidentialIdentity
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionState
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.security.PublicKey

/**
 * Temporary flow to issue a token to an [AnonymousParty] when provided it's [PublicKey]. This can be deleted when the
 * bug around being unable to issue tokens to yourself is fixed.
 */
object IssueTokenToKey {

    @CordaSerializable
    data class TokenIssuanceNotification(val anonymous: Boolean)

    @InitiatingFlow
    @StartableByRPC
    class Initiator<T : TokenType>(
            private val token: T,
            private val key: PublicKey,
            private val notary: Party,
            private val amount: Amount<T>? = null,
            private val anonymous: Boolean = true
    ) : FlowLogic<SignedTransaction>() {

        companion object {
            object ISSUANCE_NOTIFICATION : ProgressTracker.Step("Sending issuance notification to counterparty.")
            object REQUEST_CONF_ID : ProgressTracker.Step("Requesting confidential identity.")
            object DIST_LIST : ProgressTracker.Step("Adding party to distribution list.")
            object SIGNING : ProgressTracker.Step("Signing transaction proposal.")
            object RECORDING : ProgressTracker.Step("Recording signed transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(ISSUANCE_NOTIFICATION, REQUEST_CONF_ID, DIST_LIST, SIGNING, RECORDING)
        }

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            // This is the identity which will be used to issue tokens.
            val me: Party = ourIdentity

            progressTracker.currentStep = ISSUANCE_NOTIFICATION

            // This is the recipient of the tokens identity.
            val owningParty = AnonymousParty(key)

            // Create the issued token. We add this to the commands for grouping.
            val issuedToken: IssuedTokenType<T> = token issuedBy me

            // Create the token. It's either an NonFungibleToken or FungibleToken.
            val ownedToken: AbstractToken = if (amount == null) {
                issuedToken heldBy owningParty
            } else {
                amount issuedBy me heldBy owningParty
            }

            // Create the transaction.
            val transactionState: TransactionState<AbstractToken> = ownedToken withNotary notary
            val utx: TransactionBuilder = TransactionBuilder(notary = notary).apply {
                addCommand(data = IssueTokenCommand(issuedToken), keys = listOf(me.owningKey))
                addOutputState(state = transactionState)
            }
            progressTracker.currentStep = SIGNING
            // Sign the transaction. Only Concrete Parties should be used here.
            val stx: SignedTransaction = serviceHub.signInitialTransaction(utx)
            progressTracker.currentStep = RECORDING
            return subFlow(FinalityFlow(transaction = stx,
                    progressTracker = RECORDING.childProgressTracker(),
                    sessions = listOf()
            ))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            // Receive an issuance notification from the issuer. It tells us if we need to sign up for token updates or
            // generate a confidential identity.
            val issuanceNotification = otherSession.receive<TokenIssuanceNotification>().unwrap { it }

            // Generate and send over a new confidential identity, if necessary.
            if (issuanceNotification.anonymous) {
                subFlow(RequestConfidentialIdentity.Responder(otherSession))
            }

            // Resolve the issuance transaction.
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession, statesToRecord = StatesToRecord.ONLY_RELEVANT))
        }
    }

}