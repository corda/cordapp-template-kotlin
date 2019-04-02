package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.gitcoins.schema.GitUserMappingSchemaV1
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.workflow.flows.IssueToken
import com.gitcoins.states.GitToken
import net.corda.core.crypto.Crypto
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.base58ToByteArray
import java.security.PublicKey

/**
 * Flow that delegates the issuing of a [GitToken] to the [IssueToken] subflow. This flow is triggered by a GitHub push
 * event that is linked to a github webhook.
 */
@StartableByRPC
class PushEventFlow(private val gitUserName: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call() : SignedTransaction {

        val result: MutableList<GitUserMappingSchemaV1.GitUserMapping> = serviceHub.withEntityManager {
            val query = criteriaBuilder.createQuery(GitUserMappingSchemaV1.GitUserMapping::class.java)
            val gitUserMapping = query.from(GitUserMappingSchemaV1.GitUserMapping::class.java)
            query.where(criteriaBuilder.equal(gitUserMapping.get<String>("gitUserName"), gitUserName))
            createQuery(query).resultList
        }

        if(result.isEmpty()) {
            throw FlowException("No public key for git username $gitUserName.")
        }

        val token = GitToken()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val key = Crypto.decodePublicKey(result.first().userKey)

        //TODO Implement evaluation logic based on the commit

        return subFlow(IssueTokenToKey.Initiator(token, key, notary, 1 of token, anonymous = false))
    }
}

