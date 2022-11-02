package com.template.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.contracts.TransferContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty

@BelongsToContract(TransferContract::class)
class TransferState(
        val amount: Amount<TokenType>,
        val recipient: AnonymousParty,
        val sender: AnonymousParty) : ContractState {
    override val participants: List<AbstractParty> get() = listOfNotNull(recipient,sender).map { it }
}