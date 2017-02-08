package com.template.plugin;

import com.esotericsoftware.kryo.Kryo;
import com.template.api.TemplateApi;
import com.template.flow.TemplateFlow;
import com.template.service.TemplateService;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.CordaPluginRegistry;
import net.corda.core.node.PluginServiceHub;

import java.util.*;
import java.util.function.Function;

public class TemplatePlugin extends CordaPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = Collections.singletonList(TemplateApi::new);

    /**
     * A list of flows required for this CorDapp.
     */
    private final Map<String, Set<String>> requiredFlows = Collections.singletonMap(
            TemplateFlow.Initiator.class.getName(),
            new HashSet<>(Collections.emptyList()));

    /**
     * A list of long-lived services to be hosted within the node.
     */
    private final List<Function<PluginServiceHub, ?>> servicePlugins = Collections.singletonList(TemplateService::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     * The template's web frontend is accessible at /web/template.
     */
    private final Map<String, String> staticServeDirs = Collections.singletonMap(
            // This will serve the templateWeb directory in resources to /web/template
            "template", getClass().getClassLoader().getResource("templateWeb").toExternalForm()
    );

    /**
     * Registering the required types with Kryo, Corda's serialisation framework.
     */
    @Override public boolean registerRPCKryoTypes(Kryo kryo) {
        return true;
    }

    @Override public List<Function<CordaRPCOps, ?>> getWebApis() { return webApis; }
    @Override public Map<String, Set<String>> getRequiredFlows() { return requiredFlows; }
    @Override public List<Function<PluginServiceHub, ?>> getServicePlugins() { return servicePlugins; }
    @Override public Map<String, String> getStaticServeDirs() { return staticServeDirs; }
}