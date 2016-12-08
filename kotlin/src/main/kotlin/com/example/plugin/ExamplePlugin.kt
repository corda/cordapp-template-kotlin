package com.example.plugin

import com.esotericsoftware.kryo.Kryo
import com.example.api.ExampleApi
import com.example.contract.PurchaseOrderContract
import com.example.contract.PurchaseOrderState
import com.example.flow.ExampleFlow
import com.example.model.PurchaseOrder
import com.example.service.ExampleService
import net.corda.core.crypto.Party
import net.corda.core.node.CordaPluginRegistry

class ExamplePlugin : CordaPluginRegistry() {
    /** A list of classes that expose web APIs. */
    override val webApis: List<Class<*>> = listOf(ExampleApi::class.java)
    /**
     * A list of flows required for this CorDapp.
     *
     * Any flow which is invoked from from the web API needs to be registered as an entry into this Map. The Map
     * takes the form of:
     *
     *      Name of the flow to be invoked -> Set of the parameter types passed into the flow.
     *
     * E.g. In the case of this CorDapp:
     *
     *      "ExampleFlow.Initiator" -> Set(PurchaseOrderState, Party)
     *
     * This map also acts as a white list. Such that, if a flow is invoked via the API and not registered correctly
     * here, then the flow state machine will _not_ invoke the flow. Instead, an exception will be raised.
     */
    override val requiredFlows: Map<String, Set<String>> = mapOf(
            ExampleFlow.Initiator::class.java.name to setOf(PurchaseOrderState::class.java.name, Party::class.java.name)
    )
    /**
     * A list of long lived services to be hosted within the node. Typically you would use these to register flow
     * factories that would be used when an initiating party attempts to communicate with our node using a particular
     * flow. See the [ExampleService.Service] class for an implementation which sets up a
     */
    override val servicePlugins: List<Class<*>> = listOf(ExampleService.Service::class.java)
    /** A list of directories in the resources directory that will be served by Jetty under /web */
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the exampleWeb directory in resources to /web/example
            "example" to javaClass.classLoader.getResource("exampleWeb").toExternalForm()
    )

    /**
     * Register required types with Kryo (our serialisation framework)..
     */
    override fun registerRPCKryoTypes(kryo: Kryo): Boolean {
        kryo.register(PurchaseOrderState::class.java)
        kryo.register(PurchaseOrderContract::class.java)
        kryo.register(PurchaseOrder::class.java)
        return true
    }
}
