package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.ExampleContract
import com.example.contract.ExampleState
import com.example.model.ExampleModel
import net.corda.core.crypto.Party
import net.corda.core.node.PluginServiceHub
import net.corda.core.flows.FlowLogic

/**
 * This example shows a flow sending and receiving data.
 */
object ExampleFlow {
    /**
     * Initiates the agreement between the two parties
     */
    class Requester(val swap: ExampleModel, val otherParty: Party): FlowLogic<ExampleState>() {
        @Suspendable
        override fun call(): ExampleState {
            val state = ExampleState(swap, serviceHub.myInfo.legalIdentity, otherParty, ExampleContract())
            send(otherParty, state)
            return receive<ExampleState>(otherParty).unwrap { it }
        }
    }

    class Receiver(val otherParty: Party): FlowLogic<ExampleState>() {
        @Suspendable
        override fun call(): ExampleState {
            val offer = receive<ExampleState>(otherParty).unwrap { it }
            send(otherParty, offer)
            return offer
        }
    }

    class Service(services: PluginServiceHub) {
        init {
            services.registerFlowInitiator(Requester::class, ::Receiver)
        }
    }
}
