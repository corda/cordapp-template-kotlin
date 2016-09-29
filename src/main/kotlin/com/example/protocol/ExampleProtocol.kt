package com.example.protocol

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.ExampleContract
import com.example.contract.ExampleState
import com.example.deal.ExampleDeal
import com.r3corda.core.crypto.Party
import com.r3corda.core.protocols.ProtocolLogic
import com.r3corda.core.random63BitValue
import com.r3corda.core.transactions.SignedTransaction
import com.r3corda.node.services.api.AbstractNodeService
import com.r3corda.node.services.api.ServiceHubInternal
import com.r3corda.protocols.HandshakeMessage
import com.r3corda.protocols.TwoPartyDealProtocol
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This example shows a protocol agreeing to an abritrary deal and writing it to the ledger. This protocol is a simple
 * automatic agreement protocol.
 */
object ExampleProtocol {
    // The topic must be unique
    val TOPIC = "exampleprotocol.topic"

    data class OfferMessage(override val replyToParty: Party,
                            val notary: Party,
                            val dealBeingOffered: ExampleState,
                            override val sendSessionID: Long = random63BitValue(),
                            override val receiveSessionID: Long = random63BitValue()) : HandshakeMessage

    /**
     * Initiates the agreement between the two parties
     */
    class Requester(val swap: ExampleDeal, val otherParty: Party) : ProtocolLogic<SignedTransaction>() {
        override val topic: String get() = TOPIC

        @Suspendable
        override fun call(): SignedTransaction {
            require(serviceHub.networkMapCache.notaryNodes.isNotEmpty()) { "No notary nodes registered" }
            val notary = serviceHub.networkMapCache.notaryNodes.first().identity
            val myIdentity = serviceHub.storageService.myLegalIdentity
            val state = ExampleState(swap, myIdentity, otherParty, ExampleContract())

            send(otherParty, OfferMessage(myIdentity, notary, state))

            val stx = subProtocol(TwoPartyDealProtocol.Acceptor(otherParty, notary, state), inheritParentSessions = true)

            return stx;
        }
    }

    class Service(services: ServiceHubInternal): AbstractNodeService(services) {
        init {
            addProtocolHandler(TOPIC, "$TOPIC.Receiver") { offer: OfferMessage -> Receiver(offer) }
        }
    }

    class Receiver(private val offer: OfferMessage) : ProtocolLogic<SignedTransaction>()  {
        override val topic: String get() = TOPIC
        lateinit var ownParty: Party

        @Suspendable
        override fun call(): SignedTransaction {
            ownParty = serviceHub.storageService.myLegalIdentity
            val seller = TwoPartyDealProtocol.Instigator(offer.replyToParty, offer.notary,
                    offer.dealBeingOffered, serviceHub.storageService.myLegalIdentityKey)
            return subProtocol(seller, inheritParentSessions = true)
        }
    }
}