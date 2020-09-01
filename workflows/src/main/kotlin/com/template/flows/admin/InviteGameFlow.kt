package com.template.flows.admin

import co.paralleluniverse.fibers.Suspendable
import com.template.flows.getGameByProposalId
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class InviteToGameFlow(private val proposalId: UniqueIdentifier, private val player: Party) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val gameStateAndRef = getGameByProposalId(proposalId)
                ?: throw IllegalArgumentException("Game proposal $proposalId not found.")
        val txId = gameStateAndRef.ref.txhash
        val tx = serviceHub.validatedTransactions.getTransaction(gameStateAndRef.ref.txhash)
                ?: throw IllegalArgumentException("Transaction $txId not found.")
        val playerSession = initiateFlow(player)
        subFlow(SendTransactionFlow(playerSession, tx))
    }
}


@InitiatedBy(InviteToGameFlow::class)
class InviteToGameHelperFlow(val counterparty: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(counterparty, true, StatesToRecord.ALL_VISIBLE))
    }
}

