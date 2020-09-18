package com.template.webserver

import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.time.Instant
import java.time.LocalDateTime

@Configuration
open class SerializationConfig {

    @Bean
    open fun mappingJackson2HttpMessageConverter(rpc: NodeRPCConnection): MappingJackson2HttpMessageConverter? {
        val mapper = JacksonSupport.createNonRpcMapper()
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }

}