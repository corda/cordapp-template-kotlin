package com.example

import com.r3corda.node.driver.driver
import com.r3corda.node.services.transactions.SimpleNotaryService
import com.r3corda.core.node.services.ServiceInfo

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 */
fun main(args: Array<String>) {
    driver(dsl = {
        startNode("Notary", setOf(ServiceInfo(SimpleNotaryService.Type)))
        startNode("Bank A")
        startNode("Bank B")
        waitForAllNodesToFinish()
    }, isDebug = true)
}
