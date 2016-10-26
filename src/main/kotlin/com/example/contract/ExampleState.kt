package com.example.contract

import com.example.model.ExampleModel
import com.r3corda.core.contracts.Command
import com.r3corda.core.contracts.DealState
import com.r3corda.core.contracts.TransactionType
import com.r3corda.core.contracts.UniqueIdentifier
import com.r3corda.core.crypto.Party
import com.r3corda.core.transactions.TransactionBuilder
import java.security.PublicKey
import java.util.*


data class ExampleState(val swap: ExampleModel,
                        val buyer: Party,
                        val seller: Party,
                        override val contract: ExampleContract,
                        override val linearId: UniqueIdentifier = UniqueIdentifier(swap.swapRef)): DealState {
    override val ref: String = linearId.externalId!! // Same as the constructor for UniqueIdentified
    override val parties: List<Party> get() = listOf(buyer, seller)

    override fun isRelevant(ourKeys: Set<PublicKey>): Boolean {
        return !Collections.disjoint(ourKeys, parties.map { it.owningKey })
    }

    override fun generateAgreement(notary: Party): TransactionBuilder {
        val state = ExampleState(swap, buyer, seller, ExampleContract())
        return TransactionType.General.Builder(notary).withItems(state, Command(ExampleContract.Commands.Agree(), parties.map { it.owningKey }))
    }

    override val participants: List<PublicKey> = parties.map { it.owningKey }
}