package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.workflow.flows.IssueToken
import com.gitcoins.states.GitToken
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@StartableByRPC
class PushEventFlow(val user: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    @Throws(FlowException::class)
    override fun call() : SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val token = GitToken()

        // Initiator flow logic goes here.
        return subFlow(IssueToken.Initiator(token, user, notary, 1 of token, anonymous = false))
    }
}

