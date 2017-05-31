package com.template.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.template.api.TemplateApi;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.CordaPluginRegistry;
import net.corda.core.serialization.SerializationCustomization;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TemplatePlugin extends CordaPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(TemplateApi::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     * The template's web frontend is accessible at /web/template.
     */
    private final Map<String, String> staticServeDirs = ImmutableMap.of(
            // This will serve the templateWeb directory in resources to /web/template
            "template", getClass().getClassLoader().getResource("templateWeb").toExternalForm()
    );

    /**
     * Whitelisting the required types for serialisation by the Corda node.
     */
    @Override
    public boolean customizeSerialization(SerializationCustomization custom) {
        return true;
    }

    @Override public List<Function<CordaRPCOps, ?>> getWebApis() { return webApis; }
    @Override public Map<String, String> getStaticServeDirs() { return staticServeDirs; }
}