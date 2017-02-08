package com.template.state

import com.template.contract.TemplateContract
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.CompositeKey

/**
 * Define your state object here.
 */
class TemplateState(override val contract: TemplateContract): ContractState {
    /** The public keys of the involved parties. */
    override val participants: List<CompositeKey>
        get() = listOf()
}