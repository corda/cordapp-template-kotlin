package com.template

import com.template.flows.IssueFlow
import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*


class IssueFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var partyA: StartedMockNode
    private lateinit var partyB: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.template.contracts"),
                    TestCordapp.findCordapp("com.template.flows")
                ),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB")))
            )
        )
        partyA = network.createPartyNode()
        partyB = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `initial state is issued correctly and stored in vault`() {
        val iouAmount: Long = 200
        val flow = IssueFlow(partyB.info.legalIdentities[0], iouAmount)
        partyA.startFlow(flow)
        network.runNetwork()

        // Successful query means the issued state are stored in both parties' vaults.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val stateStoredByPartyA =
            partyA.services.vaultService.queryBy(IOUState::class.java, inputCriteria).states[0].state.data
        val stateStoredByPartyB =
            partyB.services.vaultService.queryBy(IOUState::class.java, inputCriteria).states[0].state.data

        assert(stateStoredByPartyA.linearId == stateStoredByPartyB.linearId)
        assert(stateStoredByPartyB.amount == Amount(iouAmount, Currency.getInstance(Locale.UK)))
        assert(stateStoredByPartyB.paid == Amount(0, Currency.getInstance(Locale.UK)))
    }
}