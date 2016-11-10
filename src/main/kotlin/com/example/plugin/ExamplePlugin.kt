package com.example.plugin

import com.example.api.ExampleApi
import com.example.model.ExampleModel
import com.example.protocol.ExampleProtocol
import net.corda.core.crypto.Party
import net.corda.core.node.CordaPluginRegistry


class ExamplePlugin : CordaPluginRegistry() {
    // A list of classes that expose web APIs.
    override val webApis: List<Class<*>> = listOf(ExampleApi::class.java)
    // A list of protocols that are required for this cordapp
    override val requiredProtocols: Map<String, Set<String>> = mapOf(
            ExampleProtocol.Requester::class.java.name to setOf(ExampleModel::class.java.name, Party::class.java.name)
    )
    override val servicePlugins: List<Class<*>> = listOf(ExampleProtocol.Service::class.java)
    // A list of directories in the resources directory that will be served by Jetty under /web
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the exampleWeb directory in resources to /web/example
            "example" to javaClass.classLoader.getResource("exampleWeb").toExternalForm()
    )
}
