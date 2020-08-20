package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.loggerFor

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

private class Client {
    companion object {
        val logger = loggerFor<Client>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        require(args.size == 3) { "Usage: Client <node address> <rpc username> <rpc password>" }
        val nodeAddress = parse(args[0])
        val rpcUsername = args[1]
        val rpcPassword = args[2]
        val client = CordaRPCClient(nodeAddress)
        val clientConnection = client.start(rpcUsername, rpcPassword)
        val proxy = clientConnection.proxy

        // Interact with the node.
        // Example #1, here we print the nodes on the network.
        val nodes = proxy.networkMapSnapshot()
        println("\n-- Here is the networkMap snapshot --")
        logger.info("{}", nodes)

        // Example #2, here we print the PartyA's node info
        val me = proxy.nodeInfo().legalIdentities.first().name
        println("\n-- Here is the node info of the node that the client connected to --")
        logger.info("{}", me)

        //Close the client connection
        clientConnection.close()
    }
}