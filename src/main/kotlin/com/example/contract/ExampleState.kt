package com.example.contract

import com.example.model.ExampleModel
import net.corda.core.contracts.Command
import net.corda.core.contracts.DealState
import net.corda.core.contracts.TransactionType
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Party
import net.corda.core.crypto.PublicKeyTree
import net.corda.core.transactions.TransactionBuilder
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
        val partyKeys = parties.flatMap { it.owningKey.keys }
        return ourKeys.intersect(partyKeys).isNotEmpty()
    }

    override fun generateAgreement(notary: Party): TransactionBuilder {
        val state = ExampleState(swap, buyer, seller, ExampleContract())
        return TransactionType.General.Builder(notary).withItems(state, Command(ExampleContract.Commands.Agree(), parties.map { it.owningKey }))
    }

    override val participants: List<PublicKeyTree> = parties.map { it.owningKey }
}
