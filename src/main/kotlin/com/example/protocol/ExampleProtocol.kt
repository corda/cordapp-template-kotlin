package com.example.protocol

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.ExampleContract
import com.example.contract.ExampleState
import com.example.deal.ExampleDeal
import com.r3corda.core.crypto.Party
import com.r3corda.core.protocols.ProtocolLogic
import com.r3corda.node.services.api.ServiceHubInternal

/**
 * This example shows a protocol sending and receiving data.
 */
object ExampleProtocol {
    data class OfferMessage(val notary: Party, val dealBeingOffered: ExampleState)

    /**
     * Initiates the agreement between the two parties
     */
    class Requester(val swap: ExampleDeal, val otherParty: Party): ProtocolLogic<ExampleState>() {
        @Suspendable
        override fun call(): ExampleState {
            val state = ExampleState(swap, serviceHub.myInfo.legalIdentity, otherParty, ExampleContract())
            send(otherParty, state)
            return receive<ExampleState>(otherParty).unwrap { it }
        }
    }

    class Service(services: ServiceHubInternal) {
        init {
            services.registerProtocolInitiator(Requester::class, ::Receiver)
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
}