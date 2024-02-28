package com.r3.developers.apples.states

import com.r3.developers.apples.contracts.AppleStampContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@BelongsToContract(AppleStampContract::class)
class AppleStamp(
    val id: UUID,
    val stampDesc: String,
    val issuer: PublicKey,
    val holder: PublicKey,
    private val participants: List<PublicKey>
) : ContractState {
    override fun getParticipants(): List<PublicKey> = participants

}