package com.r3.developers.cordapptemplate.utxoexample.workflows

import com.r3.developers.cordapptemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.util.*

// A class to hold the deserialized arguments required to start the flow.
data class GetChatFlowArgs(val id: UUID, val numberOfRecords: Int)

// A class to pair the messageFrom and message together.
data class MessageAndSender(val messageFrom: String, val message: String)

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class GetChatFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("GetChatFlow.call() called")

        // Obtain the deserialized input arguments to the flow from the requestBody.
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetChatFlowArgs::class.java)

        // Look up the latest unconsumed ChatState with the given id.
        // Note, this code brings all unconsumed states back, then filters them.
        // This is an inefficient way to perform this operation when there are a large number of chats.
        // Note, you will get this error if you input an id which has no corresponding ChatState (common error).
        val states = ledgerService.findUnconsumedStatesByType(ChatState::class.java)
        val state = states.singleOrNull {it.state.contractState.id == flowArgs.id}
            ?: throw CordaRuntimeException("Did not find an unique unconsumed ChatState with id ${flowArgs.id}")

        // Calls resolveMessagesFromBackchain() which retrieves the chat history from the backchain.
        return jsonMarshallingService.format(resolveMessagesFromBackchain(state, flowArgs.numberOfRecords ))
    }

    // resoveMessageFromBackchain() starts at the stateAndRef provided, which represents the unconsumed head of the
    // backchain for this particular chat, then walks the chain backwards for the number of transaction specified in
    // the numberOfRecords argument. For each transaction it adds the MessageAndSender representing the
    // message and who sent it to a list which is then returned.
    @Suspendable
    private fun resolveMessagesFromBackchain(stateAndRef: StateAndRef<ChatState>, numberOfRecords: Int): List<MessageAndSender>{

        // Set up a MutableList to collect the MessageAndSender(s)
        val messages = mutableListOf<MessageAndSender>()

        // Set up initial conditions for walking the backchain.
        var currentStateAndRef = stateAndRef
        var recordsToFetch = numberOfRecords
        var moreBackchain = true

        // Continue to loop until the start of the backchain or enough records have been retrieved.
        while (moreBackchain) {

            // Obtain the transaction id from the current StateAndRef and fetch the transaction from the vault.
            val transactionId = currentStateAndRef.ref.transactionId
            val transaction = ledgerService.findLedgerTransaction(transactionId)
                ?: throw CordaRuntimeException("Transaction $transactionId not found.")

            // Get the output state from the transaction and use it to create a MessageAndSender Object which
            // is appended to the mutable list.
            val output = transaction.getOutputStates(ChatState::class.java).singleOrNull()
                ?: throw CordaRuntimeException("Expecting one and only one ChatState output for transaction $transactionId.")
            messages.add(MessageAndSender(output.messageFrom.toString(), output.message))
            // Decrement the number of records to fetch.
            recordsToFetch--

            // Get the reference to the input states.
            val inputStateAndRefs = transaction.inputStateAndRefs

            // Check if there are no more input states (start of chain) or we have retrieved enough records.
            // Check the transaction is not malformed by having too many input states.
            // Set the currentStateAndRef to the input StateAndRef, then repeat the loop.
            if (inputStateAndRefs.isEmpty() || recordsToFetch == 0) {
                moreBackchain = false
            } else if (inputStateAndRefs.size > 1) {
                throw CordaRuntimeException("More than one input state found for transaction $transactionId.")
            } else {
                @Suppress("UNCHECKED_CAST")
                currentStateAndRef = inputStateAndRefs.single() as StateAndRef<ChatState>
            }
        }
     // Convert to an immutable List.
     return messages.toList()
    }
}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "get-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.GetChatFlow",
    "requestBody": {
        "id":"** fill in id **",
        "numberOfRecords":"4"
    }
}
 */