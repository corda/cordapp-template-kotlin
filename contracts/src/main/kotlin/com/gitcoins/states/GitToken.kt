package com.gitcoins.states

import com.r3.corda.sdk.token.contracts.types.FixedTokenType
import java.math.BigDecimal

/**
 * A [GitToken] is a [FixedTokenType] that is designed to be issued through push and pull request review events
 * to a GitHub repository.
 */
data class GitToken(
        override val displayTokenSize: BigDecimal = BigDecimal.ONE,
        override val tokenClass: String = "com.gitcoins.states.GitToken",
        override val tokenIdentifier: String = "GIT"
    ) : FixedTokenType()
