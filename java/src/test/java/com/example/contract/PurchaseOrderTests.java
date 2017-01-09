package com.example.contract;

import com.example.model.PurchaseOrder;
import com.example.model.PurchaseOrder.Address;
import com.example.model.PurchaseOrder.Item;
import net.corda.core.crypto.CompositeKey;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static net.corda.core.utilities.TestConstants.getTEST_TX_TIME;
import static net.corda.testing.CoreTestUtils.*;

public class PurchaseOrderTests {
    @Test
    public void transactionMustBeTimestamped() {
        Address address = new Address("London", "UK");
        List<Item> items = Collections.singletonList(new Item("Hammer", 1));
        Instant deliveryTime = getTEST_TX_TIME().plus(Duration.ofDays(7));
        PurchaseOrder purchaseOrder = new PurchaseOrder(1, new Date(deliveryTime.toEpochMilli()), address, items);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new PurchaseOrderState(purchaseOrder, getMINI_CORP(), getMEGA_CORP(), new PurchaseOrderContract()));
                CompositeKey[] keys = new CompositeKey[2];
                keys[0] = getMEGA_CORP_PUBKEY();
                keys[1] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, PurchaseOrderContract.Commands.Place::new);
                txDSL.failsWith("must be timestamped");
                txDSL.timestamp(getTEST_TX_TIME());
                txDSL.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustIncludePlaceCommand() {
        Address address = new Address("London", "UK");
        List<Item> items = Collections.singletonList(new Item("Hammer", 1));
        Instant deliveryTime = getTEST_TX_TIME().plus(Duration.ofDays(7));
        PurchaseOrder purchaseOrder = new PurchaseOrder(1, new Date(deliveryTime.toEpochMilli()), address, items);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new PurchaseOrderState(purchaseOrder, getMINI_CORP(), getMEGA_CORP(), new PurchaseOrderContract()));
                txDSL.timestamp(getTEST_TX_TIME());
                txDSL.fails();
                CompositeKey[] keys = new CompositeKey[2];
                keys[0] = getMEGA_CORP_PUBKEY();
                keys[1] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, PurchaseOrderContract.Commands.Place::new);
                txDSL.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void buyerMustSignTransaction() {
        Address address = new Address("London", "UK");
        List<Item> items = Collections.singletonList(new Item("Hammer", 1));
        Instant deliveryTime =  getTEST_TX_TIME().plus(Duration.ofDays(7));
        PurchaseOrder purchaseOrder = new PurchaseOrder(1, new Date(deliveryTime.toEpochMilli()), address, items);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new PurchaseOrderState(purchaseOrder, getMINI_CORP(), getMEGA_CORP(), new PurchaseOrderContract()));
                txDSL.timestamp(getTEST_TX_TIME());
                CompositeKey[] keys = new CompositeKey[1];
                keys[0] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, PurchaseOrderContract.Commands.Place::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void sellerMustSignTransaction() {
        Address address = new Address("London", "UK");
        List<Item> items = Collections.singletonList(new Item("Hammer", 1));
        Instant deliveryTime =  getTEST_TX_TIME().plus(Duration.ofDays(7));
        PurchaseOrder purchaseOrder = new PurchaseOrder(1, new Date(deliveryTime.toEpochMilli()), address, items);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new PurchaseOrderState(purchaseOrder, getMINI_CORP(), getMEGA_CORP(), new PurchaseOrderContract()));
                txDSL.timestamp(getTEST_TX_TIME());
                CompositeKey[] keys = new CompositeKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, PurchaseOrderContract.Commands.Place::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void cannotPlaceEmptyOrders() {
        Address address = new Address("London", "UK");
        List<Item> items = Collections.emptyList();
        Instant deliveryTime =  getTEST_TX_TIME().plus(Duration.ofDays(7));
        PurchaseOrder purchaseOrder = new PurchaseOrder(1, new Date(deliveryTime.toEpochMilli()), address, items);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new PurchaseOrderState(purchaseOrder, getMINI_CORP(), getMEGA_CORP(), new PurchaseOrderContract()));
                txDSL.timestamp(getTEST_TX_TIME());
                CompositeKey[] keys = new CompositeKey[2];
                keys[0] = getMEGA_CORP_PUBKEY();
                keys[1] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, PurchaseOrderContract.Commands.Place::new);
                txDSL.failsWith("must order at least one type of item");
                return null;
            });
            return null;
        });
    }

    @Test
    public void cannotPlaceHistoricalOrders() {
        Address address = new Address("London", "UK");
        List<Item> items = Collections.singletonList(new Item("Hammer", 1));
        Instant deliveryTime =  getTEST_TX_TIME().minus(Duration.ofDays(7));
        PurchaseOrder purchaseOrder = new PurchaseOrder(1, new Date(deliveryTime.toEpochMilli()), address, items);
        ledger(ledgerDSL -> {
            transaction(txDSL -> {
                txDSL.output(new PurchaseOrderState(purchaseOrder, getMINI_CORP(), getMEGA_CORP(), new PurchaseOrderContract()));
                txDSL.timestamp(getTEST_TX_TIME());
                CompositeKey[] keys = new CompositeKey[2];
                keys[0] = getMEGA_CORP_PUBKEY();
                keys[1] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, PurchaseOrderContract.Commands.Place::new);
                txDSL.failsWith("delivery date must be in the future");
                return null;
            });
            return null;
        });
    }
}
