package com.template.flows.admin

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.Game
import com.template.contracts.GameContract
import com.template.flows.notary
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ProposeGameFlow(private val players: List<Party>, val oracle: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val game = Game.proposeGame(proposer = ourIdentity, linearId = UniqueIdentifier(), oracle = oracle)
        val utx = TransactionBuilder(notary()).apply {
            addOutputState(game, GameContract.ID)
            addCommand(GameContract.GameCommands.ProposeGame(), listOf(ourIdentity.owningKey))
        }
        val stx = serviceHub.signInitialTransaction(utx)
        // The tentative players become state observers.
        val otherPlayerSessions = players.map { initiateFlow(it) }
        return subFlow(FinalityFlow(stx, otherPlayerSessions))
    }
}


@InitiatedBy(ProposeGameFlow::class)
class ProposeGameHelperFlow(val counterparty: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(counterparty, null, StatesToRecord.ALL_VISIBLE))
    }
}

