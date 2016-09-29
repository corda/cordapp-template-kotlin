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

                require(inputs.size == 0)
                require(outputs.size == 1)
                require(outputs[0].buyer != outputs[0].seller)
                require(outputs[0].parties.map { it.owningKey }.containsAll(outputs[0].participants))
                require(outputs[0].parties.containsAll(listOf(outputs[0].buyer, outputs[0].seller)))
                require(outputs[0].swap.data != "") // This is where to validate your agreement

                return setOf(command.value)
            }
        }
    }
}