package com.r3.developers.apples.states

import com.r3.developers.apples.contracts.BasketOfApplesContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

@BelongsToContract(BasketOfApplesContract::class)
class BasketOfApples(
    val description: String,
    val farm: PublicKey,
    val owner: PublicKey,
    val weight: Int,
    private val participants: List<PublicKey>
) : ContractState {
    override fun getParticipants(): List<PublicKey> = participants

    fun changeOwner(buyer: PublicKey): BasketOfApples {
        val participants = listOf(farm, buyer)
        return BasketOfApples(description, farm, buyer, weight, participants)
    }
}