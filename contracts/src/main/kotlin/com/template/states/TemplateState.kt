package com.template.states

import com.r3.corda.sdk.token.contracts.states.EvolvableToken
import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.math.BigDecimal

// *********
// * State *
// *********
@BelongsToContract(TemplateContract::class)
data class SomeToken(
        val stuff: String,
        val maintainer: Party,
        override val displayTokenSize: BigDecimal = BigDecimal.ONE,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableToken() {
    override val maintainers: List<Party> get() = listOf(maintainer)
}