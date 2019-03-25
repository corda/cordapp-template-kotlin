package com.template.states

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.r3.corda.sdk.token.contracts.FungibleTokenContract
import com.r3.corda.sdk.token.contracts.commands.IssueTokenCommand
import com.r3.corda.sdk.token.contracts.commands.MoveTokenCommand
import com.r3.corda.sdk.token.contracts.commands.RedeemTokenCommand
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


    @Test
    fun `test move tokens`() {
        val issuedToken = GitToken() issuedBy ISSUER.party

        transaction {
            input(FungibleTokenContract.contractId, 1 of issuedToken heldBy ALICE.party)
            output(FungibleTokenContract.contractId, 1 of issuedToken heldBy BOB.party)

            // Move command signed by Alice
            tweak {
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                verifies()
            }

            // Issue and move commands within the transaction
            tweak {
                output(FungibleTokenContract.contractId, 1 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                command(BOB.publicKey, IssueTokenCommand(issuedToken issuedBy BOB.party))
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                verifies()
            }

            // Missing input
            tweak {
                output(FungibleTokenContract.contractId, 1 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                command(ALICE.publicKey, MoveTokenCommand(issuedToken issuedBy BOB.party))
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                this.`fails with`("When moving tokens, there must be input states present.")
            }

            // Missing output
            tweak {
                input(FungibleTokenContract.contractId, 1 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                command(ALICE.publicKey, MoveTokenCommand(issuedToken issuedBy BOB.party))
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                this.`fails with`("When moving tokens, there must be output states present.")
            }

            // Zero input sum
            tweak {
                input(FungibleTokenContract.contractId, 0 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                input(FungibleTokenContract.contractId, 0 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                output(FungibleTokenContract.contractId, 1 of issuedToken issuedBy BOB.party heldBy BOB.party)
                command(ALICE.publicKey, MoveTokenCommand(issuedToken issuedBy BOB.party))
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                this.`fails with`("In move groups there must be an amount of input tokens > ZERO.")
            }

            // Zero output sum
            tweak {
                input(FungibleTokenContract.contractId, 1 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                output(FungibleTokenContract.contractId, 0 of issuedToken issuedBy BOB.party heldBy BOB.party)
                output(FungibleTokenContract.contractId, 0 of issuedToken issuedBy BOB.party heldBy BOB.party)
                command(ALICE.publicKey, MoveTokenCommand(issuedToken issuedBy BOB.party))
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                this.`fails with`("In move groups there must be an amount of output tokens > ZERO.")
            }

            // Unbalanced move
            tweak {
                input(FungibleTokenContract.contractId, 1 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                output(FungibleTokenContract.contractId, 2 of issuedToken issuedBy BOB.party heldBy BOB.party)
                command(ALICE.publicKey, MoveTokenCommand(issuedToken issuedBy BOB.party))
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                this.`fails with`("n move groups the amount of input tokens MUST EQUAL the amount of output tokens. " +
                        "In other words, you cannot create or destroy value when moving tokens.")
            }

            tweak {
                input(FungibleTokenContract.contractId, 10 of issuedToken issuedBy BOB.party heldBy ALICE.party)
                output(FungibleTokenContract.contractId, 10 of issuedToken issuedBy BOB.party heldBy BOB.party)
                command(ALICE.publicKey, MoveTokenCommand(issuedToken issuedBy BOB.party))
                command(ALICE.publicKey, MoveTokenCommand(issuedToken))
                verifies()
            }

            // Incorrect signature
            tweak {
                command(BOB.publicKey, MoveTokenCommand(issuedToken))
                this.`fails with`("There are required signers missing or some of the specified signers are not " +
                        "required. A transaction to move owned token amounts must be signed by ONLY ALL the owners " +
                        "of ALL the input owned token amounts.")
            }

            // Incorrect signature supplied with correct one
            tweak {
                command(listOf(ALICE.publicKey, BOB.publicKey), MoveTokenCommand(issuedToken))
                this.`fails with`("There are required signers missing or some of the specified signers are not " +
                        "required. A transaction to move owned token amounts must be signed by ONLY ALL the owners " +
                        "of ALL the input owned token amounts.")
            }
        }
    }

    @Test
    fun `test redeem tokens`() {
        val issuedToken = GitToken() issuedBy ISSUER.party
        transaction {
            input(FungibleTokenContract.contractId, 1 of issuedToken heldBy ALICE.party)

            // Output state present
            tweak {
                output(FungibleTokenContract.contractId, 1 of issuedToken heldBy ALICE.party)
                command(ISSUER.publicKey, RedeemTokenCommand(issuedToken))
                this.`fails with`("When redeeming tokens, there must be no output states.")
            }

            // Issuer signature on redeem
            tweak {
                command(ISSUER.publicKey, RedeemTokenCommand(issuedToken))
                verifies()
            }

            // Non-issuer signature present
            tweak {
                command(ALICE.publicKey, RedeemTokenCommand(issuedToken))
                this.`fails with`("The issuer must be the only signing party when an amount of tokens are redeemed.")
            }

            // Multiple input states
            tweak {
                input(FungibleTokenContract.contractId, 1 of issuedToken heldBy ALICE.party)
                input(FungibleTokenContract.contractId, 5 of issuedToken heldBy BOB.party)
                command(ISSUER.publicKey, RedeemTokenCommand(issuedToken))
                verifies()
            }
        }
    }
}