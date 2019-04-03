package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.gitcoins.schema.GitUserMappingSchemaV1
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.workflow.flows.IssueToken
import com.gitcoins.states.GitToken
import net.corda.core.crypto.Crypto
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.base58ToByteArray

/**
 * Flow that delegates the issuing of a [GitToken] to the [IssueToken] subflow. This flow is triggered by a GitHub pull
 * request.
 */
@StartableByRPC
class PullRequestReviewEventFlow(private val gitUserName: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call() : SignedTransaction {

        val result = subFlow(QueryGitUserDatabaseFlow(gitUserName))
        if(result.isEmpty()) {
            throw FlowException("No public key for git username '$gitUserName'. \n " +
                    "Please comment 'createKey' on a PR to generate a public key for '$gitUserName'.")
        }

        val token = GitToken()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val key = Crypto.decodePublicKey(result.first().userKey)

        // TODO Use IssueToken.Initiator once the fix that allows a node to issue to itself has gone in
        // val anon = AnonymousParty(key)
        // val party = serviceHub.identityService.wellKnownPartyFromAnonymous(anon)

        // TODO Implement evaluation logic based on the commit

        // Initiator flow logic goes here.
        return subFlow(IssueTokenToKey.Initiator(token, key, notary, 1 of token, anonymous = false))
    }
}

