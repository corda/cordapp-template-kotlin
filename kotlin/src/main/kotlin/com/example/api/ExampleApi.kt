package com.example.api

import com.example.contract.PurchaseOrderContract
import com.example.contract.PurchaseOrderState
import com.example.flow.ExampleFlow.Initiator
import com.example.flow.ExampleFlowResult
import com.example.model.PurchaseOrder
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val NOTARY_NAME = "Controller"

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(val services: CordaRPCOps) {
    val myLegalName: String = services.nodeIdentity().legalIdentity.name

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService], the names can be used to look-up identities
     * by using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers() = mapOf("peers" to services.networkMapUpdates().first
            .map { it.legalIdentity.name }
            .filter { it != myLegalName && it != NOTARY_NAME })

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("purchase-orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPurchaseOrders() = services.vaultAndUpdates().first

    /**
     * This should only be called from the 'buyer' node. It initiates a flow to agree a purchase order with a
     * seller. Once the flow finishes it will have written the purchase order to ledger. Both the buyer and the
     * seller will be able to see it when calling /api/example/purchase-orders on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("{party}/create-purchase-order")
    fun createPurchaseOrder(purchaseOrder: PurchaseOrder, @PathParam("party") partyName: String): Response {
        val otherParty = services.partyFromName(partyName)
        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val state = PurchaseOrderState(
                purchaseOrder,
                services.nodeIdentity().legalIdentity,
                otherParty,
                PurchaseOrderContract())

        // The line below blocks and waits for the future to resolve.
        val result: ExampleFlowResult = services
                .startFlow(::Initiator, state, otherParty)
                .returnValue
                .toBlocking()
                .first()

        when (result) {
            is ExampleFlowResult.Success ->
                return Response
                        .status(Response.Status.CREATED)
                        .entity(result.message)
                        .build()
            is ExampleFlowResult.Failure ->
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(result.message)
                        .build()
        }
    }
}