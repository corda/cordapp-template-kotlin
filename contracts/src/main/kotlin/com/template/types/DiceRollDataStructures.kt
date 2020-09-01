package com.template.types

import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignedData
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

//////////////////////////////////// DATA STRUCTURES

/**
 * Represents player's dice roll for given game and round.
 * Additionally we keep hash of all hands signed by enclave. So the player doesn't modify his state.
 * This structure should be signed by an oracle.
 */
@CordaSerializable
data class PlayerHand(val gameId: SecureHash, val roundId: Int, val hand: List<Int>, val hashOfRoundHands: SignedData<SecureHash>)

/**
 * This is data structure that needs to be sent back to the oracle to inform about who lost in given round.
 * Player that lost is identified by [lostKey]
 * // TODO should be signed by all participants
 */
@CordaSerializable
data class RoundEndState(val gameId: SecureHash, val roundId: Int, val lostKey: PublicKey)