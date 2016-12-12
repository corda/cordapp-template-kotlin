package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object PurchaseOrderSchema

// TODO: Add schema for purchase order items.
object PurchaseOrderSchemaV1 : MappedSchema(
        schemaFamily = PurchaseOrderSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPurchaseOrder::class.java)) {
    @Entity
    @Table(name = "purchase_order_states")
    class PersistentPurchaseOrder(
            @Column(name = "purchase_order_id")
            var purchaseOrderId: Int,

            @Column(name = "buyer_name")
            var buyerName: String,

            @Column(name = "seller_name")
            var sellerName: String,

            @Column(name = "linear_id")
            var linearId: String,

            @Column(name = "delivery_date")
            var deliveryDate: Date,

            @Column(name = "delivery_city")
            var deliveryCity: String,

            @Column(name = "delivery_country")
            var deliveryCountry: String
    ) : PersistentState()
}