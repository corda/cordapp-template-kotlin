package com.gitcoins

import com.gitcoins.flows.CreateKeyFlow
import com.gitcoins.flows.PullReviewEventFlow
import com.gitcoins.flows.PushEventFlow
import com.gitcoins.schema.GitUserMappingSchemaV1
import com.gitcoins.states.GitToken
import com.natpryce.hamkrest.assertion.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.r3.corda.sdk.token.contracts.states.FungibleToken
import com.r3.corda.sdk.token.workflow.utilities.tokenBalance
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.AbstractParty
import org.assertj.core.api.Assertions.assertThat
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.queryBy
import net.corda.core.schemas.MappedSchema
import net.corda.core.utilities.base58ToByteArray
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.hexToByteArray
import net.corda.node.services.schema.NodeSchemaService
import net.corda.nodeapi.internal.persistence.CordaPersistence
import net.corda.nodeapi.internal.persistence.DatabaseConfig
import net.corda.nodeapi.internal.persistence.HibernateConfiguration
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.internal.configureDatabase
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockServices.Companion.makeTestDataSourceProperties
import org.hibernate.SessionFactory
import net.corda.testing.core.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaBuilder
import kotlin.test.assertEquals

class GitEventFlowTests {

    private companion object {
        val dummyNode = TestIdentity(CordaX500Name("Snake Oil Issuer", "London", "GB"), 10)
        val dummyNotary = TestIdentity(DUMMY_NOTARY_NAME, 20)
    }

    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private lateinit var user: Party

    lateinit var database: CordaPersistence
    lateinit var hibernateConfig: HibernateConfiguration
    private lateinit var hibernateSession: SessionFactory
    private lateinit var entityManager: EntityManager
    private lateinit var criteriaBuilder: CriteriaBuilder

    @Before
    fun setup() {
        network = MockNetwork(
                cordappPackages = listOf(
                        "com.r3.corda.sdk.token.contracts",
                        "com.r3.corda.sdk.token.workflow"
                ),
                threadPerNode = true,
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4))

        a = network.createNode()
        b = network.createNode()
        user = b.info.singleIdentity()

        val dataSourceProps = makeTestDataSourceProperties()
        val identityService = mock<IdentityService>()
        val schemaService = NodeSchemaService(extraSchemas = setOf(GitUserMappingSchemaV1))

        database = configureDatabase(dataSourceProps, DatabaseConfig(), identityService::wellKnownPartyFromX500Name,
                identityService::wellKnownPartyFromAnonymous, schemaService)

        database.transaction {
            hibernateConfig = database.hibernateConfig
            database.createSession()
        }

        hibernateSession = sessionFactoryForSchemas(GitUserMappingSchemaV1)
        entityManager = hibernateSession.createEntityManager()
        criteriaBuilder = hibernateSession.criteriaBuilder
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    private fun sessionFactoryForSchemas(vararg schemas: MappedSchema) = hibernateConfig.sessionFactoryForSchemas(schemas.toSet())

    @Test
    fun `Create new public key for new user`() {
        val user = "gitUser"
        val query = criteriaBuilder.createQuery(GitUserMappingSchemaV1.GitUserMapping::class.java)
        val gitUserMapping = query.from(GitUserMappingSchemaV1.GitUserMapping::class.java)
        val userNamePred = criteriaBuilder.equal(gitUserMapping.get<String>("gitUserName"), user)
        query.where(userNamePred)

        val firstQuery = entityManager.createQuery(query).resultList

        assertThat(firstQuery).hasSize(0)
        val flow = CreateKeyFlow("gitUser")
        a.transaction {
            a.startFlow(flow)
        }
        val secondQuery = entityManager.createQuery(query).resultList
        assertThat(firstQuery).hasSize(1)

    }

    @Test
    fun `Issue git token from push event`() {
        val flow = PushEventFlow("gitUser")
        val signedTx = a.transaction { a.startFlow(flow) }.getOrThrow()

        assertEquals(signedTx, a.services.validatedTransactions.getTransaction(signedTx.id))

        b.transaction { b.services.validatedTransactions.trackTransaction(signedTx.id) }.getOrThrow()

        val token = signedTx.tx.outRefsOfType<FungibleToken<GitToken>>().single()
        val vaultToken = b.services.vaultService.queryBy<FungibleToken<GitToken>>().states.single()
        assertEquals(token, vaultToken)
        val tokenBalance = b.services.vaultService.tokenBalance(GitToken())
        assertEquals(1, tokenBalance.quantity)
    }


    @Test
    fun `Issue git token from pull request review event`() {
        val flow = PullReviewEventFlow("gitUser")
        val signedTx = a.transaction { a.startFlow(flow) }.getOrThrow()
        assertEquals(signedTx, a.services.validatedTransactions.getTransaction(signedTx.id))

        b.transaction { b.services.validatedTransactions.trackTransaction(signedTx.id) }.getOrThrow()

        val token = signedTx.tx.outRefsOfType<FungibleToken<GitToken>>().single()
        val vaultToken = b.services.vaultService.queryBy<FungibleToken<GitToken>>().states.single()
        assertEquals(token, vaultToken)

        val tokenBalance = b.services.vaultService.tokenBalance(GitToken())
        assertEquals(1, tokenBalance.quantity)
    }

    @Test
    fun `test key gen`() {
        val key = a.services.keyManagementService.freshKey()
        val st = key.encoded
        println(st)
        val pk = Crypto.decodePublicKey(st)
//        println(pk)
    }
}