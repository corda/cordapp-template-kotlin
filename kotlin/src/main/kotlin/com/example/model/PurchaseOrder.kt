package com.example.model

import java.util.*

/**
 * This file contains the data structures which the parties using this CorDapp will reach an agreement over. States can
 * support arbitrary complex object graphs. For a more complicated one, see
 *
 * samples/irs-demo/src/kotlin/net/corda/irs/contract/IRS.kt
 *
 * in the main Corda repo (http://github.com/corda/corda).
 *
 * These structures could be embedded within the ContractState, however for clarity we have moved them in to a separate
 * file.
 */

/**
 * The name, amount and price of an item to be purchased. It is assumed that the buyer has the seller's item catalogue
 * and will only place orders for valid items. Of course, a reference to a particular version of the catalogue could be
 * included with the Issue() purchase order transaction as an attachment, such that the seller can check the items are valid.
 * For more details on attachments see
 *
 * samples/attachment-demo/src/kotlin/net/corda/attachmentdemo
 *
 * in the main Corda repo (http://github.com/corda/corda).
 *
 * In the contract verify code, we have written some constraints about items.
 * @param the name of the item to be delivered
 * @param amount the amount of an item to be delivered
 */
data class Item(val name: String, val amount: Int)

/**
 * Simple class to represent an address.
 * @param city the city to which the items will be delivered.
 * @param country the country to which the items will be delivered.
 */
data class Address(val city: String, val country: String)

/**
 * A simple class representing a purchase order.
 * @param orderNumber the purchase order's id number.
 * @param deliveryDate the requested deliveryDate.
 * @param deliveryAddress the delivery address.
 * @param items a list of items to be purchased.
 */
data class PurchaseOrder(val orderNumber: Int,
                         val deliveryDate: Date,
                         val deliveryAddress: Address,
                         val items: List<Item>)