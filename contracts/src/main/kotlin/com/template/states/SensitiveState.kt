package com.template.states

import com.template.contracts.SensitiveFlowContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(SensitiveFlowContract::class)
data class SensitiveState(
    val secretMsg: String,
    val secretMsgHash: String = "",
    val msg: String,
    val sender: Party,
    val receiver: Party,
    override val participants: List<AbstractParty> = listOf(sender, receiver)
) : ContractState
