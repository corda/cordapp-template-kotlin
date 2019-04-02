package com.gitcoins.webserver

import com.beust.klaxon.Klaxon
import com.beust.klaxon.PathMatcher
import com.gitcoins.flows.CreateKeyFlow
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
import java.lang.IllegalArgumentException
import java.util.regex.Pattern

private const val CREATE_KEY: String = "createKey"


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


    @PostMapping(value = [ "/create-key" ])
    fun createKey(@RequestBody msg : String) : ResponseEntity<String> {

        var gitUserName: String?=null

        try {
            val pathMatcher = object : PathMatcher {
                override fun pathMatches(path: String) = Pattern.matches(".*comment.*body.*", path)
                override fun onMatch(path: String, value: Any) {
                    //FIXME
                    if (value.toString() != CREATE_KEY)
                        logger.debug("pr comment is '${value.toString()}")
//                        throw IllegalArgumentException("Invalid pr comment. Please comment 'createKey'.")
                }
            }
            Klaxon().pathMatcher(pathMatcher).parseJsonObject(StringReader(msg))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        try {
            val pathMatcher = object : PathMatcher {
                override fun pathMatches(path: String) = Pattern.matches(".*comment.*user.*login.*", path)
                override fun onMatch(path: String, value: Any) {
                    logger.debug("Github user: $value")
                    gitUserName = value.toString()
                }
            }
            Klaxon().pathMatcher(pathMatcher).parseJsonObject(StringReader(msg))
        } catch (e: MalformedJsonException) {
            e.printStackTrace()
            return ResponseEntity.badRequest().body("Github username must not be null.\n")
        }

        return try {
            proxy.startTrackedFlow(:: CreateKeyFlow, gitUserName.toString()).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("New public key generated for github user: $gitUserName")
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


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
            return ResponseEntity.badRequest().body("Github username must not be null.\n")
        }

        return try {
            proxy.startTrackedFlow(:: PushEventFlow, gitUserName.toString()).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("New push event on the repo by: $gitUserName")
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

        return try {
            proxy.startTrackedFlow(:: PullReviewEventFlow, gitUserName.toString()).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("New pull request review event on the repo by $gitUserName\n")
            //Initiate issue tokens flow
        } catch (ex: Throwable) {
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
}