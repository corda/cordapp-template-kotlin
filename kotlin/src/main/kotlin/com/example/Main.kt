package com.example

import net.corda.core.node.services.ServiceInfo
import net.corda.node.driver.driver
import net.corda.node.services.User
import net.corda.node.services.transactions.ValidatingNotaryService

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Firstly, run the "Run Example CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports which should be output to the console for each node. They typically start at 5006, 5007,
 *    5008. The "Debug CorDapp" configuration runs with port 5007, which should be "NodeB". In any case, double check
 *    the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf())
    driver(dsl = {
        startNode("Controller", setOf(ServiceInfo(ValidatingNotaryService.type)))
        startNode("NodeA", rpcUsers = listOf(user))
        startNode("NodeB", rpcUsers = listOf(user))
        startNode("NodeC", rpcUsers = listOf(user))
        waitForAllNodesToFinish()
    }, isDebug = true)
}
