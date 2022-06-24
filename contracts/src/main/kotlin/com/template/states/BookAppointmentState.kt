package com.template.states

import com.template.contracts.AppointmentContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.LocalDateTime


// *********
// * State *
// *********

@BelongsToContract(AppointmentContract::class)
@CordaSerializable
data class BookingAppointment(
        val description: String,
        val sender: Party,
        val receiver: Party,
        val date: LocalDateTime,
        override val participants: List<AbstractParty> = listOf(sender, receiver)
) : ContractState