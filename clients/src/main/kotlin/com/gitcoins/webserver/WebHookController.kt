package com.gitcoins.webserver

import com.beust.klaxon.Klaxon
import com.beust.klaxon.PathMatcher
import com.gitcoins.flows.PullReviewEventFlow
import com.gitcoins.flows.PushEventFlow
import com.google.gson.stream.MalformedJsonException
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.io.StringReader
import java.lang.Exception
import java.util.regex.Pattern

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/git/")
class WebHookController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @PostMapping(value = [ "/push-event" ])
    fun initPushFlow(@RequestBody msg : String) : ResponseEntity<String> {

        var gitUserName: String?=null
        try {
            val pathMatcher = object : PathMatcher {
                override fun pathMatches(path: String) = Pattern.matches(".*pusher.*name.*", path)
                override fun onMatch(path: String, value: Any) {
                    logger.debug("Github user: $value")
                    gitUserName = value.toString()
                }
            }
            Klaxon().pathMatcher(pathMatcher).parseJsonObject(StringReader(msg))
        } catch (e: MalformedJsonException) {
            e.printStackTrace()
        }

        if (gitUserName.isNullOrBlank())
            throw MalformedJsonException("The reviewer's user name was not found in the request body.")
        val partyX50Name = CordaX500Name.parse(gitUserName.toString())
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX50Name) ?: return ResponseEntity.badRequest().body(
                "Party named $gitUserName cannot be found.\n")

        return try {
            proxy.startTrackedFlow(:: PushEventFlow, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("New push event on the repo by $gitUserName\n")
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @PostMapping(value = [ "/pr-event" ])
    fun initPRFlow(@RequestBody msg : String) : ResponseEntity<String> {

        var gitUserName: String?=null
        try {
            val pathMatcher = object : PathMatcher {
                override fun pathMatches(path: String) = Pattern.matches(".*review.*user.*login.*", path)
                override fun onMatch(path: String, value: Any) {
                    logger.debug("Github user: $value")
                    gitUserName = value.toString()
                }
            }
            Klaxon().pathMatcher(pathMatcher).parseJsonObject(StringReader(msg))
        } catch (e: MalformedJsonException) {
            e.printStackTrace()
        }

        if (gitUserName.isNullOrBlank())
            throw MalformedJsonException("The reviewer's user name was not found in the request body.")
        val partyX50Name = CordaX500Name.parse(gitUserName.toString())
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX50Name) ?: return ResponseEntity.badRequest().body(
                "Party named $gitUserName cannot be found.\n")
        return try {
            proxy.startTrackedFlow(:: PullReviewEventFlow, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("New pull request review event on the repo by $gitUserName\n")
            //Initiate issue tokens flow
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
}