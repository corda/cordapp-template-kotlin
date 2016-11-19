package com.example.api

import com.example.contract.PurchaseOrderContract
import com.example.contract.PurchaseOrderState
import com.example.model.PurchaseOrder
import com.example.protocol.ExampleFlow
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.linearHeadsOfType
import net.corda.core.transactions.SignedTransaction
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(val services: ServiceHub) {
    val me: String = services.myInfo.legalIdentity.name
    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("who-am-i")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to me)

    /**
     * Returns all parties registered with the [NetworkMapService], the names can be used to look-up identities
     * by using the [IdentityService].
     */
    @GET
    @Path("get-peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers() = mapOf("peers" to services.networkMapCache.partyNodes
            .map { it.legalIdentity.name }
            .filter { it != me })

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("purchase-orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPurchaseOrders() = services.vaultService.linearHeadsOfType<PurchaseOrderState>()

    /**
<<<<<<< 2dd5f78fe6a381c8eff43f833b2f20d83086c104
     * This initiates a flow to agree a deal with the other party. Once the flow finishes it will
     * have written this deal to the ledger.
=======
     * This should only be called from the 'buyer' node. It initiates a protocol to agree a purchase order with a
     * seller. Once the protocol finishes it will have written the purchase order to ledger. Both the buyer and the
     * seller will be able to see it when calling /api/example/purchase-orders on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The protocol is invoked asynchronously. It returns a future when the protocol's call() method returns.
>>>>>>> First commit for updated corda-sdk.
     */
    @PUT
    @Path("{party}/create-purchase-order")
    fun createDeal(po: PurchaseOrder, @PathParam("party") partyName: String): Response {
        val otherParty = services.identityService.partyFromName(partyName)
        if(otherParty != null) {
            val state = PurchaseOrderState(po, services.myInfo.legalIdentity, otherParty, PurchaseOrderContract())
            // The line below blocks and waits for the future to resolve.
<<<<<<< 2dd5f78fe6a381c8eff43f833b2f20d83086c104
            services.invokeFlowAsync<ExampleState>(ExampleFlow.Requester::class.java, swap, otherParty).resultFuture.get()
=======
            services.invokeProtocolAsync<SignedTransaction>(ExampleFlow.Initiator::class.java, state, otherParty).resultFuture.get()
>>>>>>> First commit for updated corda-sdk.
            return Response.status(Response.Status.CREATED).build()
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }
}
