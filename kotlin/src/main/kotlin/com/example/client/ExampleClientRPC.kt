package com.example.client

import com.example.contract.PurchaseOrderState
import com.google.common.net.HostAndPort
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import net.corda.node.services.config.configureTestSSL
import net.corda.node.services.messaging.CordaRPCClient
import org.slf4j.Logger
import rx.Observable

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

fun main(args: Array<String>) {
    ExampleClientRPC().main(args)
}

private class ExampleClientRPC {
    companion object {
        val logger: Logger = loggerFor<ExampleClientRPC>()
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: ExampleClientRPC <node address>" }
        val nodeAddress = HostAndPort.fromString(args[0])
        val client = CordaRPCClient(nodeAddress, configureTestSSL())

        // Can be amended in the com.example.MainKt file.
        client.start("user1", "test")
        val proxy = client.proxy()

        // Grab all signed transactions and all future signed transactions.
        val (transactions: List<SignedTransaction>, futureTransactions: Observable<SignedTransaction>) =
                proxy.verifiedTransactions()

        // Log the 'placed' purchase order states and listen for new ones.
        futureTransactions.startWith(transactions).toBlocking().subscribe { transaction ->
            transaction.tx.outputs.forEach { output ->
                val state = output.data as PurchaseOrderState
                logger.info(state.purchaseOrder.toString())
            }
        }
    }
}
