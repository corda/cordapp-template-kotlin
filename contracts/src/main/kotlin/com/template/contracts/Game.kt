package com.template.contracts

import com.template.schemas.GameSchemaV1
import com.template.schemas.PersistentGame
import com.template.types.PlayerHand
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@BelongsToContract(GameContract::class)
data class Game(

        /** GAME ADMIN */

        /** The game proposer. */
        val proposer: Party,
        /** Generate this externally to the sandbox. */
        val proposalId: UniqueIdentifier,
        /** The Dice Oracle. */
        val oracle: Party,

        /** GAME STATE */

        /** The current status of the game. */
        val status: Status = Status.PROPOSING,
        /** Keeps track of who's playing and how many dice they have left. Mutable as we need to update entries. */
        val players: LinkedHashMap<Party, Int> = linkedMapOf(),
        /** This is set when the game moves from PROPOSING to STARTING state. */
        val gameId: SecureHash? = null,
        /** The current round of the game. */
        val currentRound: Int = 0,

        /** ROUND STATE */

        /** The number of turns so far for this round. */
        val turnNumber: Int = 0,
        /** Keeps track of the dice rolls for each round. */
        val rolls: Map<Party, PlayerHand> = emptyMap(),
        /** Each round has a number and is associated with an ordered list of turns. */
        val currentBid: Turn? = null,
        /** The player who called "Perudo!". */
        val caller: Party? = null
) : LinearState, QueryableState {

    companion object {
        const val MAX_DICE = 5
        /** Set up a game state ready to prose a new game. */
        fun proposeGame(proposer: Party, linearId: UniqueIdentifier, oracle: Party): Game {
            return Game(proposer, linearId, oracle, players = linkedMapOf(proposer to MAX_DICE))
        }
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState = when (schema) {
        is GameSchemaV1 -> PersistentGame(
                proposer = proposer,
                oracle = oracle,
                gameId = gameId.toString(),
                status = status,
                currentRound = currentRound
        )
        else -> throw java.lang.IllegalArgumentException("Unrecognised schema $schema")
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(GameSchemaV1)

    /**
     * This list starts with only the proposer but the three other players are added once they accept the invitation to
     * play the game.
     */
    override val participants: List<AbstractParty> get() = players.keys.map { it }

    override val linearId: UniqueIdentifier get() = proposalId

    val stillPlaying: List<Party> get() = players.filterValues { it > 0 }.keys.toList()

    /** Calculates the next player based upon the turn number for the round. */
    val nextTurn: Party
        // TODO: this maybe has a bug because when someone is out of the game then the counter might jump - need to test it.
        get() {
            val index = turnNumber % stillPlaying.size
            return stillPlaying[index]
        }

    @CordaSerializable
    enum class Status {
        PROPOSING,  // When a party has proposed a game but the remaining players have not yet been added.
        BIDDING,    // When we are accepting bids from each player in a cycle.
        CALLING,    // When someone calls "perudo" to check the dice and start a new round.
        END         // When only one party have dice left who is the winner.
    }

    /** Represnts the roll of a set of dice from the encalve. Only added once a game has been called. */
    // TODO: Need to add more here.
    @CordaSerializable
    data class Roll(val roll: List<Int>)

    /**
     * A player can increase the quantity of dice (e.g. from "five threes" to "six threes") or the die number (e.g.
     * "five threes" to "five sixes") or both. If a player increases the quantity, they can choose any number e.g.
     * a bid may increase from "five threes" to "six twos".
     */
    @CordaSerializable
    data class Bid(val number: Int, val die: Int) {
        init {
            check(number in 1..6) { "Number must be between 0 and 6." }
            check(die in 1..6) { "Die must be between 0 and 6." }
        }
    }

    /**
     * To start the game the hash of the Game state when it was in a proposal state must be provided. The Contract will
     * check the correct hash was provided.
     */
    fun startGame(prevStateHash: SecureHash, otherPlayers: List<Party>): Game {
        val playersAndDice = otherPlayers.map { it to MAX_DICE }.toMap()
        val newPlayers = players + playersAndDice
        return copy(
                gameId = prevStateHash,
                players = LinkedHashMap(newPlayers)
        ).newRound()
    }

    /**
     * Starting a new round increments the round counter and purges the turns list and rolls list which was created
     * during the previous round. Caller is also set to null.
     */
    private fun newRound(newTurnNumber: Int = 0): Game {
        return copy(
                currentBid = null,
                rolls = emptyMap(),
                status = Status.BIDDING,
                turnNumber = newTurnNumber,                 // Last person to lose starts the next round.
                caller = null
        ).incrementRoundNumber()
    }

    /**
     * IMPORTANT - not ideal but need to make sure this is called when updating the state!
     */
    private fun incrementTurnNumber(): Game {
        return copy(turnNumber = turnNumber + 1)
    }

    private fun incrementRoundNumber(): Game {
        return copy(currentRound = currentRound + 1)
    }

    private fun checkEnd(): Boolean {
        return stillPlaying.size == 1
    }

    /**
     * Updates the current bid.
     */
    fun bid(me: Party, bid: Bid): Game {
        val bidMap = me to bid
        return copy(currentBid = bidMap).incrementTurnNumber()
    }

    /**
     * Ends the round and gets everyone to reveal their dice.
     */
    fun perudo(me: Party, roll: PlayerHand): Game {
        return copy(status = Status.CALLING, caller = me).revealDice(me, roll)
    }

    /**
     * Adds your dice roll to the state.
     */
    fun revealDice(me: Party, roll: PlayerHand): Game {
        val myRoll = me to roll
        return copy(rolls = rolls + myRoll).incrementTurnNumber()
    }

    private fun calculateLoser(): Party {
        // Check we are good to go.
        val numberOfRollsRevealed = rolls.size
        check(numberOfRollsRevealed == stillPlaying.size) { "Only $numberOfRollsRevealed rolls revealed." }
        if (caller == null) throw IllegalStateException("Caller not set.")
        if (currentBid == null) throw IllegalStateException("There's no current bid!")
        // Merge all rolls into one.
        val allRolls = rolls.values.flatMap { it.hand }
        // Get all the stuff we need.
        val die = currentBid.second.die
        val guess = currentBid.second.number
        val bidder = currentBid.first
        val caller = caller
        // Do the calculation.
        val filteredRolls = allRolls.filter { it == die }
        return if (filteredRolls.size < guess) {
            bidder
        } else {
            caller
        }
    }

    fun loserKey(): PublicKey = calculateLoser().owningKey

    /**
     * This is called to remove a die from the loser. T he contract checks that the die was removed from the correct
     * player by looking at all the dice and seeing if there was at least the amount specified or whether there were
     * less. The player to call perudo is stored in the game state.
     */
    fun endRound(): Game {
        val loser = calculateLoser()
        // What's going on here? For some reason the map is not mutable even though it is declared as such.
        players.computeIfPresent(loser) { _, diceLeft -> diceLeft - 1 }
        return when (checkEnd()) {
            true -> copy(status = Status.END)
            false -> {
                // The last person to lose starts the next round.
                val losingIndex = players.keys.indexOf(loser)
                newRound(losingIndex)
            }
        }
    }
}

