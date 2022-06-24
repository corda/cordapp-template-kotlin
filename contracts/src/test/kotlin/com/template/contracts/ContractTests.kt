package com.template.contracts

import com.template.states.AppointmentState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.LocalDateTime

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.template"))
    var patient = TestIdentity(CordaX500Name("Patient", "London", "US"))
    var doctor = TestIdentity(CordaX500Name("Doctor", "London", "US"))

    @Test
    fun dummytest() {
        val state = AppointmentState("Appointment Description", patient.party, doctor.party, LocalDateTime.now())

        ledgerServices.ledger {
            // Should fail
          /*  transaction {
                //failing transaction
                input(AppointmentContract.ID, state)
                output(AppointmentContract.ID, state)
                command(patient.publicKey, AppointmentContract.Commands.RequestAvailability())
                fails()
            } */

            // Should fail
            transaction {
                //failing transaction
                input(AppointmentContract.ID, state)
                output(AppointmentContract.ID, state)
                command(patient.publicKey, AppointmentContract.Commands.CreateAppointment())
                fails()
            }

            //pass
            transaction {
                //passing transaction
                output(AppointmentContract.ID, state)
                command(patient.publicKey, AppointmentContract.Commands.CreateAppointment())
                verifies()
            }
        }
    }
}