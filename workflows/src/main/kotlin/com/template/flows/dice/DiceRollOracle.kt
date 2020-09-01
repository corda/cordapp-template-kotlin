import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.Game
import com.template.flows.dice.DiceRollProvider
import com.template.flows.dice.PersistentPayerHand
import com.template.types.PlayerHand
import com.template.types.RoundEndState
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignedData
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.serialization.serialize
import net.corda.core.utilities.toBase58String
import net.corda.core.utilities.unwrap
import java.security.PublicKey
import java.util.*

// TODO I promise I will make this code nice and tidy :D especially internal state bits
// TODO it will be structured as a Corda Service

// TODO attacks:
// * REPLAY - this one is mitigated by locking the round with a hash of all dice rolls for that round
// * MEMENTO - we lock game id in the dice roll too, so we won't reuse it! we also lock the sequence
// * IMPERSONATING ANOTHER NODE - this will be mitigated by keys and signatures from participants, on requesting dice roll for instance
// * requesting more die than we can? need to do the end game state right and should be fine
// * don't reveal the state of the game to other players (so they can't peek other players dices)

//////////////////////////////////// START GAME
/**
 * Initial flow to be called when initiating new game with new participants, it needs new game id - secure hash, and list of participants.
 * Called by game proposer.
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class RequestNewGameFlow(
        // TODO receive it as a state? signed by everyone
        val gameState: Game, // TODO maybe for now let's have it signed by just proposer, later we should sign it with all participants keys
        val enclaveNode: Party
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // TODO later change to state ref
        // Sign with our main key
        val serialisedGameState = gameState.serialize()
        val signature = serviceHub.keyManagementService.sign(serialisedGameState.bytes, ourIdentity.owningKey)
        val signedData = SignedData(serialisedGameState, signature)
        val enclaveSession = initiateFlow(enclaveNode)
        enclaveSession.send(signedData)
    }
}

@InitiatedBy(RequestNewGameFlow::class)
class RequestNewGameFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedGameState = otherSession.receive<SignedData<Game>>().unwrap { it } // TODO verify it inside unwrap
        val gameState = signedGameState.verified()

        val oracle = serviceHub.cordaService(OracleService::class.java)
        oracle.initateGame(gameState)
        // TODO check that participants signed the signeGameHash
    }
}

//////////////////////////////////// END ROUND

/**
 * This is a flow called on the end of each round, so the enclave can update how many dice each player has!
 * It is suposed to be called just once by one of the participants - ideally proposer
 * TODO make sure it was called just once!
 * @param endRoundState should be signed by all participants
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class ProvideInformationOnEndRoundState(
        val endRoundState: SignedData<RoundEndState>,
        val enclaveNode: Party
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // TODO end round state should be signed by all participants
        val enclaveSession = initiateFlow(enclaveNode)
        enclaveSession.send(endRoundState)
    }
}

@InitiatedBy(ProvideInformationOnEndRoundState::class)
class ProvideInformationOnEndRoundStateHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedEndRoundState = otherSession.receive<SignedData<RoundEndState>>().unwrap { it } // TODO verify it inside unwrap
        val endRoundState = signedEndRoundState.verified()
        val oracle = serviceHub.cordaService(OracleService::class.java)
        oracle.endRound(endRoundState)
        // TODO check that participants signed the end round state
    }
}

//////////////////////////////////// NEXT ROUND
/**
 * This flow gets called by each participant so they obtain the hand for next round.
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class NextRoundFlow(
        val gameId: SecureHash,
        val roundId: Int,
        val enclaveNode: Party
) : FlowLogic<SignedData<PlayerHand>>() {
    @CordaSerializable
    data class RequestPlayerHand(val gameId: SecureHash, val roundId: Int, val playerKey: PublicKey, val uuid: UUID?)

    @Suspendable
    override fun call(): SignedData<PlayerHand> {
        val enclaveSession = initiateFlow(enclaveNode)
        val request = RequestPlayerHand(gameId, roundId, ourIdentity.owningKey, null)
        val serialisedRequest = request.serialize()
        // TODO we need to shield ourselves from someone taking over that request and replaying it to enclave! we need challenge respnse, should be implemented later
        val signatureOverDeserialisedRequest = serviceHub.keyManagementService.sign(serialisedRequest.bytes, ourIdentity.owningKey)
        val signedData = SignedData(serialisedRequest, signatureOverDeserialisedRequest)
        enclaveSession.send(signedData)
        val signedPlayerHand = enclaveSession.receive<SignedData<PlayerHand>>().unwrap { it }
        if (signedPlayerHand.sig.by != enclaveNode.owningKey) {
            throw IllegalArgumentException("Someone tried to impersonate the enclave")
        }
        println("Persisitng player hand into DB")
        val playerHand = signedPlayerHand.verified()
        serviceHub.withEntityManager { persist(PersistentPayerHand(playerHand)) }
        return signedPlayerHand
        // TODO impersonation attack, replay attack
    }
}

@InitiatedBy(NextRoundFlow::class)
class NextRoundFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // TODO we should challenge response here so no one replays that request message (basically we put UUID into the message that needs to be signed)
        val signedRequest = otherSession.receive<SignedData<NextRoundFlow.RequestPlayerHand>>().unwrap { it }
        val request = signedRequest.verified()
        check(signedRequest.sig.by == request.playerKey) { "Signature mismatch" }

        val oracle = serviceHub.cordaService(OracleService::class.java)
        val signedPlayerHand = oracle.getPlayerHand(request.gameId, request.roundId, request.playerKey, request.uuid)
        otherSession.send(signedPlayerHand)
    }
}

@CordaService
class OracleService(private val services: AppServiceHub) : SingletonSerializeAsToken() {
    // To keep track of the all games ideally in database, but it's hackathon ;)
    // TODO for now i don't keep old game states, we could do that if we had DB storage, but i optimise for enclave
    val internalGamesStatesMap: MutableMap<SecureHash, InternalGameState> = mutableMapOf()

    /**
     * Data structure for keeping track of what is going on in the given game
     */
    data class InternalGameState(
            val gameId: SecureHash,
            var currentRound: Int, // TODO change that var
            val playerKeys: List<PublicKey>,
            val playerNumberOfDice: MutableMap<PublicKey, Int>, // Keep how many dices each player has
            val playerHands: MutableMap<PublicKey, List<Int>> // This gets generated by enclave - important bit!!
    ) {
        fun lockHash(): SecureHash {
            // TODO theoretically we should sort the hands too, but because it's list, we shouldn't experience change of order at demo
            // and for demo purposes i prefer when dices are random, not sorted
            val sortedMap = playerHands.toSortedMap(kotlin.Comparator { x, y -> x.toBase58String().compareTo(y.toBase58String()) })
            val mapHash = sortedMap.serialize().hash
            return mapHash
        }

        companion object {
            /**
             * Provider of randomness
             */
            val diceRollProvider = DiceRollProvider()

            fun newInternalState(gameState: Game): InternalGameState {
                val gameId = gameState.gameId ?: throw IllegalArgumentException("Game id must me not null")
                val playerKeys = gameState.players.keys.map { it.owningKey }
                val playerNumberOfDice = mutableMapOf<PublicKey, Int>()
                playerKeys.associateTo(playerNumberOfDice) { it to 5 }
                val playerHands = diceRollProvider.generateAllPlayerHands(playerNumberOfDice)
                return InternalGameState(
                        gameId = gameId,
                        currentRound = 1,           // Round starts at 1.
                        playerKeys = playerKeys,
                        playerNumberOfDice = playerNumberOfDice, // We start with 5 dices
                        playerHands = playerHands // We start with empty hands
                )
            }
        }

        fun updateToNewRound(lostKey: PublicKey) {
            with(this) {
                currentRound = currentRound + 1
                playerNumberOfDice.computeIfPresent(lostKey) { k, v -> if (v > 0) v - 1 else 0 }
                val newPlayerHands = diceRollProvider.generateAllPlayerHands(playerNumberOfDice)
                playerHands.forEach {
                    playerHands[it.key] = newPlayerHands[it.key]!!
                }
            }
        }
    }


    @Suspendable
    fun initateGame(gameState: Game) {
        val gameId = gameState.gameId ?: throw IllegalArgumentException("Game id must me not null")
        check(gameState.players.size == 4) { "Game should have 4 participants exactly" }
        check(gameId !in internalGamesStatesMap) { "Game with id $gameId was already initiated in this enclave" }
        val initialInternalGameState = InternalGameState.newInternalState(gameState)
        internalGamesStatesMap.put(gameId, initialInternalGameState)
    }

    @Suspendable
    fun endRound(endRoundState: RoundEndState) {
        val gameId = endRoundState.gameId
        // TODO Stuff below extract out to enclave API
        check(gameId in internalGamesStatesMap) { "Game with id $gameId isn't know to enclave" }
        val internalGameState = internalGamesStatesMap[gameId]!!
        // Check that the sequence id is correct! (so someone doesn't send it twice and decreases number of dices for a player ;)
        check(endRoundState.roundId == internalGameState.currentRound) { "End state information got sent with different round number" }
        // We need to move the game round, decrement dices, zero the hands
        internalGamesStatesMap[gameId]!!.updateToNewRound(endRoundState.lostKey) // TODO test it gets updated in place
    }

    @Suspendable
    fun getPlayerHand(gameId: SecureHash, roundId: Int, playerKey: PublicKey, uuid: UUID?): SignedData<PlayerHand> {
        val oracleIdentity = services.myInfo.legalIdentities.first().owningKey
        check(gameId in internalGamesStatesMap) { "Game with id ${gameId} isn't know to enclave" }
        val internalGameState = internalGamesStatesMap[gameId]!!
        check(internalGameState.currentRound == roundId) { "Rounds don't align" }
        val hand = internalGameState.playerHands[playerKey]!!
        // sign hands hash
        val lockHash: SecureHash = internalGameState.lockHash()
        val signedLockHash = services.keyManagementService.sign(lockHash.serialize().bytes, oracleIdentity)
        val playerHandObject = PlayerHand(internalGameState.gameId, internalGameState.currentRound, hand, SignedData(lockHash.serialize(), signedLockHash))
        // THAT signature API is so awkward
        val signedPlayerHandObject = services.keyManagementService.sign(playerHandObject.serialize().bytes, oracleIdentity)
        return SignedData(playerHandObject.serialize(), signedPlayerHandObject)
    }
}