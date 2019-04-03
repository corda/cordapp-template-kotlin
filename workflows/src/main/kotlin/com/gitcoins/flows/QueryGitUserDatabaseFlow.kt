package com.gitcoins.flows

import com.gitcoins.schema.GitUserMappingSchemaV1
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

/**
 * Utility flow to query the [GitUserMappingSchemaV1.GitUserMapping] table.
 */
@StartableByRPC
class QueryGitUserDatabaseFlow(private val gitUserName: String) : FlowLogic<MutableList<GitUserMappingSchemaV1.GitUserMapping>>() {

    @Throws(FlowException::class)
    override fun call() : MutableList<GitUserMappingSchemaV1.GitUserMapping> {

        val result: MutableList<GitUserMappingSchemaV1.GitUserMapping> = serviceHub.withEntityManager {
            val query = criteriaBuilder.createQuery(GitUserMappingSchemaV1.GitUserMapping::class.java)
            val gitUserMapping = query.from(GitUserMappingSchemaV1.GitUserMapping::class.java)
            query.where(criteriaBuilder.equal(gitUserMapping.get<String>("gitUserName"), gitUserName))
            createQuery(query).resultList
        }
        return result
    }
}