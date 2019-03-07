package com.template.states

import com.r3.corda.sdk.token.contracts.states.EvolvableTokenType
import com.template.ExampleEvolvableTokenTypeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.math.BigDecimal

@BelongsToContract(ExampleEvolvableTokenTypeContract::class)
class ExampleEvolvableTokenType(
        val importantInformationThatMayChange: String,
        override val maintainers: List<Party>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType() {
    companion object {
        // Used to identify our contract when building a transaction.
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override val displayTokenSize: BigDecimal = BigDecimal.ONE
}