package com.r3.developers.cordapptemplate.utxoexample.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.REQUIRE_SINGLE_COMMAND
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS
import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract.Companion.UNKNOWN_COMMAND
import com.r3.developers.cordapptemplate.utxoexample.states.ChatState
import net.corda.v5.ledger.utxo.Command
import org.junit.jupiter.api.Test
import java.util.*

/**
 * This class is an implementation of ContractTest. This provides functions to easily perform unit tests on contracts.
 * This allows us to unit test our implementation of contracts without having to trigger a workflow.
 **/

// This specific class is involved with testing the scenarios involving the ChatState state and the Create command.
class ChatContractCreateCommandTest : ContractTest() {

    // The following are default values for states so that tests can easily refer and re-use them
    private val outputChatStateChatName = "aliceChatName"
    private val outputChatStateChatMessage = "aliceChatMessage"
    internal val outputChatState = ChatState(
        UUID.randomUUID(),
        outputChatStateChatName,
        aliceName,
        outputChatStateChatMessage,
        listOf(aliceKey, bobKey)
    )

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
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants)
        }
        /**
         *  The assertVerifies function is the general way to test if a contract test passes or fails a transaction.
         *  If the transaction is verified, then it means that the contract tests pass.
         **/
        assertVerifies(transaction)
    }

    @Test
    fun missingCommand() {
        // The following test builds a transaction that would fail due to not having a command.
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addSignatories(outputChatState.participants)
        }
        /**
         * The assertFailsWith function is the general way to test for unhappy path test cases contract tests.
         *
         * The transaction defined above will fail because the transaction does not include a command, whilst the contract
         * expected one. So, we expect the transaction to fail, and only 'pass' our test if we can match the error message
         * we expect.
         *
         * NOTE: the assertFailsWith method tests if the exact string of the error message matches the expected message
         *       to test if the string of the error message contains a substring within the error message, use the
         *       assertFailsWithMessageContaining() function using the same arguments.
         **/
        assertFailsWith(transaction, REQUIRE_SINGLE_COMMAND)
    }

    @Test
    fun shouldNotAcceptUnknownCommand() {
        // The following test builds a transaction that would fail due to providing an invalid command.
        class MyDummyCommand : Command

        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(MyDummyCommand())
            addSignatories(outputChatState.participants)
        }

        assertFailsWith(transaction, UNKNOWN_COMMAND)
    }

    @Test
    fun outputStateCannotHaveZeroParticipants() {
        // The following test builds a transaction that would fail due to not providing participants, when the contract
        // expects exactly two participants.
        val state = ChatState(
            UUID.randomUUID(),
            "myChatName",
            aliceName,
            "myChatMessage",
            emptyList()
        )
        val transaction = buildTransaction {
            addOutputState(state)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS")
    }

    @Test
    fun outputStateCannotHaveOneParticipant() {
        // The following test builds a transaction that would fail due to not providing the right number of participants.
        // This test provides a list of only one participant, when the contract expects exactly two participants.
        val state = ChatState(
            UUID.randomUUID(),
            "myChatName",
            aliceName,
            "myChatMessage",
            listOf(aliceKey)
        )
        val transaction = buildTransaction {
            addOutputState(state)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS")
    }

    @Test
    fun outputStateCannotHaveThreeParticipants() {
        // The following test builds a transaction that would fail due to not providing the right number of participants.
        // This test provides a list of three participants, when the contract expects exactly two participants.
        val state = ChatState(
            UUID.randomUUID(),
            "myChatName",
            aliceName,
            "myChatMessage",
            listOf(aliceKey, bobKey, charlieKey)
        )
        val transaction = buildTransaction {
            addOutputState(state)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS")
    }

    @Test
    fun shouldBeSigned() {
        // The following test builds a transaction that would fail due to not signing the transaction.
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS")
    }

    @Test
    fun cannotBeSignedByOnlyOneParticipant() {
        // The following test builds a transaction that would fail due to being signed by only one participant and not
        // all participants.
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants[0])
        }
        assertFailsWith(transaction, "Failed requirement: $TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS")
    }

    @Test
    fun shouldNotIncludeInputState() {
        // The following test builds a transaction that would fail due to providing an input state when the contract did
        // not expect one
        happyPath() // generate an existing state to search for
        val existingState = ledgerService.findUnconsumedStatesByType(ChatState::class.java).first() // doesn't matter which as this will fail validation
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES")
    }

    @Test
    fun shouldNotHaveTwoOutputStates() {
        // The following test builds a transaction that would fail due to providing two output states when the contract
        // only
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE")
    }
}