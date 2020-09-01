package com.template.flows.admin

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ProposalResponse
import com.template.contracts.ProposalResponseContract
import com.template.flows.getGameByProposalId
import com.template.flows.notary
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ProposalResponseFlow(
        private val proposalId: UniqueIdentifier,
        private val response: ProposalResponse.Response
) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val game = getGameByProposalId(proposalId)
                ?: throw IllegalArgumentException("Game proposal $proposalId not found.")
        val gameProposer = game.state.data.proposer
        val proposalResponse = ProposalResponse(proposalId, response, gameProposer, ourIdentity)
        // Create transaction.
        val utx = TransactionBuilder(notary()).apply {
            addOutputState(proposalResponse, ProposalResponseContract.ID)
            // TODO: Tidy up this nested class...
            addCommand(ProposalResponseContract.ProposalResponseCommands.CreateResponse(), listOf(ourIdentity.owningKey))
        }
        val stx = serviceHub.signInitialTransaction(utx)
        // The tentative players become state observers.
        val proposerSession = initiateFlow(gameProposer)
        subFlow(FinalityFlow(stx, proposerSession))
    }
}


@InitiatedBy(ProposalResponseFlow::class)
class ProposalResponseHelperFlow(val counterparty: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Proposer is a participant to should store this state by default.
        subFlow(ReceiveFinalityFlow(counterparty, null))
    }
}