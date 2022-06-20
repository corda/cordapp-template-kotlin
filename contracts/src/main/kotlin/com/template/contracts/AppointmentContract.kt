package com.template.contracts

import com.template.states.AppointmentState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

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
        val command = tx.getCommand<Commands>(0)

        val output = tx.outputsOfType<AppointmentState>().first()


        when (command.value) {

            is Commands.CreateAppointment -> requireThat {
                print("input state is ${output}")
                "The description should not be empty.".using(output.description.length > 0)
            }
            is Commands.RequestAvailability -> requireThat {
                print("input state is ${tx.inputStates}")
                "No inputs should be consumed when requesting available dates an appointment.".using(tx.inputStates.isEmpty())
            }
            else -> {
                throw IllegalArgumentException("Command is not available.")
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class CreateAppointment : Commands
        class RequestAvailability : Commands
    }
}