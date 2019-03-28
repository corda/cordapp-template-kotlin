package com.gitcoins

import com.gitcoins.flows.PullReviewEventFlow
import com.gitcoins.flows.PushEventFlow
import com.gitcoins.states.GitToken
import com.r3.corda.sdk.token.contracts.states.FungibleToken
import com.r3.corda.sdk.token.workflow.utilities.tokenBalance
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GitEventFlowTests {

    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private lateinit var user: Party

    @Before
    fun setup() {
        network = MockNetwork(
                cordappPackages = listOf(
                        "com.r3.corda.sdk.token.contracts",
                        "com.r3.corda.sdk.token.workflow"
                ),
                threadPerNode = true,
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4))

        a = network.createNode()
        b = network.createNode()
        user = b.info.singleIdentity()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `Issue git token from push event`() {
        val flow = PushEventFlow(user)
        val signedTx = a.transaction { a.startFlow(flow) }.getOrThrow()

        assertEquals(signedTx, a.services.validatedTransactions.getTransaction(signedTx.id))

        b.transaction { b.services.validatedTransactions.trackTransaction(signedTx.id) }.getOrThrow()

        val token = signedTx.tx.outRefsOfType<FungibleToken<GitToken>>().single()
        val vaultToken = b.services.vaultService.queryBy<FungibleToken<GitToken>>().states.single()
        assertEquals(token, vaultToken)
        val tokenBalance = b.services.vaultService.tokenBalance(GitToken())
        assertEquals(1, tokenBalance.quantity)
    }


    @Test
    fun `Issue git token from pull request review event`() {
        val flow = PullReviewEventFlow(user)
        val signedTx = a.transaction { a.startFlow(flow) }.getOrThrow()
        assertEquals(signedTx, a.services.validatedTransactions.getTransaction(signedTx.id))

        b.transaction { b.services.validatedTransactions.trackTransaction(signedTx.id) }.getOrThrow()

        val token = signedTx.tx.outRefsOfType<FungibleToken<GitToken>>().single()
        val vaultToken = b.services.vaultService.queryBy<FungibleToken<GitToken>>().states.single()
        assertEquals(token, vaultToken)

        val tokenBalance = b.services.vaultService.tokenBalance(GitToken())
        assertEquals(1, tokenBalance.quantity)
    }
}