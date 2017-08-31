package net.corda.test

import net.corda.core.node.services.ServiceInfo
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.testing.DUMMY_BANK_A
import net.corda.testing.DUMMY_BANK_B
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.driver.driver
import org.junit.Assert
import org.junit.Test

class DriverBasedTest {
    @Test
    fun `run driver test`() {
        driver(isDebug = true, startNodesInProcess = true) {
            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (notaryHandle, nodeAHandle, nodeBHandle) = listOf(
                    startNode(DUMMY_NOTARY.name, setOf(ServiceInfo(SimpleNotaryService.type))),
                    startNode(DUMMY_BANK_A.name),
                    startNode(DUMMY_BANK_B.name)
            ).map { it.getOrThrow() }

            // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
            // started and can communicate. This is a very basic test, in practice tests would be starting flows,
            // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
            // as intended.
            Assert.assertEquals(notaryHandle.rpc.partyFromX500Name(DUMMY_BANK_A.name), DUMMY_NOTARY)
            Assert.assertEquals(nodeAHandle.rpc.partyFromX500Name(DUMMY_BANK_B.name), DUMMY_BANK_B)
            Assert.assertEquals(nodeBHandle.rpc.partyFromX500Name(DUMMY_NOTARY.name), DUMMY_NOTARY)
        }
    }
}