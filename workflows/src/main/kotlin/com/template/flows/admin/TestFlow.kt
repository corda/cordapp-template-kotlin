package com.template.flows.admin

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class Ping(private val counterparty: Party) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val counterpartySession = initiateFlow(counterparty)
        val counterpartyData = counterpartySession.sendAndReceive<String>("ping")
        counterpartyData.unwrap { msg ->
            assert(msg == "pong")
        }
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