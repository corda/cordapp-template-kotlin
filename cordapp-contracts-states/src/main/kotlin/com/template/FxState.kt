package com.template

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.util.*

data class FxState(val currencyOne: Amount<Currency>,
                   val currencyTwo: Amount<Currency>,
                   val buyer: Party,
                   val seller: Party,
                   val spotRate: BigDecimal) : ContractState {

    override val participants: List<AbstractParty> get() = listOf(buyer, seller)

    override fun toString() = " ${this.currencyTwo.quantity} ${this.currencyOne.token.currencyCode} sold by ${this.seller} to ${this.buyer} using ${this.currencyOne.quantity} ${this.currencyOne.token.currencyCode} at a rate of ${this.spotRate}"
}