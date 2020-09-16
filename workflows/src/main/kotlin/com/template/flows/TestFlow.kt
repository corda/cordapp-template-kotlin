package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class TestFlow(private val counterparties: List<Party>, private val someObject: SomeObject) : FlowLogic<SomeObject>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SomeObject {
        counterparties.forEach { counterParty ->
            val counterpartySession = initiateFlow(counterParty)
            val counterpartyData = counterpartySession.sendAndReceive<String>("ping")
            counterpartyData.unwrap { msg ->
                assert(msg == "pong")
            }
        }
        return someObject
    }
}

@InitiatedBy(TestFlow::class)
class TestFlowResponder(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val counterpartyData = counterpartySession.receive<String>()
        counterpartyData.unwrap { msg ->
            assert(msg == "ping")
        }
        counterpartySession.send("pong")
    }
}

@CordaSerializable
data class SomeObject(val integer: Int, val str: String, val bool: Boolean)