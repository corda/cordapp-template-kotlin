package com.gitcoins

import com.gitcoins.flows.CreateKeyFlow
import com.gitcoins.flows.PullRequestReviewEventFlow
import com.gitcoins.flows.PushEventFlow
import com.gitcoins.flows.QueryGitUserDatabaseFlow
import com.gitcoins.states.GitToken
import com.r3.corda.sdk.token.contracts.states.FungibleToken
import com.r3.corda.sdk.token.workflow.utilities.tokenBalance
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GitEventFlowTests {

    private companion object {
        val gitUser = "gitUsername"
    }

    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var user: Party

    @Before
    fun setup() {
        network = MockNetwork(
                cordappPackages = listOf(
                        "com.r3.corda.sdk.token.contracts",
                        "com.r3.corda.sdk.token.workflow",
                        "com.gitcoins.schema"
                ),
                threadPerNode = true,
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4))

        a = network.createNode()
        user = a.info.singleIdentity()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `Query git users`() {
        val result = a.startFlow(QueryGitUserDatabaseFlow(gitUser)).getOrThrow()
        assertThat(result).isEmpty()
    }

    @Test
    fun `Create new public key`() {
        val future = a.startFlow(CreateKeyFlow(gitUser))
        network.waitQuiescent()
        future.getOrThrow()

        val result = a.startFlow(QueryGitUserDatabaseFlow(gitUser)).getOrThrow()
        assertEquals(gitUser, result.first().gitUserName)

        val duplicate = a.startFlow(CreateKeyFlow(gitUser))
        assertFailsWith<FlowException> { duplicate.getOrThrow() }
    }

    @Test
    fun `Issue git token from push event`() {
        val futureFail = a.startFlow(PushEventFlow(gitUser))
        assertFailsWith<FlowException> { futureFail.getOrThrow() }

        val future = a.startFlow(CreateKeyFlow(gitUser))
        network.waitQuiescent()
        future.getOrThrow()

        val signedTx = a.transaction { a.startFlow(PushEventFlow(gitUser)) }.getOrThrow()
        assertEquals(signedTx, a.services.validatedTransactions.getTransaction(signedTx.id))

        val token = signedTx.tx.outRefsOfType<FungibleToken<GitToken>>().single()
        val vaultToken = a.services.vaultService.queryBy<FungibleToken<GitToken>>().states.single()
        assertEquals(token, vaultToken)

        val tokenBalance = a.services.vaultService.tokenBalance(GitToken())
        assertEquals(1, tokenBalance.quantity)
    }

    @Test
    fun `Issue git token from pull request review event`() {
        val futureFail = a.startFlow(PullRequestReviewEventFlow(gitUser))
        assertFailsWith<FlowException> { futureFail.getOrThrow() }

        val future = a.startFlow(CreateKeyFlow(gitUser))
        network.waitQuiescent()
        future.getOrThrow()

        val signedTx = a.transaction { a.startFlow(PullRequestReviewEventFlow(gitUser)) }.getOrThrow()
        assertEquals(signedTx, a.services.validatedTransactions.getTransaction(signedTx.id))

        val token = signedTx.tx.outRefsOfType<FungibleToken<GitToken>>().single()
        val vaultToken = a.services.vaultService.queryBy<FungibleToken<GitToken>>().states.single()
        assertEquals(token, vaultToken)

        val tokenBalance = a.services.vaultService.tokenBalance(GitToken())
        assertEquals(1, tokenBalance.quantity)
    }
}