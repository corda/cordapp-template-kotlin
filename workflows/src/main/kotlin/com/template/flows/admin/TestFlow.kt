package com.template.flows.admin

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class Ping(private val counterparties: List<Party>, private val blah: Blah) : FlowLogic<Blah>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Blah {
        counterparties.forEach { counterParty ->
            val counterpartySession = initiateFlow(counterParty)
            val counterpartyData = counterpartySession.sendAndReceive<String>("ping")
            counterpartyData.unwrap { msg ->
                assert(msg == "pong")
            }
        }
        return blah
    }
}

@InitiatedBy(Ping::class)
class Pong(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
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
data class Blah(val integer: Int, val str: String, val bool: Boolean)