package com.example.api;

import com.example.contract.PurchaseOrderContract;
import com.example.contract.PurchaseOrderState;
import com.example.flow.ExampleFlow;
import com.example.model.PurchaseOrder;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static net.corda.core.Utils.getOrThrow;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class ExampleApi {
    private final CordaRPCOps services;
    private final String myLegalName;

    public ExampleApi(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeIdentity().getLegalIdentity().getName();
    }

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> whoami() { return singletonMap("me", myLegalName); }

    /**
     * Returns all parties registered with the [NetworkMapService]. The names can be used to look up identities by
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getPeers() {
        final String NOTARY_NAME = "Controller";
        return singletonMap(
                "peers",
                services.networkMapUpdates().getFirst()
                        .stream()
                        .map(node -> node.getLegalIdentity().getName())
                        .filter(name -> !name.equals(myLegalName) && !name.equals(NOTARY_NAME))
                        .collect(toList()));
    }

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("purchase-orders")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getPurchaseOrders() {
        return services.vaultAndUpdates().getFirst();
    }

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
    public Response createPurchaseOrder(PurchaseOrder purchaseOrder, @PathParam("party") String partyName) throws InterruptedException, ExecutionException {
        final Party otherParty = services.partyFromName(partyName);

        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final PurchaseOrderState state = new PurchaseOrderState(
                purchaseOrder,
                services.nodeIdentity().getLegalIdentity(),
                otherParty,
                new PurchaseOrderContract());

        // The line below blocks and waits for the flow to return.
        final ExampleFlow.ExampleFlowResult result = getOrThrow(services
                .startFlowDynamic(ExampleFlow.Initiator.class, state, otherParty)
                .getReturnValue(), null);

        final Response.Status status;
        if (result instanceof ExampleFlow.ExampleFlowResult.Success) {
            status = Response.Status.CREATED;
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return Response
                .status(status)
                .entity(result.toString())
                .build();
    }
}