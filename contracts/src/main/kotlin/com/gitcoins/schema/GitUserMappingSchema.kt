package com.gitcoins.schema

import net.corda.core.schemas.MappedSchema
import java.io.Serializable
import javax.persistence.*

object GitUserMappingSchema

object GitUserMappingSchemaV1 : MappedSchema (
        schemaFamily = GitUserMappingSchema.javaClass,
        version = 1,
        mappedTypes = listOf(GitUserMapping::class.java)) {

    @Entity
    @Table(name="GUM")
    class GitUserMapping(
            @Id
            @Column(name = "id", unique = true, nullable = false)
            val key: String,

            @Column(name="git_user_name", nullable = false)
            var gitUserName: String,

            @Column(name="user_key", nullable = false)
            var userKey: ByteArray
    ) : Serializable {
        constructor(): this("", "", ByteArray(1))
    }
}