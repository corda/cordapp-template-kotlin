package com.example.api

import com.r3corda.core.node.ServiceHub
import java.time.LocalDateTime
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

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
    fun getBusinessDate(): Any {
        return LocalDateTime.now(services.clock).toLocalDate()
    }
}