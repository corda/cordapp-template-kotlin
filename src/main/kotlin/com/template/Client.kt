package com.template

import com.google.common.net.HostAndPort
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import rx.Observable

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
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: TemplateClient <node address>" }
        val nodeAddress = HostAndPort.fromString(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.template.MainKt file.
        val proxy = client.start("user1", "test").proxy

        // Grab all signed transactions and all future signed transactions.
        val (transactions: List<SignedTransaction>, futureTransactions: Observable<SignedTransaction>) =
                proxy.verifiedTransactionsFeed()

        // Log the existing TemplateStates and listen for new ones.
        futureTransactions.startWith(transactions).toBlocking().subscribe { transaction ->
            transaction.tx.outputs.forEach { (data) ->
                val state = data as TemplateState
                logger.info(state.toString())
            }
        }
    }
}
