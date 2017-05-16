package com.template.api;

import net.corda.core.messaging.CordaRPCOps;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

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
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("message", "Template GET endpoint.");
        return Response.ok(entity).build();
    }

    /**
     * Accessible at /api/template/templatePutEndpoint.
     */
    @PUT
    @Path("templatePutEndpoint")
    public Response templatePutEndpoint(Object payload) {
        return Response.ok("Template PUT endpoint.").build();
    }
}