package com.template.flows.game

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.flows.getGameByGameId
import com.template.flows.getRoll
import com.template.flows.notary
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

/**
 * This is called via RPC only.
 */
@StartableByRPC
@InitiatingFlow
class MakeCallInitiator(val gameId: SecureHash) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val inputStateAndRef = getGameByGameId(gameId)
                ?: throw IllegalArgumentException("Cannot find game with id $gameId.")
        val gameState = inputStateAndRef.state.data
        val currentRound = gameState.currentRound
        val roll = getRoll(gameId, currentRound)
        val output = inputStateAndRef.state.data.perudo(ourIdentity, roll)
        val transaction = TransactionBuilder(notary = notary()).apply {
            addInputState(inputStateAndRef)
            addOutputState(output)
            addCommand(GameContract.GameCommands.MakeCall(), ourIdentity.owningKey)
        }
        val signedTransaction = serviceHub.signInitialTransaction(transaction)
        // We send the updated state to all players.
        val otherPlayers = output.participants - ourIdentity
        val otherPlayerSessions = otherPlayers.map { initiateFlow(it) }
        subFlow(FinalityFlow(signedTransaction, otherPlayerSessions, StatesToRecord.ONLY_RELEVANT))
    }
}

/**
 * Just stores the new game state.
 */
@InitiatedBy(MakeCallInitiator::class)
class MakeCallResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(otherSession))
    }
}