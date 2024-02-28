package com.r3.developers.cordapptemplate.utxoexample.workflows

import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract
import com.r3.developers.cordapptemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.StateRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*


data class TestContractFlowArgs(val otherMember: String)

class TestContractFlow: ClientStartableFlow  {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {


        val results = mutableMapOf<String, String>()

        log.info("TestContractFlow.call() called")

        class FakeCommand : Command

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TestContractFlowArgs::class.java)

            val myInfo = memberLookup.myInfo()

            val otherMember = memberLookup.lookup(MemberX500Name.parse(flowArgs.otherMember)) ?:
            throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")

            // Obtain the Notary name and public key.
            val notary = notaryLookup.notaryServices.first()

            // Create a well formed transaction with an output State which can be referenced
            // as an input StateRef in the tests
            lateinit var inputStateRef: StateRef
            lateinit var chatId: UUID

            try {
                val chatState = ChatState(
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                chatId = chatState.id

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Create())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                inputStateRef = StateRef(signedTransaction.id, 0)
                flowEngine.subFlow(FinalizeChatSubFlow(signedTransaction, otherMember.name))

            } catch (e:Exception) {
                throw CordaRuntimeException("Set up transaction could not be created because of exception: ${e.message}")
            }




            // *************   START TESTS ****************

            // Multiple Commands not permitted
            results["Multiple Commands not permitted"] = try {
                val chatState = ChatState(
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Create())
                    .addCommand(FakeCommand())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("Requires a single command.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // ChatState with 3 Participants not permitted
            results["ChatState with 3 Participants not permitted"]  = try {

                val chatState = ChatState(
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Create())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("The output state should have two and only two participants.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // Input State on Create not permitted
            results["Input State on Create not permitted"] = try {
                val chatState = ChatState(
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(inputStateRef)
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Create())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Create there should be no input states.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }

            // Zero output States on Create not permitted

                // Test omitted as it would fail on
                // "The output state should have two and only two participants." first


            // Two output States on Create not permitted
            results["Two output States on Create not permitted"] = try {
                val chatState = ChatState(
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(chatState)
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Create())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"
            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Create there should be one and only one output state.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // Zero input State on Update not permitted
            results["Zero input State on Update not permitted"] = try {

                val chatState = ChatState(
                    id = chatId,
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Update())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Update there should be one and only one input state.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // Two Input State on Update not permitted
            results["Two Input State on Update not permitted"] = try {

                log.info("MB: test change")
                val chatState = ChatState(
                    id = chatId,
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(inputStateRef)
                    .addInputState(inputStateRef)
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Update())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Update there should be one and only one input state.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // Zero output States on Update not permitted

                // Test omitted as it would fail on
                // "The output state should have two and only two participants." first


            // Two output States on Update not permitted
            results["Two output States on Update not permitted"] = try {
                val chatState = ChatState(
                    id = chatId,
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(inputStateRef)
                    .addOutputState(chatState)
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Update())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"
            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Update there should be one and only one output state.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // On Update id must not change
            results["On Update id must not change"] = try {
                val chatState = ChatState(
                    id = UUID.randomUUID(),
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(inputStateRef)
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Update())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"
            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Update id must not change")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // On Update chatName must not change
            results["On Update chatName must not change"] = try {
                val chatState = ChatState(
                    id = chatId,
                    chatName = "DummyChat Name has changed",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(inputStateRef)
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Update())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"
            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Update chatName must not change.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // On Update participants must not change
            results["On Update participants must not change"] = try {
                val chatState = ChatState(
                    id = chatId,
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), myInfo.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addInputState(inputStateRef)
                    .addOutputState(chatState)
                    .addCommand(ChatContract.Update())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"
            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("When command is Update participants must not change.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // FakeCommand not permitted
            results["FakeCommand not permitted"] = try {
                val chatState = ChatState(
                    chatName = "DummyChat",
                    messageFrom = myInfo.name,
                    message = "Dummy Message",
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )


                // Use UTXOTransactionBuilder to build up the draft transaction.
                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(chatState)
                    .addCommand(FakeCommand())
                    .addSignatories(chatState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("Command not allowed.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }



            return results.toString()

            // Catch any exceptions, log them and rethrow the exception.
        } catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }

}
/*
{
    "clientRequestId": "dummy-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.TestContractFlow",
    "requestBody": {
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
    }
}

 */