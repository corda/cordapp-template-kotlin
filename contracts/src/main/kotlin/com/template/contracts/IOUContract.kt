package com.template.contracts

import com.template.states.IOUState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


class IOUContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.IOUContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Issue -> {
                requireThat {
                    "There should no input states for issuance".using(tx.inputStates.isEmpty())
                    "There should an output".using(tx.outputStates.count() == 1)
                }

                val outputState = tx.outputsOfType<IOUState>().first()
                requireThat {
                    "The output state amount must be bigger than 0".using(outputState.amount.quantity > 0)
                }

                requireThat {
                    "The lender and borrower cannot be the same.".using(
                        outputState.borrower.owningKey != outputState.lender.owningKey
                    )
                }
            }
            is Commands.Transfer -> {
                requireThat {
                    "There must be a single input state.".using(tx.inputStates.count() == 1)
                    "There must be a single output state.".using(tx.outputStates.count() == 1)
                }

                val signers = tx.commands.first().signers.toSet()
                val participants = tx.outputStates.first().participants.toMutableList()
                participants.add(tx.inputStates.first().participants.first())

                requireThat {
                    "There must be three participants namely lender, new lender and borrower.".using(participants.count() == 3)
                }

                val participantsKey = mutableSetOf<PublicKey>()
                for (participant in participants) {
                    participantsKey.add(participant.owningKey)
                }

                requireThat {
                    "There must be two signers".using(signers.count() == 2)
                    "Signers must be among participants.".using(participantsKey.containsAll(signers))
                }

                val inputState = tx.inputsOfType<IOUState>().first()
                val outputState = tx.outputsOfType<IOUState>().first()
                // TODO: why this middle man is required?
                val checkOutputState = IOUState(
                    msg = "test state",
                    amount = outputState.amount,
                    paid = outputState.paid,
                    lender = outputState.lender,
                    borrower = outputState.borrower,
                    linearId = outputState.linearId
                )

                requireThat {
                    "Amount should not change after transfer: ${inputState.amount} <> ${checkOutputState.amount}".using(
                        checkOutputState.amount == inputState.amount
                    )
                    "Paid sum should not change after transfer: ${inputState.paid} <> ${checkOutputState.paid}".using(
                        checkOutputState.paid == inputState.paid
                    )
                    "LinearId should not change after transfer".using(checkOutputState.linearId == inputState.linearId)
                    "Borrower should not change after transfer".using(checkOutputState.borrower == inputState.borrower)
                }
            }
            is Commands.Settle -> {
                requireThat { "There should be only one input state.".using(tx.inputStates.count() == 1) }

                val inputIOU = tx.inputsOfType<IOUState>().first()
                val inputAmount = inputIOU.amount
                requireThat { "There should be zero or one output state.".using(tx.outputStates.count() <= 1) }
                if (tx.outputStates.count() == 1) {
                    val outputIOU = tx.outputsOfType<IOUState>().first()
                    requireThat {
                        "Only the paid amount can change during part settlement.".using(
                            outputIOU.amount == inputAmount
                        )
                        "The paid amount must increase in case of part settlement of the IOU.".using(
                            outputIOU.paid > inputIOU.paid
                        )
                        "The paid amount must be less than the total IOU.".using(
                            outputIOU.paid < inputIOU.amount
                        )
                    }
                }
                // Both lender and borrower must have signed the transaction.
                val listOfParticipantPublicKeys = inputIOU.participants.map { it -> it.owningKey }.toSet()
                val signers = tx.commands.first().signers.toSet()
                requireThat {
                    "Both lender and borrower must have signed the transaction.".using(
                        listOfParticipantPublicKeys == signers
                    )
                }
            }
            else -> {
                throw IllegalArgumentException("Unrecognised command type: ${command.value}")
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Transfer : Commands
        class Settle : Commands
    }
}