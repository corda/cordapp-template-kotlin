package com.template.plugin

import com.esotericsoftware.kryo.Kryo
import com.template.api.TemplateApi
import com.template.flow.TemplateFlow
import com.template.service.TemplateService
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.node.PluginServiceHub
import java.util.function.Function

class TemplatePlugin : CordaPluginRegistry() {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::TemplateApi))

    /**
     * A list of flows required for this CorDapp.
     */
    override val requiredFlows: Map<String, Set<String>> = mapOf(
            TemplateFlow.Initiator::class.java.name to setOf()
    )

    /**
     * A list of long-lived services to be hosted within the node.
     */
    override val servicePlugins: List<Function<PluginServiceHub, out Any>> = listOf(Function(TemplateService::Service))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     * The template's web frontend is accessible at /web/template.
     */
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the templateWeb directory in resources to /web/template
            "template" to javaClass.classLoader.getResource("templateWeb").toExternalForm()
    )

    /**
     * Registering the required types with Kryo, Corda's serialisation framework.
     */
    override fun registerRPCKryoTypes(kryo: Kryo): Boolean {
        return true
    }
}