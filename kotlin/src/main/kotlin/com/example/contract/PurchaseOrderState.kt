package com.example.contract

import com.example.contract.PurchaseOrderContract.Commands
import com.example.model.PurchaseOrder
import net.corda.core.contracts.Command
import net.corda.core.contracts.DealState
import net.corda.core.contracts.TransactionType
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Party
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

/**
 * The state object which we will use the record the agreement of a valid purchase order issued by a buyer to a seller.
 *
 * There are a few key state interfaces. The most fundamental of which is [ContractState]. We have defined other
 * interfaces for different requirements. In this case we are implementing a [DealState] which defines a few helper
 * properties and methods for managing states pertaining to deals.
 *
 * @param po details of the purchase order
 * @param buyer the party issuing the purchase order
 * @param seller the party receiving and approving the purchase order
 * @param contract a reference to the contract code which governs how this state object can behave given particular
 * transaction types.
 * @param linearId Unique id shared by all [LinearState] states throughout history within the vaults of all parties.
 */
data class PurchaseOrderState(val purchaseOrder: PurchaseOrder,
                              val buyer: Party,
                              val seller: Party,
                              override val contract: PurchaseOrderContract,
                              override val linearId: UniqueIdentifier = UniqueIdentifier(purchaseOrder.orderNumber.toString())):
        DealState {
    /** Another ref field, for matching with data in external systems. In this case the external Id is the po number. */
    override val ref: String get() = linearId.externalId!!
    /** List of parties involved in this particular deal */
    override val parties: List<Party> get() = listOf(buyer, seller)
    /** The public keys of the parties that are able to consume this state in a valid transaction. */
    override val participants: List<CompositeKey> get() = parties.map { it.owningKey }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    override fun isRelevant(ourKeys: Set<PublicKey>): Boolean {
        val partyKeys = parties.flatMap { it.owningKey.keys }
        return ourKeys.intersect(partyKeys).isNotEmpty()
    }

    /**
     * Helper function to generate a new Issue() purchase order transaction. For more details on building transactions
     * see the API for [TransactionBuilder] in the JavaDocs.
     *
     * https://docs.corda.net/api/net.corda.core.transactions/-transaction-builder/index.html
     * */
    override fun generateAgreement(notary: Party): TransactionBuilder {
        return TransactionType.General.Builder(notary)
                .withItems(this, Command(Commands.Place(), participants))
    }
}
