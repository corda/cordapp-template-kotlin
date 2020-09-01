package com.template.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ProposalResponseContract::class)
data class ProposalResponse(
        val proposalId: UniqueIdentifier,
        val response: Response,
        val proposer: Party,
        val me: Party
) : ContractState {
    override val participants: List<AbstractParty> = listOf(me, proposer)

    @CordaSerializable
    enum class Response {
        ACCEPT, REJECT
    }
}