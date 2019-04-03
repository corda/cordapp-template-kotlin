package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.gitcoins.schema.GitUserMappingSchemaV1
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

/**
 * Simple flow that will generate a [PublicKey] when given a GitHub username and store them off-ledger in a
 * [GitUserMappingSchemaV1.GitUserMapping] table. If the given username is found in the table then a [FlowException] is
 * thrown.
 */
@StartableByRPC
class CreateKeyFlow(private val gitUserName: String) : FlowLogic<Unit>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call() {
        // Check there is a key for the username
        val result = subFlow(QueryGitUserDatabaseFlow(gitUserName))

        if (result.isNotEmpty() && result.first().userKey != null)
            throw FlowException("Public key for this github user: $gitUserName already exists")

        val keyAndCert = serviceHub.keyManagementService.freshKeyAndCert(ourIdentityAndCert, false)
        serviceHub.identityService.verifyAndRegisterIdentity(keyAndCert)
        val key = keyAndCert.owningKey
        serviceHub.withEntityManager {
            persist(GitUserMappingSchemaV1.GitUserMapping(UniqueIdentifier().id.toString(), gitUserName, key.encoded))
        }
    }

}