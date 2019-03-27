package com.gitcoins.webserver

import com.google.gson.JsonParser
import com.gitcoins.flows.PushEventFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/git/")
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy


    @PostMapping(value = [ "/push-event" ])
    fun initiatePushEval(@RequestBody msg : String) : ResponseEntity<String> {
        var partyName = JsonParser().parse(msg).asJsonObject.getAsJsonObject("pusher").get("name").asString
        val partyX50Name = CordaX500Name.parse(partyName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX50Name) ?: return ResponseEntity.badRequest().body(
                "Party named $partyName cannot be found.\n")

        return try {
            val tokenIssued = proxy.startTrackedFlow(:: PushEventFlow, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("New push event on the repo by $partyName\n")
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @PostMapping(value = [ "/pr-event" ])
    fun initiatePREval(@RequestBody msg : String) : ResponseEntity<String> {
        var partyName = JsonParser().parse(msg).asJsonObject.getAsJsonObject("review").get("name").asString
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body("New pull request review event on the repo.\n")
            //Initiate issue tokens flow
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
}