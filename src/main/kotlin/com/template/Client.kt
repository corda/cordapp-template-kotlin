package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.utilities.loggerFor
import net.corda.core.utilities.parseNetworkHostAndPort
import org.slf4j.Logger

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda Node and
 * stream some State data from the node.
 */
fun main(args: Array<String>) {
    TemplateClient().main(args)
}

private class TemplateClient {
    companion object {
        val logger: Logger = loggerFor<TemplateClient>()
        private fun logState(state: StateAndRef<TemplateState>) = logger.info("{}", state.state.data))
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: TemplateClient <node address>" }
        val nodeAddress = args[0].parseNetworkHostAndPort()
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.template.MainKt file.
        val proxy = client.start("user1", "test").proxy

        // Grab all signed transactions and all future signed transactions.
        val (snapshot, updates) = proxy.vaultTrack(TemplateState::class.java)

        // Log the existing TemplateStates and listen for new ones.
        snapshot.states.forEach { logState(it) }
        updates.toBlocking().subscribe { update ->
            update.produced.forEach { logState(it) }
        }
    }
}
