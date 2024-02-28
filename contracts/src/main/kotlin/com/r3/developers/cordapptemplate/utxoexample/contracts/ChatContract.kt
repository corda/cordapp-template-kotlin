package com.r3.developers.cordapptemplate.utxoexample.contracts

import com.r3.developers.cordapptemplate.utxoexample.states.ChatState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class ChatContract: Contract {

    // Use an internal scoped constant to hold the error messages
    // This allows the tests to use them, meaning if they are updated you won't need to fix tests just because the wording was updated
    internal companion object {

        const val REQUIRE_SINGLE_COMMAND = "Requires a single command."
        const val UNKNOWN_COMMAND = "Command not allowed."
        const val OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS = "The output state should have two and only two participants."
        const val TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS = "The transaction should have been signed by both participants."

        const val CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES = "When command is Create there should be no input states."
        const val CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE =  "When command is Create there should be one and only one output state."

        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Update there should be one and only one input state."
        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Update there should be one and only one output state."
        const val UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Update id must not change."
        const val UPDATE_COMMAND_CHATNAME_SHOULD_NOT_CHANGE = "When command is Update chatName must not change."
        const val UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Update participants must not change."
    }

    // Command Class used to indicate that the transaction should start a new chat.
    class Create: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Update: Command

    // verify() function is used to apply contract rules to the transaction.
    override fun verify(transaction: UtxoLedgerTransaction) {

        // Ensures that there is only one command in the transaction
        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)

        // Applies a universal constraint (applies to all transactions irrespective of command)
        OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as ChatState
            output.participants.size== 2
        }

        TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as ChatState
            transaction.signatories.containsAll(output.participants)
        }

        // Switches case based on the command
        when(command) {
            // Rules applied only to transactions with the Create Command.
            is Create -> {
                CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES using (transaction.inputContractStates.isEmpty())
                CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
            }
            // Rules applied only to transactions with the Update Command.
            is Update -> {
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as ChatState
                val output = transaction.outputContractStates.single() as ChatState
                UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                UPDATE_COMMAND_CHATNAME_SHOULD_NOT_CHANGE using (input.chatName == output.chatName)
                UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                        input.participants.toSet().intersect(output.participants.toSet()).size == 2)
            }
            else -> {
                throw CordaRuntimeException(UNKNOWN_COMMAND)
            }
        }
    }

    // Helper function to allow writing constraints in the Corda 4 '"text" using (boolean)' style
    private infix fun String.using(expr: Boolean) {
        if (!expr) throw CordaRuntimeException("Failed requirement: $this")
    }

    // Helper function to allow writing constraints in '"text" using {lambda}' style where the last expression
    // in the lambda is a boolean.
    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed requirement: $this")
    }
}