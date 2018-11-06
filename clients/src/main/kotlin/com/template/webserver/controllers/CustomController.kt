package com.template.webserver.controllers

import com.template.webserver.NodeRPCConnection
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Define CorDapp-specific endpoints in a controller such as this.
 */
@RestController
@RequestMapping("/custom") // The paths for GET and POST requests are relative to this base path.
class CustomController(
        private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/customendpoint", produces = arrayOf("text/plain"))
    private fun status() = "Modify this."
}