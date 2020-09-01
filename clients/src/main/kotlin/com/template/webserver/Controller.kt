package com.template.webserver

import net.corda.core.contracts.StateAndRef
import net.corda.core.cordapp.CordappInfo
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException

/**
 * Define your API endpoints here.
 */
@CrossOrigin(origins = arrayOf("*"), maxAge = 3600) //Bit of a hack
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

//    @CrossOrigin
//    @GetMapping(value = ["/startGame"])
//    private fun startGame(): APIResponse<String> {
//        return try {
//            proxy.startFlow(
//                    ::StartGameFlow
//            ).returnValue.get()
//            APIResponse.success("Game started")
//        } catch (e: Exception) {
//            logger.error(e.message)
//            APIResponse.error("Could not start game: ${e.message}")
//        }
//    }
//
//    @CrossOrigin
//    @GetMapping(value = ["/getTopCardWithDetails"], produces = [MediaType.APPLICATION_JSON_VALUE])
//    private fun getTopCard(): APIResponse<CardDetails> {
//        return try {
//            APIResponse.success(
//                    proxy.startFlowDynamic(
//                            GetTopCardWithDetailsFlow::class.java
//                    ).returnValue.get()
//            )
//        } catch (e: Exception) {
//            logger.error(e.message)
//            APIResponse.error("Error ${e.message}")
//        }
//    }
//
//    @CrossOrigin
//    @GetMapping(value = ["/chooseCategory/{category}"], produces = [MediaType.APPLICATION_JSON_VALUE])
//    private fun chooseCategory(@PathVariable("category") category: Int): APIResponse<String> {
//        return try {
//            //using last for testing only
//            //val currentGame = proxy.vaultQuery(GameState::class.java).states.single()
//            val currentGame = proxy.vaultQuery(GameState::class.java).states.last()
//            proxy.startFlow(
//                    ::ChooseCategoryFlow,
//                    currentGame,
//                    category
//            ).returnValue.get()
//            APIResponse.success("Category picked: $category")
//        } catch (e: Exception) {
//            logger.error(e.message)
//            APIResponse.error("Error while picking category $category : ${e.message}")
//        }
//    }

    @GetMapping(value = [ "parties" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getParties() : APIResponse<List<String>> {
        return try {
            APIResponse.success(proxy.networkMapSnapshot().map{ it.legalIdentities.first().name.toString()})
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting parties")
        }
    }

    @GetMapping(value = [ "flows" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getInstalledFlows() : APIResponse<List<String>> {
        return try {
            APIResponse.success(proxy.registeredFlows())
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting flows")
        }
    }

    @GetMapping(value = [ "cordapps" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCordapps() : APIResponse<List<CordappInfo>> {
        return try {
            APIResponse.success(proxy.nodeDiagnosticInfo().cordapps)
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting cordapps")
        }
    }

//    @GetMapping(value = [ "states" ], produces = [MediaType.APPLICATION_JSON_VALUE])
//    fun getCordappStates() : APIResponse<List<CordappInfo>> {
//        return try {
//            APIResponse.success(proxy.nodeDiagnosticInfo().cordapps.first().)
//        } catch (e: Exception) {
//            logger.error(e.message)
//            APIResponse.error("Error while getting flows")
//        }
//    }
}