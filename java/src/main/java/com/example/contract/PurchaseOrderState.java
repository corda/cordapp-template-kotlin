package com.example.contract;

import com.example.contract.PurchaseOrderContract.Commands.Place;
import com.example.model.PurchaseOrder;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionType;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

/**
 * The state object that we will use to record the agreement of a valid purchase order issued by a buyer to a seller.
 *
 * There are a few key state interfaces, the most fundamental of which is [ContractState]. We have defined other
 * interfaces for different requirements. In this case, we are implementing a [DealState] which defines a few helper
 * properties and methods for managing states pertaining to deals.
 */
public class PurchaseOrderState implements DealState {
    private final PurchaseOrder purchaseOrder;
    private final Party buyer;
    private final Party seller;
    private final PurchaseOrderContract contract;
    private final UniqueIdentifier linearId;

    public PurchaseOrderState(PurchaseOrder purchaseOrder,
                              Party buyer,
                              Party seller,
                              PurchaseOrderContract contract)
    {
        this.purchaseOrder = purchaseOrder;
        this.buyer = buyer;
        this.seller = seller;
        this.contract = contract;
        this.linearId = new UniqueIdentifier(
                Integer.toString(purchaseOrder.getOrderNumber()),
                UUID.randomUUID());
    }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Party getBuyer() { return buyer; }
    public Party getSeller() { return seller; }
    @Override public PurchaseOrderContract getContract() { return contract; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public String getRef() { return linearId.getExternalId(); }
    @Override public Integer getEncumbrance() { return null; }
    @Override public List<Party> getParties() { return Arrays.asList(buyer, seller); }
    @Override public List<CompositeKey> getParticipants() {
        return getParties()
                .stream()
                .map(Party::getOwningKey)
                .collect(toList());
    }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        final List<PublicKey> partyKeys = getParties()
                .stream()
                .flatMap(party -> party.getOwningKey().getKeys().stream())
                .collect(toList());
        return ourKeys
                .stream()
                .anyMatch(partyKeys::contains);

    }

    /**
     * Helper function to generate a new Issue() purchase order transaction. For more details on building transactions
     * see the API for [TransactionBuilder] in the JavaDocs.
     * https://docs.corda.net/api/net.corda.core.transactions/-transaction-builder/index.html
     */
    @Override public TransactionBuilder generateAgreement(Party notary) {
        return new TransactionType.General.Builder(notary)
                .withItems(this, new Command(new Place(), getParticipants()));
    }
}