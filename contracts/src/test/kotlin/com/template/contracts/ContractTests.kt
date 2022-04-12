package com.template.contracts

import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.template"))
    var alice = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var bank = TestIdentity(CordaX500Name("Bank", "TestLand", "US"))
    var bob = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))


    @Test
    fun `test settle command`() {
        val linearId = UniqueIdentifier("test id", UUID.randomUUID())

        val inputState = IOUState(
            "input state",
            Amount(100, Currency.getInstance(Locale.UK)),
            Amount(0, Currency.getInstance(Locale.UK)),
            bank.party, bob.party,
            linearId = linearId
        )

        val outputState = IOUState(
            "input state",
            Amount(100, Currency.getInstance(Locale.UK)),
            Amount(40, Currency.getInstance(Locale.UK)),
            bank.party, bob.party,
            linearId = linearId
        )

        ledgerServices.ledger {
            transaction {
                input(IOUContract.ID, inputState)
                output(IOUContract.ID, outputState)
                command(listOf(bank.publicKey, bob.publicKey), IOUContract.Commands.Settle())
                verifies()
            }
        }
    }


    @Test
    fun `test transfer command checks all signers and only lender changes after transfer`() {
        val linearId = UniqueIdentifier("test id", UUID.randomUUID())

        val inputState = IOUState(
            "old input state",
            Amount<Currency>(100, Currency.getInstance(Locale.UK)),
            Amount<Currency>(0, Currency.getInstance(Locale.UK)),
            alice.party, bob.party,
            linearId = linearId
        )

        val outputState = IOUState(
            "new input state",
            Amount<Currency>(100, Currency.getInstance(Locale.UK)),
            Amount<Currency>(0, Currency.getInstance(Locale.UK)),
            bank.party, bob.party,
            linearId = linearId
        )

        ledgerServices.ledger {
            transaction {
                input(IOUContract.ID, inputState)
                output(IOUContract.ID, outputState)
                command(listOf(bob.publicKey, bank.publicKey), IOUContract.Commands.Transfer())
                verifies()
            }
        }
    }

    @Test
    fun `test no input state should be provided for issuance`() {
        val linearId = UniqueIdentifier("test id", UUID.randomUUID())

        val inputState = IOUState(
            "old input state",
            Amount<Currency>(100, Currency.getInstance(Locale.UK)),
            Amount<Currency>(0, Currency.getInstance(Locale.UK)),
            alice.party, bob.party,
            linearId = linearId
        )

        val outputState = IOUState(
            "new input state",
            Amount<Currency>(100, Currency.getInstance(Locale.UK)),
            Amount<Currency>(0, Currency.getInstance(Locale.UK)),
            bank.party, bob.party,
            linearId = linearId
        )

        ledgerServices.ledger {
            transaction {
                input(IOUContract.ID, inputState)
                output(IOUContract.ID, outputState)
                command(listOf(alice.publicKey, bank.publicKey), IOUContract.Commands.Issue())
                fails()
            }
        }
    }

    @Test
    fun `test issued output amount is bigger than zero`() {
        val validInitialOutputState = IOUState(
            "output state",
            Amount<Currency>(100, Currency.getInstance(Locale.UK)),
            Amount<Currency>(0, Currency.getInstance(Locale.UK)),
            alice.party, bob.party,
            linearId = UniqueIdentifier("test id", UUID.randomUUID())
        )

        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, validInitialOutputState)
                command(alice.publicKey, IOUContract.Commands.Issue())
                verifies()
            }
        }

        val invalidInitialOutputState = IOUState(
            "output state",
            Amount<Currency>(0, Currency.getInstance(Locale.UK)),
            Amount<Currency>(0, Currency.getInstance(Locale.UK)),
            alice.party, bob.party,
            linearId = UniqueIdentifier("test id", UUID.randomUUID())
        )

        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, invalidInitialOutputState)
                command(alice.publicKey, IOUContract.Commands.Issue())
                fails()
            }
        }
    }
}