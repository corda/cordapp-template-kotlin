package com.template.flows.paymentFlows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldTokenAmountCriteria
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import com.r3.corda.lib.tokens.workflows.utilities.sumTokenCriteria
import com.template.contracts.TransferContract
import com.template.flows.utilities.GenerateKey
import com.template.states.TransferState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat

import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * A Flow to send money between accounts
 */
@StartableByRPC
@InitiatingFlow
@StartableByService
class SendMoney(val sender: String, val receiver: String, val amount: Long, val currency: String) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        val token = getInstance(currency)

        // Retrieve All Accounts
        val allAccounts = accountService.ourAccounts()
        val issuedTokenType =  IssuedTokenType(ourIdentity, token)

        // Get Sender AccountInfo
        val senderAccount = allAccounts.single { it.state.data.name == sender}.state.data

        // Get Receiver AccountIngo
        val receiverAccount = accountService.accountInfo(receiver).single().state.data

        requireThat {"Sender not hosted on this node" using (senderAccount.host == ourIdentity) }

        val senderKey = subFlow(GenerateKey(senderAccount.identifier.id)).owningKey
        val receiverKey = subFlow(RequestKeyForAccount(receiverAccount))

        // Create an instance of a TransferState
        val output = TransferState(Amount(amount, issuedTokenType),receiverKey,AnonymousParty(senderKey))

        // Get Notary and build transaction
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)
        builder.addOutputState(output).addCommand(TransferContract.Commands.Create(), listOf(receiverKey.owningKey,senderKey))

        // Sign Transaction
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey,senderKey))

        // Collect Signatures
        val sessionForAccountToSendTo = initiateFlow(receiverAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSendTo, receiverKey.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAccountToSendTo).filter { it.counterparty != ourIdentity }))

        return "Complete transferred money from $sender to $receiver"
    }
}

@InitiatedBy(SendMoney::class)
class SendPaymentResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>(){
    @Suspendable
    override fun call() {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                // Custom Logic to validate transaction.
            }
        })
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}