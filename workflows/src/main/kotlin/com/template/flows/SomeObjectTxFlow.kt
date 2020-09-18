package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.newSecureRandom
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.examples.yo.contracts.YoContract
import net.corda.examples.yo.flows.YoFlow
import net.corda.examples.yo.states.YoState
import java.util.*

@CordaSerializable
data class TestObject(val amount: Int, val string: String, val bool: Boolean)

@InitiatingFlow
@StartableByRPC
class TestFlow1(private val counterParty: Party):FlowLogic<String>() {
    override fun call(): String {
        return "TEST FLOW 1"
    }
}

@InitiatingFlow
@StartableByRPC
class TestFlow2(private val counterParty: Party):FlowLogic<String>() {
    override fun call(): String {
        return "TEST FLOW 2"
    }
}

@InitiatingFlow
@StartableByRPC
class SomeObjectTxFlow(private val counterparties: List<Party>, private val testObj: TestObject) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(){
        counterparties.forEach { counterParty ->
            val someObject = SomeObject(counterparties, testObj.amount, testObj.string, testObj.bool)
            val counterpartySession = initiateFlow(counterParty)
            val notary = serviceHub.networkMapCache.notaryIdentities.single() // METHOD 1
            val utx = TransactionBuilder(notary = notary).addOutputState(someObject, MyContract.contractId)
            val stx = serviceHub.signInitialTransaction(utx)
            stx.verify(serviceHub)
            subFlow(FinalityFlow(stx, listOf(counterpartySession)))
        }
    }
}

@InitiatedBy(SomeObjectTxFlow::class)
class SomeObjectTxFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

@CordaSerializable
data class SomeObject(val parties: List<Party>, val integer: Int, val str: String, val bool: Boolean): LinearState {
    override val linearId: UniqueIdentifier
        get() = UniqueIdentifier(newSecureRandom().toString(), UUID.randomUUID())
    override val participants: List<AbstractParty>
        get() = parties
}


class MyContract: Contract {
    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }
    override fun verify(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }
}
//    public static final String ID = "com.template.ContractB";