package com.r3.developers.cordapptemplate.utxoexample.states

import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*


// The ChatState represents data stored on ledger. A chat consists of a linear series of messages between two
// participants and is represented by a UUID. Any given pair of participants can have multiple chats
// Each ChatState stores one message between the two participants in the chat. The backchain of ChatStates
// represents the history of the chat.

@BelongsToContract(ChatContract::class)
data class ChatState(
    // Unique identifier for the chat.
    val id : UUID = UUID.randomUUID(),
    // Non-unique name for the chat.
    val chatName: String,
    // The MemberX500Name of the participant who sent the message.
    val messageFrom: MemberX500Name,
    // The message
    val message: String,
    // The participants to the chat, represented by their public key.
    private val participants: List<PublicKey>) : ContractState {

    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    // Helper function to create a new ChatState from the previous (input) ChatState.
    fun updateMessage(messageFrom: MemberX500Name, message: String) =
        copy(messageFrom = messageFrom, message = message)
}

