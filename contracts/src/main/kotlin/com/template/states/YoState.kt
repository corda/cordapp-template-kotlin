package net.corda.examples.yo.states

import net.corda.examples.yo.contracts.YoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(YoContract::class)
data class YoState(val origin: Party,
                   val target: Party,
                   val yo: String = "Yo!") : ContractState {
    override val participants = listOf(target)
    override fun toString() = "${origin.name}: $yo"
}
