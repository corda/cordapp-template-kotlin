package com.template.webserver

import com.template.flows.IssueInitiator
import com.template.flows.TransferInitiator
import com.template.states.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */
    @GetMapping(value = "me", produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    fun whoami() = mapOf("me" to myLegalName)


    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GetMapping(value = "tokens", produces = arrayOf("application/json"))
    private fun tokens(): List<Map<String, String>> {
        val states = proxy.vaultQuery(TokenState::class.java).states

        return states.map { stateAndRef ->
            val token = stateAndRef.state.data

            mapOf<String, String>(
                    "creator" to token.creator.name.organisation,
                    "owner" to token.owner.name.organisation,
                    "description" to token.description,
                    "count" to token.ownerCount.toString(),
                    "id" to token.linearId.toString()
            )
        }
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /spring/api/tokens on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */

    @PostMapping(value = "create-token", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=application/x-www-form-urlencoded"))
    fun createToken(request: HttpServletRequest): ResponseEntity<String> {

        val owner = request.getParameter("owner")
                ?: return ResponseEntity.badRequest().body("Query parameter 'owner' must not be null.\n")
        val description = request.getParameter("description")
                ?: return ResponseEntity.badRequest().body("Query parameter 'description' must not be null.\n")

        val possibleParties = proxy.partiesFromName(owner, false)

        if (possibleParties.size != 1)
            return ResponseEntity.badRequest().body("Matching party not found.")

        val otherParty = possibleParties.iterator().next()

        return try {
            val signedTx = proxy.startTrackedFlow(::IssueInitiator, otherParty, description).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @PutMapping(value = "transfer-token", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=application/x-www-form-urlencoded"))
    fun transferToken(request: HttpServletRequest): ResponseEntity<String> {

        val id = request.getParameter("id")
        if(id == null){
            return ResponseEntity.badRequest().body("Query parameter 'id' must not be null.\n")
        }
        val linearId = id as UniqueIdentifier

        val newOwner = request.getParameter("newOwner")
        val partyX500Name = CordaX500Name.parse(newOwner)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $newOwner cannot be found.\n")

        return try {

            val token: StateAndRef<TokenState> = proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.linearId == linearId }.first()

            val signedTx = proxy.startTrackedFlow(::TransferInitiator, token, otherParty).returnValue.getOrThrow()

            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }






}