package com.template

import com.template.flows.IssueFlow
import com.template.flows.SettleFlow
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


class SettleFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var partyA: StartedMockNode
    private lateinit var partyB: StartedMockNode
    private lateinit var partyC: StartedMockNode

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
        partyC = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `settle flow partially settles`() {
        // Issue a new state
        val iouAmount: Long = 200
        val issueFlow = IssueFlow(partyB.info.legalIdentities[0], iouAmount)
        val future = partyA.startFlow(issueFlow)
        network.runNetwork() // Required otherwise the test will hang. TODO: understand why

        // Settle partial amount
        val signedTnx = future.get()
        val iouLinearId = (signedTnx.tx.outputs.single().data as IOUState).linearId
        val settleFlow = SettleFlow(
            lender = partyA.info.legalIdentities[0],
            stateId = iouLinearId,
            toPay = Amount(100, Currency.getInstance(Locale.UK))
        )
        partyB.startFlow(settleFlow)
        network.runNetwork()

        // PartyA: original lender, PartyB: borrower, PartyC: New lender after the transfer
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val statesInPartyA = partyA.services.vaultService.queryBy(IOUState::class.java, inputCriteria).states
        val statesInPartyB = partyB.services.vaultService.queryBy(IOUState::class.java, inputCriteria).states

        assert(statesInPartyA.count() == 1)
        assert(statesInPartyB.count() == 1)

        assert(statesInPartyB[0].state.data.amount == Amount(iouAmount, Currency.getInstance(Locale.UK)))
        assert(statesInPartyB[0].state.data.paid == Amount(100, Currency.getInstance(Locale.UK)))
    }
}