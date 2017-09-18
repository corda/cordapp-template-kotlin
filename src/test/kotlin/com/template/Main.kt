package com.template

import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.ServiceInfo
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.User
import net.corda.testing.driver.driver

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Run the "Run Template CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports for each node, which should be output to the console. The "Debug CorDapp" configuration runs
 *    with port 5007, which should be "NodeA". In any case, double-check the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf())
    driver(isDebug = true) {
        startNode(providedName = CordaX500Name("Controller", "London", "GB"), advertisedServices = setOf(ServiceInfo(ValidatingNotaryService.type)))
        val (nodeA, nodeB, nodeC) = listOf(
                startNode(providedName = CordaX500Name("Entity A", "Paris", "FR"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("Entity B", "Rome", "IT"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("Entity C", "New York", "US"), rpcUsers = listOf(user))).map { it.getOrThrow() }

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)

        waitForAllNodesToFinish()
    }
}