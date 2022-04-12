package com.template.states

import com.template.contracts.IOUContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*


@BelongsToContract(IOUContract::class)
data class IOUState(
    val msg: String,
    val amount: Amount<Currency>,
    val paid: Amount<Currency>,
    val lender: Party,
    val borrower: Party,
    override val participants: List<AbstractParty> = listOf(lender, borrower),
    override val linearId: UniqueIdentifier
) : ContractState, LinearState