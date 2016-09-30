package com.example.contract

import com.r3corda.core.contracts.*
import com.r3corda.core.contracts.clauses.*
import com.r3corda.core.crypto.SecureHash
import java.math.BigDecimal

open class ExampleContract() : Contract {
    override fun verify(tx: TransactionForContract) = verifyClause(tx, AllComposition(Clauses.Timestamped(), Clauses.Group()), tx.commands.select<Commands>())

    interface Commands : CommandData {
        class Agree : TypeOnlyCommandData(), Commands  // Both sides agree to trade
    }

    override val legalContractReference: SecureHash = SecureHash.sha256("OGTRADE.KT")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as ExampleContract
        if (legalContractReference != other.legalContractReference) return false
        return true
    }

    interface Clauses {
        class Timestamped : Clause<ContractState, Commands, Unit>() {
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

        class Group : GroupClauseVerifier<ExampleState, Commands, UniqueIdentifier>(AnyComposition(Agree())) {
            override fun groupStates(tx: TransactionForContract): List<TransactionForContract.InOutGroup<ExampleState, UniqueIdentifier>>
                    // Group by Trade ID for in / out states
                    = tx.groupStates() { state -> state.linearId }
        }

        class Agree : Clause<ExampleState, Commands, UniqueIdentifier>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<ExampleState>,
                                outputs: List<ExampleState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.Agree>()
                requireThat {
                    "no inputs are consumes on agree" by (inputs.size == 0)
                    "one output state is created" by (outputs.size == 1)
                    val out = outputs.single()
                    "the buyer is not the seller" by (out.buyer != out.seller)
                    "the participants and parties are the same" by (out.parties.map { it.owningKey }.containsAll(out.participants))
                    "the buyer and seller are the parties" by (out.parties.containsAll(listOf(out.buyer, out.seller)))
                    "my model data exists" by (out.swap.data != "") // This is where to validate your agreement
                }

                return setOf(command.value)
            }
        }
    }
}