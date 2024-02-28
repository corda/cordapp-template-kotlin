package com.r3.developers.cordapptemplate.utxoexample.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_CHATNAME_SHOULD_NOT_CHANGE
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.cordapptemplate.utxoexample.states.ChatState
import net.corda.v5.ledger.utxo.StateAndRef
import org.junit.jupiter.api.Test
import java.util.*

/**
 * This class is an implementation of ContractTest. This provides functions to easily perform unit tests on contracts.
 * This allows us to unit test our implementation of contracts without having to trigger a workflow.
 **/

// This specific class is involved with testing the scenarios involving the ChatState state and the Update command.
class ChatContractUpdateCommandTest : ContractTest() {

    // The following is a helper function to create a ChatState with default values.
    // This is done so that we can easily re-use this block of code when writing our tests that require an input state
    @Suppress("UNCHECKED_CAST")
    private fun createInitialChatState(): StateAndRef<ChatState> {
        val outputState = ChatContractCreateCommandTest().outputChatState
        val transaction = buildTransaction {
            addOutputState(outputState)
            addCommand(ChatContract.Create())
            addSignatories(outputState.participants)
        }
        transaction.toLedgerTransaction()
        return transaction.outputStateAndRefs.first() as StateAndRef<ChatState>
    }

    /**
     * All Tests must start with the @Test annotation. Tests can be run individually by running them with your IDE.
     * Alternatively, tests can be grouped up and tested by running the test from the line defining the class above.
     * If you need help to write tests, think of a happy path scenario and then think of every line of code in the contract
     * where the transaction could fail.
     * It helps to meaningfully name tests so that you know exactly what success case or specific error you are testing for.
     **/

    @Test
    fun happyPath() {
        // The following test builds a transaction that should pass all the contract verification checks.
        // The buildTransaction function helps create a utxoLedgerTransaction that can be referenced for contract tests
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants)
        }
        /**
         *  The assertVerifies function is the general way to test if a contract test passes or fails a transaction.
         *  If the transaction is verified, then it means that the contract tests pass.
         **/
        assertVerifies(transaction)
    }

    @Test
    fun shouldHaveOneInputState(){
        // The following test builds a transaction that would fail due to not providing a input state, when the contract
        // expects exactly one input state
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants)
        }
        /**
         * The assertFailsWith function is the general way to test for unhappy path test cases contract tests.
         *
         * NOTE: the assertFailsWith method tests if the exact string of the error message matches the expected message
         *       to test if the string of the error message contains a substring within the error message, use the
         *       assertFailsWithMessageContaining() function using the same arguments.
         **/
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE")
    }

    @Test
    fun shouldNotHaveTwoInputStates(){
        // The following test builds a transaction that would fail due to having two input states, when the contract
        // expects exactly one.
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE")
    }

    @Test
    fun shouldNotHaveTwoOutputStates(){
        // The following test builds a transaction that would fail due to having two output states, when the contract
        // expects exactly one.
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE")
    }

    @Test
    fun idShouldNotChange(){
        // The following test builds a transaction that would fail because the contract makes sure that the id of the
        // output state does not change from the input state.
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState
            .updateMessage(bobName, "bobResponse")
            .copy(id = UUID.randomUUID())
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE")
    }

    @Test
    fun chatNameShouldNotChange(){
        // The following test builds a transaction that would fail because the contract makes sure that the chatName of
        // the output state does not change from the chat name from the input state.
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState
            .updateMessage(bobName, "bobResponse")
            .copy(chatName = "newName")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_CHATNAME_SHOULD_NOT_CHANGE")
    }

    @Test
    fun participantsShouldNotChange(){
        // The following test builds a transaction that would fail because the contract makes sure that the list of
        // participants from the output state does not change from the list of participants from the input state.
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState
            .updateMessage(bobName, "bobResponse")
            .copy(participants = listOf(bobKey, charlieKey)) //  The input state lists 'Alice' and 'Bob' as the participants
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE")
    }

    @Test
    fun outputStateMustBeSigned() {
        // The following test builds a transaction that would fail because it does not include signatories, where the
        // contract expects all the participants to be signatories.
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertFailsWith(transaction, "Failed requirement: $TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS")
    }

    @Test
    fun outputStateCannotBeSignedByOnlyOneParticipant() {
        // The following test builds a transaction that would fail because it only includes one signatory, where the
        // contract expects all the participants to be signatories.
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
            addSignatories(updatedOutputChatState.participants[0])
        }
        assertFailsWith(transaction, "Failed requirement: $TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS")
    }
}