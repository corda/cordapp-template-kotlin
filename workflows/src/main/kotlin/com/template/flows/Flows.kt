package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap
import org.bouncycastle.crypto.tls.HashAlgorithm.sha256
import java.nio.ByteBuffer
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator(val messageSize : Int, val toSendTo : Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()


    @Suspendable
    override fun call() {
        val byteBuffer = ByteBuffer.allocate(messageSize)
        val msgBuffer = byteBuffer.array()
        Random().nextBytes(msgBuffer)

        val counterPartySession = initiateFlow(toSendTo)
        counterPartySession.send(msgBuffer)

        val hashedReturn = counterPartySession.receive<SecureHash.SHA256>().unwrap{data -> data}
        val hashedMsg = SecureHash.Companion.sha256(msgBuffer)

        if(hashedMsg.equals(hashedReturn)){
            println("Flow concluded : messages equal")
        } else {
            println("Flow concluded : messages not equal")
        }
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val messageSize : ByteArray = counterpartySession.receive<ByteArray>().unwrap{data -> data}
        val hashedMsg = SecureHash.Companion.sha256(messageSize)
        counterpartySession.send(hashedMsg)
    }
}
