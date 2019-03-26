package com.template.webserver

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

    @PostMapping(value = [ "/push-event" ])
    fun initiatePushEval(@RequestBody string : String) : ResponseEntity<String> {

        return try {
            ResponseEntity.status(HttpStatus.CREATED).body("New push event on the repo.\n")
            //Initiate issue tokens flow
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @PostMapping(value = [ "/pr-event" ])
    fun initiatePREval(@RequestBody string : String) : ResponseEntity<String> {

        return try {
            ResponseEntity.status(HttpStatus.CREATED).body("New PR event on the repo.\n")
            //Initiate issue tokens flow
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @PostMapping( value = ["/payload"])
    fun payload(@RequestBody string : String) : String {

        return "Webhook gr8 succsezz."
    }
}