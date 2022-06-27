package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AppointmentContract
import com.template.states.AppointmentState
import com.template.states.BookingAppointment
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.stream.Collectors


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class RequestAvailability(private val patient: Party) : FlowLogic<List<LocalDateTime>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): List<LocalDateTime> {

        val counterpartySession = initiateFlow(patient)
        val dates = counterpartySession.sendAndReceive<List<LocalDateTime>>("Give me the available dates")

        return dates.unwrap { it }
    }
}

@InitiatedBy(RequestAvailability::class)
class ProvideAvailability(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val logger = LoggerFactory.getLogger(ProvideAvailability::class.java)
        logger.info("Sending back to the patient the availability of appointment dates")

        val dates = listOf<LocalDateTime>(LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3))
        logger.info("The date of availability : ${dates}")

        counterpartySession.send(dates)
    }
}


@StartableByRPC
@InitiatingFlow
class BookAppointmentRequest(private val doctor: Party, private val appDate: LocalDateTime, private val appointmentReason: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val output = AppointmentState(description = appointmentReason, sender = ourIdentity, receiver = doctor, date = appDate, participants = listOf(ourIdentity, doctor))

        // Step 3. Create a new TransactionBuilder object.
        val builder = TransactionBuilder(notary)
                .addCommand(AppointmentContract.Commands.CreateAppointment(), listOf(ourIdentity.owningKey, doctor.owningKey))
                .addOutputState(output)


        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> = output.participants.stream().map { el: AbstractParty? -> el as Party? }.collect(Collectors.toList())
        otherParties.remove(ourIdentity)

        val sessions = otherParties.stream().map { el: Party? ->
            initiateFlow(el!!)
        }.collect(Collectors.toList())

        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(BookAppointmentRequest::class)
class BookAppointmentAnswer(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val logger = LoggerFactory.getLogger(BookAppointmentAnswer::class.java)
        logger.info("Last step arrived")

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an AppointmentState transaction" using (output is AppointmentState)
            }
        }
        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}


@InitiatingFlow
@StartableByRPC
class DecideAppointmentAnswer(val patient: Party, val stateRef: StateRef, val acceptBookingAppointment: Boolean) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val logger = LoggerFactory.getLogger(DecideAppointmentAnswer::class.java)
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val inputCriteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
        val input = serviceHub.vaultService.queryBy( AppointmentState::class.java, inputCriteria).states.filter { state-> state.ref == stateRef }

        logger.info("input is ${input}")

        val output = BookingAppointment(description= "Appointment booked", sender = ourIdentity , receiver = patient, date = input.first().state.data.date, participants = listOf(ourIdentity, patient))
        val builder = TransactionBuilder(notary)
                .addInputState(input.first())
                .addCommand(AppointmentContract.Commands.BookAppointment(), listOf(ourIdentity.owningKey, patient.owningKey))


        if(acceptBookingAppointment){
            builder.addOutputState(output)
        }

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> = output.participants.stream().map { el: AbstractParty? -> el as Party? }.collect(Collectors.toList())
        otherParties.remove(ourIdentity)

        val sessions = otherParties.stream().map { el: Party? ->
            initiateFlow(el!!)
        }.collect(Collectors.toList())

        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        return subFlow(FinalityFlow(stx, sessions))
    }
}


@InitiatedBy(DecideAppointmentAnswer::class)
class AppointmentAnswerVerify(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val logger = LoggerFactory.getLogger(AppointmentAnswerVerify::class.java)
        logger.info("Last step arrived2 ")

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an AppointmentState transaction" using (output is BookingAppointment)
            }
        }
        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

