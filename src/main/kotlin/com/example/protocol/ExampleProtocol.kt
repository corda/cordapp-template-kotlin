package com.example.protocol

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.ExampleContract
import com.example.contract.ExampleState
import com.example.model.ExampleModel
import com.r3corda.core.crypto.Party
import com.r3corda.core.node.PluginServiceHub
import com.r3corda.core.protocols.ProtocolLogic

/**
 * This example shows a protocol sending and receiving data.
 */
object ExampleProtocol {
    /**
     * Initiates the agreement between the two parties
     */
    class Requester(val swap: ExampleModel, val otherParty: Party): ProtocolLogic<ExampleState>() {
        @Suspendable
        override fun call(): ExampleState {
            val state = ExampleState(swap, serviceHub.myInfo.legalIdentity, otherParty, ExampleContract())
            send(otherParty, state)
            return receive<ExampleState>(otherParty).unwrap { it }
        }
    }

    class Receiver(val otherParty: Party): ProtocolLogic<ExampleState>() {
        @Suspendable
        override fun call(): ExampleState {
            val offer = receive<ExampleState>(otherParty).unwrap { it }
            send(otherParty, offer)
            return offer
        }
    }

    class Service(services: PluginServiceHub) {
        init {
            services.registerProtocolInitiator(Requester::class, ::Receiver)
        }
    }
}