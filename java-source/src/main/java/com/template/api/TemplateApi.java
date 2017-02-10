package com.template.api;

import net.corda.core.messaging.CordaRPCOps;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;

// This API is accessible from /api/template. The endpoint paths specified below are relative to it.
@Path("template")
public class TemplateApi {
    private final CordaRPCOps services;

    public TemplateApi(CordaRPCOps services) {
        this.services = services;
    }

    /**
     * Accessible at /api/template/templateGetEndpoint.
     */
    @GET
    @Path("templateGetEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    public Response templateGetEndpoint() {
        return Response.accepted().entity("Template GET endpoint.").build();
    }

    /**
     * Accessible at /api/template/templatePutEndpoint.
     */
    @PUT
    @Path("templatePutEndpoint")
    public Response templatePutEndpoint(Object payload) {
        return Response.accepted().entity("Template PUT endpoint.").build();
    }
}