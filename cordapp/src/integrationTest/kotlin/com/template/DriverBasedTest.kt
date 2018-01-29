package com.template

import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.nodeapi.internal.ServiceInfo
import net.corda.testing.DUMMY_BANK_A
import net.corda.testing.DUMMY_BANK_B
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.driver.driver
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import kotlin.test.assertEquals

class DriverBasedTest {
    val nodeNames = listOf(DUMMY_NOTARY.name, DUMMY_BANK_A.name, DUMMY_BANK_B.name)

    @Test
    fun `node test`() {
        driver(isDebug = true, startNodesInProcess = true) {
            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (notaryHandle, partyAHandle, partyBHandle) = listOf(
                    startNode(providedName = nodeNames[0], advertisedServices = setOf(ServiceInfo(SimpleNotaryService.type))),
                    startNode(providedName = nodeNames[1]),
                    startNode(providedName = nodeNames[2])
            ).map { it.getOrThrow() }

            // This test makes an RPC call to retrieve another node's name from the network map, to verify that the
            // nodes have started and can communicate. This is a very basic test, in practice tests would be starting
            // flows, and verifying the states in the vault and other important metrics to ensure that your CorDapp is
            // working as intended.
            assertEquals(notaryHandle.rpc.wellKnownPartyFromX500Name(DUMMY_BANK_A.name)!!.name, DUMMY_BANK_A.name)
            assertEquals(partyAHandle.rpc.wellKnownPartyFromX500Name(DUMMY_BANK_B.name)!!.name, DUMMY_BANK_B.name)
            assertEquals(partyBHandle.rpc.wellKnownPartyFromX500Name(DUMMY_NOTARY.name)!!.name, DUMMY_NOTARY.name)
        }
    }

    @Test
    fun `node webserver test`() {
        driver(isDebug = true, startNodesInProcess = true) {
            val nodeHandles = listOf(
                    startNode(providedName = nodeNames[0], advertisedServices = setOf(ServiceInfo(SimpleNotaryService.type))),
                    startNode(providedName = nodeNames[1]),
                    startNode(providedName = nodeNames[2])
            ).map { it.getOrThrow() }

            // This test starts each node's webserver and makes an HTTP call to retrieve the body of a GET endpoint on
            // the node's webserver, to verify that the nodes' webservers have started and have loaded the API.
            nodeHandles.forEach { nodeHandle ->
                startWebserver(nodeHandle).getOrThrow()

                val nodeAddress = nodeHandle.webAddress
                val url = "http://$nodeAddress/api/template/templateGetEndpoint"

                val request = Request.Builder().url(url).build()
                val client = OkHttpClient()
                val response = client.newCall(request).execute()

                assertEquals("Template GET endpoint.", response.body().string())
            }
        }
    }
}