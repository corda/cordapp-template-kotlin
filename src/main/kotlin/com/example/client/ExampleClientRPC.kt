package com.example.client

import com.google.common.net.HostAndPort
import com.r3corda.client.CordaRPCClient
import com.r3corda.core.transactions.SignedTransaction
import com.r3corda.core.utilities.loggerFor
import com.r3corda.node.services.messaging.CordaRPCOps
import org.graphstream.graph.Edge
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import rx.Observable
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  execute an exposed RPC command [CordaRPCOps]
 *
 *  Please follow instructions in the following tutorial to execute this example:
 *  https://docs.corda.r3cev.com/tutorial-clientrpc-api.html for further details.
 **/

class ExampleClientRPC {
}

private val log = loggerFor<ExampleClientRPC>()

enum class PrintOrVisualise {
    Print,
    Visualise
}

fun main(args: Array<String>) {

    require(args.size == 2) { "Usage: ExampleClientRPC <node address> [Print|Visualise]" }

    val nodeAddress = HostAndPort.fromString(args[0])
    val printOrVisualise = PrintOrVisualise.valueOf(args[1])
    val certificatePath = Paths.get("build/resources/main/certificates")

    val client = CordaRPCClient(nodeAddress, certificatePath)
    client.start()
    val proxy = client.proxy()

    val (transactions: List<SignedTransaction>, futureTransactions: Observable<SignedTransaction>) =
        proxy.verifiedTransactions()

    when (printOrVisualise) {
        PrintOrVisualise.Print -> {
            futureTransactions.startWith(transactions).subscribe { transaction ->
                log.info("NODE ${transaction.id}")
                transaction.tx.inputs.forEach { input ->
                    log.info("EDGE ${input.txhash} ${transaction.id}")
                }
            }
            CompletableFuture<Unit>().get()
        }
        PrintOrVisualise.Visualise -> {
            val graph = SingleGraph("transactions")
            transactions.forEach { transaction ->
                graph.addNode<Node>("${transaction.id}")
            }
            transactions.forEach { transaction ->
                transaction.tx.inputs.forEach { ref ->
                    graph.addEdge<Edge>("$ref", "${ref.txhash}", "${transaction.id}")
                }
            }
            futureTransactions.subscribe { transaction ->
                graph.addNode<Node>("${transaction.id}")
                transaction.tx.inputs.forEach { ref ->
                    graph.addEdge<Edge>("$ref", "${ref.txhash}", "${transaction.id}")
                }
            }
            graph.display()
        }
    }
}