package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.ExampleEvolvableTokenTypeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(ExampleEvolvableTokenTypeContract::class)
class ExampleEvolvableTokenType(
        val importantInformationThatMayChange: String,
        val maintainer: Party,
        override val linearId: UniqueIdentifier,
        override val fractionDigits: Int = 0
) : EvolvableTokenType() {
    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override val maintainers: List<Party> get() = listOf(maintainer)
}