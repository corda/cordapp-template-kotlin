package com.template.types

import com.r3.corda.lib.tokens.contracts.types.TokenType

data class ExampleFixedTokenType(
        override val tokenIdentifier: String,
        override val fractionDigits: Int = 0
) : TokenType(tokenIdentifier, fractionDigits) {
    override fun toString(): String = tokenIdentifier

    companion object {
        private val registry = mapOf(
                Pair("WIBBLE", ExampleFixedTokenType("WIBBLE", 2)),
                Pair("WOBBLE", ExampleFixedTokenType("WIBBLE", 5))
        )

        fun getInstance(code: String): ExampleFixedTokenType {
            return registry[code] ?: throw IllegalArgumentException("$code doesn't exist.")
        }
    }
}