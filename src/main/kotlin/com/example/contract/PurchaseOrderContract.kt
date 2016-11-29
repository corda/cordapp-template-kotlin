package com.example.contract

import com.example.model.Item
import net.corda.core.contracts.*
import net.corda.core.contracts.clauses.*
import net.corda.core.crypto.SecureHash
import net.corda.core.random63BitValue

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract facilitates the business logic required for two parties to come to an agreement over a newly issued
 * [PurchaseOrderState], which in turn, encapsulates a [PurchaseOrder].
 *
 * For a new [PurchaseOrderState] to be issued onto the ledger a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [PurchaseOrderState].
 * - An Issue() command with the public keys of the buyer and seller parties.
 * - A timestamp.
 *
 * The contract code (implemented within the [Timestamped] and [Issue] clauses) is run when the transaction is
 * verified via the verify() function.
 * - [Timestamped] checks for the existence of a timestamp
 * - [Issue] runs a series of constraints, see more below
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class PurchaseOrderContract() : Contract {
    /**
     * The AllComposition() clause mandates that all specified clauses clauses (in this case [Timestamped] and [Group])
     * must be executed and valid for a transaction involving this type of contract to be valid.
     */
    override fun verify(tx: TransactionForContract) =
            verifyClause(tx, AllComposition(Clauses.Timestamp(), Clauses.Group()), tx.commands.select<Commands>())

    /**
     * Currently this contract only implements one command.
     * If you wish to add further commands to perhaps Amend() or Cancel() a purchase order, you would add them here. You
     * would then need to add associated clauses to handle transaction verification for the new commands.
     */
    interface Commands : CommandData {
        data class Place(override val nonce: Long = random63BitValue()) : IssueCommand, Commands
//        // Additional commands defined below.
//        data class Amend(): TypeOnlyCommandData, Commands
    }

    /** This is a reference to the underlying legal contract template and associated parameters. */
    override val legalContractReference: SecureHash = SecureHash.sha256("purchase order contract template and params")

    /** This is where we implement our clauses. */
    interface Clauses {
        /** Checks for the existence of a timestamp. */
        class Timestamp : Clause<ContractState, Commands, Unit>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<ContractState>,
                                outputs: List<ContractState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: Unit?): Set<Commands> {
                require(tx.timestamp?.midpoint != null) { "must be timestamped" }
                // We return an empty set because we don't process any commands
                return emptySet()
            }
        }

        // If you add additional clauses. Make sure to reference them within the 'Anycomposition()' clause.
        class Group : GroupClauseVerifier<PurchaseOrderState, Commands, UniqueIdentifier>(AnyComposition(Place())) {
            override fun groupStates(tx: TransactionForContract): List<TransactionForContract.InOutGroup<PurchaseOrderState, UniqueIdentifier>>
                    // Group by purchase order linearId for in / out states
                    = tx.groupStates(PurchaseOrderState::linearId)
        }

        class Place : Clause<PurchaseOrderState, Commands, UniqueIdentifier>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<PurchaseOrderState>,
                                outputs: List<PurchaseOrderState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.Place>()
                requireThat {
                    // Generic constraints around generation of the issue purchase order transaction.
                    "No inputs should be consumed when issuing a purchase order." by (inputs.isEmpty())
                    "Only one output state should be created for each group." by (outputs.size == 1)
                    val out = outputs.single()
                    "The buyer and the seller cannot be the same entity." by (out.buyer != out.seller)
                    "The 'participants' and 'parties' must be the same." by (out.parties.map { it.owningKey }.containsAll(out.participants))
                    "The buyer and the seller are the parties." by (out.parties.containsAll(listOf(out.buyer, out.seller)))

                    // Purchase order specific constraints.
                    "We only deliver to the UK." by (out.po.deliveryAddress.country == "UK")
                    "You must order at least one type of item." by (out.po.items.isNotEmpty())
                    "You cannot order zero or negative amounts of an item." by (out.po.items.map(Item::amount).all { it > 0 })
                    "You can only order up to 10 items at a time." by (out.po.items.map(Item::amount).sum() <= 10)
                    val time = tx.timestamp?.midpoint
                    "The delivery date must be in the future." by (out.po.deliveryDate.toInstant() > time)
                }

                return setOf(command.value)
            }
        }

//        // Additional clauses go here.
//        class Amend : Clause<PurchaseOrderState, Commands, UniqueIdentifier>() {
//            override fun verify(tx: TransactionForContract,
//                                inputs: List<PurchaseOrderState>,
//                                outputs: List<PurchaseOrderState>,
//                                commands: List<AuthenticatedObject<Commands>>,
//                                groupingKey: UniqueIdentifier?): Set<Commands> {
//                val command = tx.commands.requireSingleCommand<Commands.Amend>()
//                requireThat {
//                    // Generic constraints around amending purchase orders.
//                    // ...
//                    // Purchase order specific constraints.
//                    // ...
//                }
//                return setOf(command.value)
//            }
//        }
    }
}
