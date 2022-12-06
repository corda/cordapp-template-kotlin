package com.template.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import com.template.states.SensitiveState

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.template"))
    var alice = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var bob = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))

    @Test
    fun dummytest() {
        val state = SensitiveState("Hello-World", "hash", "msg", alice.party, bob.party)
        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                //failing transaction
                input(SensitiveFlowContract.ID, state)
                output(SensitiveFlowContract.ID, state)
                command(alice.publicKey, SensitiveFlowContract.Commands.Create())
                fails()
            }
            //pass
            transaction {
                //passing transaction
                output(SensitiveFlowContract.ID, state)
                command(alice.publicKey, SensitiveFlowContract.Commands.Create())
                verifies()
            }
        }
    }
}
