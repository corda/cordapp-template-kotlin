package net.corda.test

import net.corda.core.getOrThrow
import net.corda.core.node.services.ServiceInfo
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.testing.DUMMY_BANK_A
import net.corda.testing.DUMMY_BANK_B
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.driver.driver
import org.junit.Test

class DriverBasedTest {
    private companion object {
        val nodeALegalName = DUMMY_BANK_A.name
        val nodeBLegalName = DUMMY_BANK_B.name
    }

    @Test
    fun `run driver test`() {
        driver(isDebug = true) {
            startNode(DUMMY_NOTARY.name, setOf(ServiceInfo(SimpleNotaryService.type))).getOrThrow()
            startNode(nodeALegalName).getOrThrow()
            startNode(nodeBLegalName).getOrThrow()
        }
    }
}