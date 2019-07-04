package com.template.states

import com.template.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(TokenContract::class)
data class TokenState(val creator: Party, val owner: Party, val description: String, val ownerCount: Int,
                      override val linearId: UniqueIdentifier = UniqueIdentifier(), override val participants: List<AbstractParty> = listOf(creator, owner)) : LinearState