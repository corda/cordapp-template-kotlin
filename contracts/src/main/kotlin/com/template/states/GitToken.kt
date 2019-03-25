package com.template.states

import com.r3.corda.sdk.token.contracts.types.FixedTokenType
import java.math.BigDecimal

// *********
// * State *
// *********
data class GitToken(
        override val displayTokenSize: BigDecimal = BigDecimal.ONE,
        override val tokenClass: String = "com.template.states.GitToken",
        override val tokenIdentifier: String = "GIT"
    ) : FixedTokenType()
