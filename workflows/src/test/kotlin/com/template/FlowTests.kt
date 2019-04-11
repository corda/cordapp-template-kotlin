package com.template

import com.r3.corda.sdk.token.contracts.EvolvableTokenContract
import com.r3.corda.sdk.token.contracts.states.EvolvableTokenType
import com.r3.corda.sdk.token.contracts.states.FungibleToken
import com.r3.corda.sdk.token.contracts.types.TokenPointer
import com.r3.corda.sdk.token.contracts.types.TokenType
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.contracts.utilities.withNotary
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.money.GBP
import com.r3.corda.sdk.token.workflow.flows.*
import com.r3.corda.sdk.token.workflow.utilities.getLinearStateById
import com.r3.corda.sdk.token.workflow.utilities.ownedTokenAmountsByToken
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

abstract class MockNetworkTest(val names: List<CordaX500Name>) {

    constructor(vararg names: String) : this(names.map { CordaX500Name(it, "London", "GB") })

    constructor(numberOfNodes: Int) : this(*(1..numberOfNodes).map { "Party${it.toChar() + 64}" }.toTypedArray())

    protected val network = MockNetwork(
            cordappPackages = listOf(
                    "com.r3.corda.sdk.token.money",
                    "com.r3.corda.sdk.token.contracts",
                    "com.r3.corda.sdk.token.workflow",
                    "com.template"
            ),
            threadPerNode = true,
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
    )

    /** The nodes which makes up the network. */
    protected lateinit var nodes: List<StartedMockNode>
    protected lateinit var nodesByName: Map<Any, StartedMockNode>

    /** Override this to assign each node to a variable for ease of use. */
    @Before
    abstract fun initialiseNodes()

    @Before
    fun setupNetwork() {
        nodes = names.map { network.createPartyNode(it) }

        val nodeMap = LinkedHashMap<Any, StartedMockNode>()
        nodes.forEachIndexed { index, node ->
            nodeMap[index] = node
            nodeMap[node.info.chooseIdentity().name.organisation] = node
        }
        nodesByName = nodeMap
    }

    @After
    fun tearDownNetwork() {
        network.stopNodes()
    }

    fun StartedMockNode.legalIdentity() = services.myInfo.legalIdentities.first()

    protected val NOTARY: StartedMockNode get() = network.defaultNotaryNode

    /** From a transaction which produces a single output, retrieve that output. */
    inline fun <reified T : ContractState> SignedTransaction.singleOutput() = tx.outRefsOfType<T>().single()

    /** Gets the linearId from a LinearState. */
    inline fun <reified T : LinearState> StateAndRef<T>.linearId() = state.data.linearId

    /** Check to see if a node recorded a transaction with a particular hash. Return a future signed transaction. */
    fun StartedMockNode.watchForTransaction(txId: SecureHash): CordaFuture<SignedTransaction> {
        return transaction { services.validatedTransactions.trackTransaction(txId) }
    }

    fun StartedMockNode.watchForTransaction(tx: SignedTransaction): CordaFuture<SignedTransaction> {
        return watchForTransaction(tx.id)
    }

    /** Create an evolvable token. */
    fun <T : EvolvableTokenType> StartedMockNode.createEvolvableToken(evolvableToken: T, notary: Party): CordaFuture<SignedTransaction> {
        return transaction { startFlow(CreateEvolvableToken.Initiator(transactionState = evolvableToken withNotary notary)) }
    }

    /** Update an evolvable token. */
    fun <T : EvolvableTokenType> StartedMockNode.updateEvolvableToken(old: StateAndRef<T>, new: T): CordaFuture<SignedTransaction> {
        return transaction { startFlow(UpdateEvolvableToken(old = old, new = new)) }
    }

    fun <T : TokenType> StartedMockNode.issueTokens(
            token: T,
            owner: StartedMockNode,
            notary: StartedMockNode,
            amount: Amount<T>? = null,
            anonymous: Boolean = true
    ): CordaFuture<SignedTransaction> {
        return transaction {
            startFlow(IssueToken.Initiator(
                    token = token,
                    owner = owner.legalIdentity(),
                    notary = notary.legalIdentity(),
                    amount = amount,
                    anonymous = anonymous
            ))
        }
    }

