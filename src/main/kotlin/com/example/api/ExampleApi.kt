package com.example.api

import com.example.contract.ExampleState
import com.example.model.ExampleModel
import com.example.flow.ExampleFlow
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.linearHeadsOfType
import net.corda.core.transactions.SignedTransaction
import java.time.LocalDateTime
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(val services: ServiceHub) {

    /**
     * This returns the current time. Can be accessed from /api/example/current-date
     * Jackson is used to automatically serialise the object to valid JSON output.
     */
    @GET
    @Path("current-date")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCurrentDate(): Any {
        return mapOf("date" to LocalDateTime.now(services.clock).toLocalDate())
    }

    /**
     * Displays all current example deals in the ledger
     */
    @GET
    @Path("deals")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDeals(): Any {
        val states = services.vaultService.linearHeadsOfType<ExampleState>()
        return states
    }

    /**
     * This initiates a flow to agree a deal with the other party. Once the flow finishes it will
     * have written this deal to the ledger.
     */
    @PUT
    @Path("{party}/create-deal")
    fun createDeal(swap: ExampleModel, @PathParam("party") partyName: String): Response {
        val otherParty = services.identityService.partyFromName(partyName)
        if(otherParty != null) {
            // The line below blocks and waits for the future to resolve.
            services.invokeFlowAsync<ExampleState>(ExampleFlow.Requester::class.java, swap, otherParty).resultFuture.get()
            return Response.status(Response.Status.CREATED).build()
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }
}
