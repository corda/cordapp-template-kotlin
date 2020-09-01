package com.template.flows.dice

import net.corda.core.crypto.newSecureRandom
import java.security.PublicKey

/**
 * This is super secret sauce and should be done by enclave
 */
class DiceRollProvider {
    private val secureRandom = newSecureRandom()

    fun rollDice(): Int {
        return secureRandom.nextInt(6) + 1
    }

    fun generatePlayerHand(handSize: Int): List<Int> {
        val playerHand = mutableListOf<Int>()
        for (i in 1..handSize) playerHand.add(rollDice())
        return playerHand
    }

    fun generateAllPlayerHands(playerNumberOfDice: MutableMap<PublicKey, Int>): MutableMap<PublicKey, List<Int>> {
        val playerHands = mutableMapOf<PublicKey, List<Int>>()
        for (entry in playerNumberOfDice) {
            playerHands[entry.key] = generatePlayerHand(entry.value)
        }
        return playerHands
    }
}