package net.corda.test

import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.nodeapi.ServiceInfo
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
                    startNode(providedName = DUMMY_NOTARY.name, advertisedServices = setOf(ServiceInfo(SimpleNotaryService.type))),
                    startNode(providedName = DUMMY_BANK_A.name),
                    startNode(providedName = DUMMY_BANK_B.name)
            ).map { it.getOrThrow() }

            // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
            // started and can communicate. This is a very basic test, in practice tests would be starting flows,
            // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
            // as intended.
            Assert.assertEquals(notaryHandle.rpc.wellKnownPartyFromX500Name(DUMMY_BANK_A.name)!!.name, DUMMY_BANK_A.name)
            Assert.assertEquals(nodeAHandle.rpc.wellKnownPartyFromX500Name(DUMMY_BANK_B.name)!!.name, DUMMY_BANK_B.name)
            Assert.assertEquals(nodeBHandle.rpc.wellKnownPartyFromX500Name(DUMMY_NOTARY.name)!!.name, DUMMY_NOTARY.name)
        }
    }
}