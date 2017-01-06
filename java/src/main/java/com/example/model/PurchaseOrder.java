package com.example.model;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * These files contain the data structures which the parties using this CorDapp will reach an agreement over. States can
 * support arbitrary complex object graphs. For a more complicated one, see
 *
 * samples/irs-demo/src/kotlin/net/corda/irs/contract/IRS.kt
 *
 * in the main Corda repo (http://github.com/corda/corda).
 *
 * These structures could be embedded within the ContractState. However, for clarity, we have moved them in to a
 * separate file.
 */

/**
 * A simple class representing a purchase order.
 */
public class PurchaseOrder {
    private int orderNumber;
    private Date deliveryDate;
    private Address deliveryAddress;
    private List<Item> items;

    public int getOrderNumber() { return orderNumber; }
    public Date getDeliveryDate() { return deliveryDate; }
    public Address getDeliveryAddress() { return deliveryAddress; }
    public List<Item> getItems() { return items; }

    public PurchaseOrder(int orderNumber, Date deliveryDate, Address deliveryAddress, List<Item> items) {
        this.orderNumber = orderNumber;
        this.deliveryDate = deliveryDate;
        this.deliveryAddress = deliveryAddress;
        this.items = items;
    }

    // Dummy constructor used by the createPurchaseOrder API endpoint.
    public PurchaseOrder() {}

    @Override public String toString() {
        return String.format("PurchaseOrder(orderNumber=%d, deliveryDate=%s, deliveryAddress=%s, items=%s)",
                orderNumber,
                deliveryDate.toString(),
                deliveryAddress.toString(),
                Arrays.toString(items.toArray()));
    }

    /**
     * An inner class representing a purchase order address.
     */
    public static class Address {
        private String city;
        private String country;

        public String getCity() { return city; }
        public String getCountry() { return country; }

        public Address(String city, String country) {
            this.city = city;
            this.country = country;
        }

        @Override public String toString() {
            return String.format("Address(city=%s, country=%s)", city, country);
        }
    }

    /**
     * An inner class representing a purchase order items.
     */
    public static class Item {
        private String name;
        private int amount;

        public String getName() { return name; }
        public int getAmount() { return amount; }

        public Item(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        @Override public String toString() {
            return String.format("Item(name=%s, amount=%d)", name, amount);
        }
    }
}