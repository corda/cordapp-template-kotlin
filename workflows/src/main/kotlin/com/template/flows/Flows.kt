package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.nio.ByteBuffer

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator(val messageSize : Int, val toSendTo : Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()


    @Suspendable
    override fun call() {
        // Initiator flow logic goes here.
        val bb = ByteBuffer.allocate(4)
        bb.putInt(messageSize)
        val byteArray = bb.array()
        val counterParty = initiateFlow(toSendTo)


    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}
