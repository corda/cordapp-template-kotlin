package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.gitcoins.schema.GitUserMappingSchemaV1
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.toStringShort
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import org.jetbrains.annotations.TestOnly

/**
 * TODO
 */
@StartableByRPC
class CreateKeyFlow(private val gitUserName: String) : FlowLogic<Unit>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call() {
        // Check there is no key for username already
        val result: MutableList<GitUserMappingSchemaV1.GitUserMapping> = serviceHub.withEntityManager {
            val query = criteriaBuilder.createQuery(GitUserMappingSchemaV1.GitUserMapping::class.java)
            val gitUserMapping = query.from(GitUserMappingSchemaV1.GitUserMapping::class.java)
            query.where(criteriaBuilder.equal(gitUserMapping.get<String>("gitUserName"), gitUserName))
            createQuery(query).resultList
        }

        if (result.isNotEmpty())
            throw FlowException("Public key for this github user: $gitUserName already exists")

        val keyAndCert = serviceHub.keyManagementService.freshKeyAndCert(ourIdentityAndCert, false)
        serviceHub.identityService.verifyAndRegisterIdentity(keyAndCert)
        val key = keyAndCert.owningKey
        val gum = GitUserMappingSchemaV1.GitUserMapping(
                UniqueIdentifier().id.toString(),
                gitUserName, key.encoded)
        serviceHub.withEntityManager {
            persist(gum)
        }
    }

}