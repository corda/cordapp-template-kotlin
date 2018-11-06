package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

const val RPC_USERNAME = "user1"
const val RPC_PASSWORD = "test"

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda node and
 * perform RPC operations on the node.
 */
fun main(args: Array<String>) = TemplateClient().main(args)

private class TemplateClient {
    companion object {
        val logger: Logger = loggerFor<TemplateClient>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        require(args.size == 1) { "Usage: TemplateClient <node address>" }
        val nodeAddress = parse(args[0])
        val client = CordaRPCClient(nodeAddress)
        val proxy = client.start(RPC_USERNAME, RPC_PASSWORD).proxy

        // Interact with the node.
        // For example, here we grab all existing ContractStates and log them.
        val existingContractStates = proxy.vaultQueryBy<ContractState>().states
        existingContractStates.forEach { stateAndRef ->
            logger.info("{}", stateAndRef.state.data)
        }
    }
}