package com.template.states

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.r3.corda.sdk.token.contracts.FungibleTokenContract
import com.r3.corda.sdk.token.contracts.commands.IssueTokenCommand
import com.r3.corda.sdk.token.contracts.commands.MoveTokenCommand
import com.r3.corda.sdk.token.contracts.utilities.heldBy
import com.r3.corda.sdk.token.contracts.utilities.issuedBy
import com.r3.corda.sdk.token.contracts.utilities.of
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NotaryInfo
import net.corda.node.services.api.IdentityServiceInternal
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.SerializationEnvironmentRule
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.EnforceVerifyOrFail
import net.corda.testing.dsl.TransactionDSL
import net.corda.testing.dsl.TransactionDSLInterpreter
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import org.junit.Rule
import org.junit.Test

class GitTokensTests {
    private companion object {
        val NOTARY = TestIdentity(DUMMY_NOTARY_NAME, 20)
        val ISSUER = TestIdentity(CordaX500Name("ISSUER", "London", "GB"))
        val ALICE = TestIdentity(CordaX500Name("ALICE", "London", "GB"))
        val BOB = TestIdentity(CordaX500Name("BOB", "London", "GB"))
    }

    @Rule
    @JvmField
    val testSerialization = SerializationEnvironmentRule()

    private val aliceServices = MockServices(
            cordappPackages = listOf("com.r3.corda.sdk.token.contracts", "com.r3.corda.sdk.token.money"),
            initialIdentity = ALICE,
            identityService = mock<IdentityServiceInternal>().also {
                doReturn(ALICE.party).whenever(it).partyFromKey(ALICE.publicKey)
                doReturn(BOB.party).whenever(it).partyFromKey(BOB.publicKey)
                doReturn(ISSUER.party).whenever(it).partyFromKey(ISSUER.publicKey)
            },
            networkParameters = testNetworkParameters(
                    minimumPlatformVersion = 4,
                    notaries = listOf(NotaryInfo(NOTARY.party, false))
            )
    )

    private fun transaction(script: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail) {
        aliceServices.transaction(NOTARY.party, script)
    }

    class WrongCommand : TypeOnlyCommandData()


    @Test
    fun `test issue token` () {
        val issuedToken  = GitToken() issuedBy ISSUER.party
        transaction {

            output(FungibleTokenContract.contractId, 1 of issuedToken heldBy ALICE.party)

            // No commands
            tweak {
                this.`fails with`("A transaction must contain at least one command")
            }

            // Signed by a non-issuing party
            tweak {
                command(BOB.publicKey, IssueTokenCommand(issuedToken))
                this.`fails with`("The issuer must be the only signing party when an amount of tokens are issued.")
            }

            // Multiple signatures on the issuing command
            tweak {
                command(listOf(BOB.publicKey, ALICE.publicKey), IssueTokenCommand(issuedToken))
                this.`fails with`("The issuer must be the only signing party when an amount of tokens are issued.")
            }

            // No issuing command
            tweak {
                command(BOB.publicKey, WrongCommand())
                this.`fails with`("There must be at least one owned token command this transaction.")
            }

            // Non-issuing command present with the issuing command
            tweak {
                command(ISSUER.publicKey, IssueTokenCommand(issuedToken))
                command(ISSUER.publicKey, MoveTokenCommand(issuedToken))
                this.`fails with`("There must be exactly one TokenCommand type per group! For example: You cannot "+
                    "map an Issue AND a Move command to one group of tokens in a transaction.")
                }

            // Input states forbidden on issuing commands
            tweak {
                input(FungibleTokenContract.contractId, 1 of issuedToken heldBy ALICE.party)
                command(ISSUER.publicKey, IssueTokenCommand(issuedToken))
                this.`fails with`("When issuing tokens, there cannot be any input states.")
            }

            // Zero value token amounts are forbidden
            tweak {
                output(FungibleTokenContract.contractId, 0 of issuedToken heldBy ALICE.party)
                command(ISSUER.publicKey, IssueTokenCommand(issuedToken))
                this.`fails with`("You cannot issue tokens with a zero amount")
            }

            // Assert verifies
            //
            // Multiple output states
            tweak {
                output(FungibleTokenContract. contractId, 1 of issuedToken heldBy ALICE.party)
                output(FungibleTokenContract. contractId, 5 of issuedToken heldBy ALICE.party)
                output(FungibleTokenContract. contractId, 10 of issuedToken heldBy ALICE.party)
                command(ISSUER.publicKey, IssueTokenCommand(issuedToken))
                verifies()
            }

            tweak {
                command(ISSUER.publicKey, IssueTokenCommand(issuedToken))
                verifies()
            }
        }
    }
}