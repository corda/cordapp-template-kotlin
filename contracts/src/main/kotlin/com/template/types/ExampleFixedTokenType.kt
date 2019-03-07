package com.template.types

import com.r3.corda.sdk.token.contracts.types.FixedTokenType
import java.math.BigDecimal

data class ExampleFixedTokenType(
        override val tokenIdentifier: String,
        private val defaultFractionDigits: Int = 0
) : FixedTokenType() {
    override val tokenClass: String get() = javaClass.canonicalName
    override val displayTokenSize: BigDecimal get() = BigDecimal.ONE.scaleByPowerOfTen(-defaultFractionDigits)
    override fun toString(): String = tokenIdentifier

    companion object {
        private val registry = mapOf(
                Pair("WIB", ExampleFixedTokenType("WIBBLE", 2)),
                Pair("WOB", ExampleFixedTokenType("WIBBLE", 5))
        )

        fun getInstance(code: String): ExampleFixedTokenType {
            return registry[code] ?: throw IllegalArgumentException("$code doesn't exist.")
        }
    }
}