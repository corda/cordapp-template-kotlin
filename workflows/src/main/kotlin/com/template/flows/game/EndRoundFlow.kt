package com.template.flows.game

import ProvideInformationOnEndRoundState
import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.flows.getGameByGameId
import com.template.flows.notary
import com.template.types.RoundEndState
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignedData
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class EndRoundFlow(val gameId: SecureHash) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val inputStateAndRef = getGameByGameId(gameId)
                ?: throw IllegalArgumentException("Cannot find game with id $gameId.")
        val previousGame = inputStateAndRef.state.data
        val loserKey = previousGame.loserKey()
        val output = inputStateAndRef.state.data.endRound()
        val transaction = TransactionBuilder(notary = notary()).apply {
            addInputState(inputStateAndRef)
            addOutputState(output)
            addCommand(GameContract.GameCommands.EndRound(), ourIdentity.owningKey)
        }
        val signedTransaction = serviceHub.signInitialTransaction(transaction)
        // We send the updated state to all players.
        val otherPlayers = output.participants - ourIdentity
        val otherPlayerSessions = otherPlayers.map { initiateFlow(it) }
        val ftx = subFlow(FinalityFlow(signedTransaction, otherPlayerSessions, StatesToRecord.ONLY_RELEVANT))
        // TODO it's a bit of a hack, but works for hackathon, later we should implement better integration
        val endRoundObj = RoundEndState(gameId, previousGame.currentRound, loserKey)
        val signature = serviceHub.keyManagementService.sign(endRoundObj.serialize().bytes, ourIdentity.owningKey)
        val signedData = SignedData(endRoundObj.serialize(), signature)
        subFlow(ProvideInformationOnEndRoundState(signedData, output.oracle))
        return ftx
    }
}

/**
 * Just stores the new game state.
 */
@InitiatedBy(EndRoundFlow::class)
class EndRoundFlowResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(otherSession))
    }
}