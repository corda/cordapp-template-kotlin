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
    var alice = TestIdentity(CordaX500Name("Patient", "London", "US"))
    var bob = TestIdentity(CordaX500Name("Doctor", "London", "US"))

    @Test
    fun dummytest() {
        val state = AppointmentState("Appointment Description", alice.party, bob.party, LocalDateTime.now() )

        ledgerServices.ledger {
            // Should fail no input should be used when requesting available dates
            transaction {
                //failing transaction
                input(AppointmentContract.ID, state)
                output(AppointmentContract.ID, state)
                command(alice.publicKey, AppointmentContract.Commands.RequestAvailability())
                fails()
            }

            // Should fail bid price is equal to previous highest bid
            transaction {
                //failing transaction
                input(AppointmentContract.ID, state)
                output(AppointmentContract.ID, state)
                command(alice.publicKey, AppointmentContract.Commands.CreateAppointment())
                fails()
            }



//            //pass
//            transaction {
//                //passing transaction
//                output(AppointmentContract.ID, state)
//                command(alice.publicKey, AppointmentContract.Commands.CreateAppointment())
//                verifies()
//            }
        }
    }
}