    fun <T : TokenType> StartedMockNode.moveTokens(
            token: T,
            owner: StartedMockNode,
            amount: Amount<T>? = null,
            anonymous: Boolean = true
    ): CordaFuture<SignedTransaction> {
        return transaction {
            startFlow(MoveToken.Initiator(
                    ownedToken = token,
                    owner = owner.legalIdentity(),
                    amount = amount,
                    anonymous = anonymous
            ))
        }

    }

    fun <T : TokenType> StartedMockNode.redeemTokens(
            token: T,
            issuer: StartedMockNode,
            amount: Amount<T>? = null,
            anonymous: Boolean = true
    ): CordaFuture<SignedTransaction> {
        return startFlow(RedeemToken.InitiateRedeem(
                ownedToken = token,
                issuer = issuer.legalIdentity(),
                amount = amount,
                anonymous = anonymous
        ))
    }
}

// A token representing a house on ledger.
@BelongsToContract(HouseContract::class)
data class House(
        val address: String,
        val valuation: Amount<FiatCurrency>,
        override val maintainers: List<Party>,
        override val displayTokenSize: BigDecimal = BigDecimal.TEN,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType()

// TODO: When contract scanning bug is fixed then this does not need to implement Contract.
class HouseContract : EvolvableTokenContract(), Contract {

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        // Not much to do for this example token.
        val newHouse = tx.outputStates.single() as House
        newHouse.apply {
            require(valuation > Amount.zero(valuation.token)) { "Valuation must be greater than zero." }
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val oldHouse = tx.inputStates.single() as House
        val newHouse = tx.outputStates.single() as House
        require(oldHouse.address == newHouse.address) { "The address cannot change." }
        require(newHouse.valuation > Amount.zero(newHouse.valuation.token)) { "Valuation must be greater than zero." }
    }

}

class TokenFlowTests : MockNetworkTest(numberOfNodes = 3) {

    lateinit var A: StartedMockNode
    lateinit var B: StartedMockNode
    lateinit var I: StartedMockNode

    @Before
    override fun initialiseNodes() {
        A = nodes[0]
        B = nodes[1]
        I = nodes[2]
    }

    @Test
    fun `create evolvable token, then issue and move`() {
        // Create new evolvable token type.
        val house = House("24 Leinster Gardens, Bayswater, London", 1_000_000.GBP, listOf(I.legalIdentity()))
        val createTokenTx = I.createEvolvableToken(house, NOTARY.legalIdentity()).getOrThrow()
        val houseTokenType: StateAndRef<House> = createTokenTx.singleOutput()

        // Issue amount of the token.
        val housePointer: TokenPointer<House> = house.toPointer()
        val issueTokenTx = I.issueTokens(housePointer, A, NOTARY, 100 of housePointer).getOrThrow()
        val houseTokens: StateAndRef<FungibleToken<TokenPointer<House>>> = issueTokenTx.singleOutput()
        A.watchForTransaction(issueTokenTx.id).toCompletableFuture().getOrThrow()
        val houseTokenTypeQueryA = A.transaction { A.services.vaultService.getLinearStateById<LinearState>(housePointer.pointer.pointer) }
        assertEquals(houseTokenTypeQueryA!!, houseTokenType)
        val houseQueryA = A.transaction { A.services.vaultService.ownedTokenAmountsByToken(housePointer).states.single() }
        assertEquals(houseQueryA, houseTokens)

        // Move some of the tokens.
        val moveTokenTx = A.moveTokens(housePointer, B, 100 of housePointer, anonymous = true).getOrThrow()
        val movedHouseTokens: StateAndRef<FungibleToken<TokenPointer<House>>> = moveTokenTx.singleOutput()
        B.watchForTransaction(moveTokenTx.id).getOrThrow()
        val houseTokenTypeQueryB = B.transaction { B.services.vaultService.getLinearStateById<LinearState>(housePointer.pointer.pointer) }
        assertEquals(houseTokenTypeQueryB!!, houseTokenType)
        val houseQueryB = B.transaction { B.services.vaultService.ownedTokenAmountsByToken(housePointer).states.single() }
        assertEquals(houseQueryB, movedHouseTokens)
    }
}