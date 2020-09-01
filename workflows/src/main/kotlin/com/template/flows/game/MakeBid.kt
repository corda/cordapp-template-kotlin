package com.template.flows.game

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.Game
import com.template.contracts.GameContract
import com.template.flows.getGameByGameId
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
class MakeBidInitiator(
        val gameId: SecureHash,
        val bid: Game.Bid
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val inputStateAndRef = getGameByGameId(gameId)
                ?: throw IllegalArgumentException("Cannot find game with id $gameId.")
        val output = inputStateAndRef.state.data.bid(ourIdentity, bid)
        val transaction = TransactionBuilder(notary = notary()).apply {
            addInputState(inputStateAndRef)
            addOutputState(output)
            addCommand(GameContract.GameCommands.MakeBid(), ourIdentity.owningKey)
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
@InitiatedBy(MakeBidInitiator::class)
class MakeBidResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(otherSession))
    }
}