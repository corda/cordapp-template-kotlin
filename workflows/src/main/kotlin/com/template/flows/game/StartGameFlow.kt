package com.template.flows.game

import RequestNewGameFlow
import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.flows.getGameByProposalId
import com.template.flows.notary
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class StartGameFlow(val proposalId: UniqueIdentifier, val otherPlayers: List<Party>) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val gameStateAndRef = getGameByProposalId(proposalId)
                ?: throw IllegalArgumentException("Game proposal $proposalId not found.")
        val gameRef = gameStateAndRef.ref
        val startedGameState = gameStateAndRef.state.data.startGame(gameRef.txhash, otherPlayers)
        val utx = TransactionBuilder(notary()).apply {
            addInputState(gameStateAndRef)
            addOutputState(startedGameState, GameContract.ID)
            val signers = (otherPlayers + ourIdentity).map { it.owningKey }
            addCommand(GameContract.GameCommands.StartGame(), signers)
        }
        val ptx = serviceHub.signInitialTransaction(utx)
        val sessions = otherPlayers.map { initiateFlow(it) }
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        val ftx = subFlow(FinalityFlow(stx, sessions))
        subFlow(RequestNewGameFlow(startedGameState, startedGameState.oracle))
        return ftx
    }
}

@InitiatedBy(StartGameFlow::class)
class StartGameHelperFlow(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // TODO: Add some checking here.
            }
        })
        // All parties are participants so should store this state by default.
        subFlow(ReceiveFinalityFlow(otherSession, null))
    }
}