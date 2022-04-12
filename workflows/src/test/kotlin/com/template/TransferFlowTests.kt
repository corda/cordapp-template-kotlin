package com.template

import com.template.flows.IssueFlow
import com.template.flows.TransferFlow
import com.template.states.IOUState
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test


class TransferFlowTests {
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
    fun `transfer flow updates only the lender`() {
        // Issue a new state
        val iouAmount: Long = 500
        val issueFlow = IssueFlow(partyB.info.legalIdentities[0], iouAmount)
        val future = partyA.startFlow(issueFlow)
        network.runNetwork() // Required otherwise the test will hang. TODO: understand

        // Transfer the ownership
        val signedTnx = future.get()
        val iouLinearId = (signedTnx.tx.outputs.single().data as IOUState).linearId
        val transferFlow = TransferFlow(iouLinearId, partyC.info.legalIdentities[0])
        partyA.startFlow(transferFlow)
        network.runNetwork()

        // PartyA: original lender, PartyB: borrower, PartyC: New lender after the transfer
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val statesInPartyA = partyA.services.vaultService.queryBy(IOUState::class.java, inputCriteria).states
        val statesInPartyB = partyB.services.vaultService.queryBy(IOUState::class.java, inputCriteria).states
        val statesInPartyC = partyC.services.vaultService.queryBy(IOUState::class.java, inputCriteria).states

        // TODO: understand why and where it becomes zero
        assert(statesInPartyA.count() == 0)
        assert(statesInPartyB.count() == 1)
        assert(statesInPartyC.count() == 1)

        assert(statesInPartyB[0].state.data.linearId == statesInPartyC[0].state.data.linearId)
        assert(statesInPartyB[0].state.data.amount == statesInPartyC[0].state.data.amount)
        assert(statesInPartyB[0].state.data.paid == statesInPartyC[0].state.data.paid)
    }
}