package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.internal.ResolveTransactionsFlow
import net.corda.core.internal.notary.NotaryInternalException
import net.corda.core.internal.notary.SinglePartyNotaryService
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.TransactionWithSignatures
import net.corda.core.utilities.ProgressTracker
import net.corda.node.services.api.ServiceHubInternal
import net.corda.node.services.transactions.PersistentUniquenessProvider
import net.corda.node.services.transactions.ValidatingNotaryFlow
import java.security.PublicKey

@BelongsToContract(MyContract::class)
class MyState(val party: Party): ContractState {
    override val participants = listOf(party)
}
class MyContract: Contract {
    override fun verify(tx: LedgerTransaction) { }
}
class MyCommand : TypeOnlyCommandData()

@InitiatingFlow
@StartableByRPC
class Initiator : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(MyState(ourIdentity), "com.template.flows.MyContract")
                .addCommand(MyCommand(), ourIdentity.owningKey)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val txId = subFlow(FinalityFlow(signedTx, listOf())).tx.id

        waitForLedgerCommit(txId)

        val input = serviceHub.vaultService.queryBy<MyState>().states.first()

        val txBuilder2 = TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(MyState(ourIdentity), "com.template.flows.MyContract")
                .addCommand(MyCommand(), ourIdentity.owningKey)
        val signedTx2 = serviceHub.signInitialTransaction(txBuilder2)
        subFlow(FinalityFlow(signedTx2, listOf()))
    }
}

class MyCustomValidatingNotaryService(override val services: ServiceHubInternal, override val notaryIdentityKey: PublicKey) : SinglePartyNotaryService() {
    override val uniquenessProvider = PersistentUniquenessProvider(services.clock, services.database, services.cacheFactory)

    override fun createServiceFlow(otherPartySession: FlowSession): FlowLogic<Void?> = MyValidatingNotaryFlow(otherPartySession, this)

    override fun start() {}
    override fun stop() {}
}

class MyValidatingNotaryFlow(otherSide: FlowSession, service: MyCustomValidatingNotaryService) : ValidatingNotaryFlow(otherSide, service) {
    @Suspendable
    override fun verifyTransaction(requestPayload: NotarisationPayload) {
        try {
            val stx = requestPayload.signedTransaction
            resolveAndContractVerify(stx)
            verifySignatures(stx)
            customVerify(stx)
        } catch (e: Exception) {
            throw  NotaryInternalException(NotaryError.TransactionInvalid(e))
        }
    }

    @Suspendable
    private fun resolveAndContractVerify(stx: SignedTransaction) {
        subFlow(ResolveTransactionsFlow(stx, otherSideSession))
        stx.verify(serviceHub, false)
    }

    private fun verifySignatures(stx: SignedTransaction) {
        val transactionWithSignatures = stx.resolveTransactionWithSignatures(serviceHub)
        checkSignatures(transactionWithSignatures)
    }

    private fun checkSignatures(tx: TransactionWithSignatures) {
        tx.verifySignaturesExcept(service.notaryIdentityKey)
    }

    private fun customVerify(stx: SignedTransaction) {
        // Add custom verification logic
    }
}
