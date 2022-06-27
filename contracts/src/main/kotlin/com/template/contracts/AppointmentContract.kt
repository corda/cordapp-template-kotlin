package com.template.contracts

import com.template.states.AppointmentState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory

// ************
// * Contract *
// ************
class AppointmentContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.AppointmentContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.

    override fun verify(tx: LedgerTransaction) {

        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {

            is Commands.CreateAppointment -> requireThat {
                val output = tx.outputsOfType<AppointmentState>().first()
                "No inputs should be consumed when requesting available dates an appointment.".using(tx.inputStates.isEmpty())
                "The description should not be empty.".using(output.description.length > 0)
                "An output state should be generated".using(!tx.outputStates.isEmpty())
            }

            is Commands.BookAppointment -> requireThat {
                "Transaction should have input.".using(!tx.inputStates.isEmpty())
            }
            else -> {
                throw IllegalArgumentException("Command is not available.")
            }
        }
    }

    // Used to indicate the transaction's intent.
    sealed class Commands : CommandData {
        class CreateAppointment : Commands()
        class BookAppointment : Commands()
    }


}