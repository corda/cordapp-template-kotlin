package com.template

import com.r3.corda.sdk.token.contracts.EvolvableTokenContract
import net.corda.core.transactions.LedgerTransaction

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
class ExampleEvolvableTokenTypeContract : EvolvableTokenContract() {
    override fun additionalCreateChecks(tx: LedgerTransaction) = Unit
    override fun additionalUpdateChecks(tx: LedgerTransaction) = Unit
}