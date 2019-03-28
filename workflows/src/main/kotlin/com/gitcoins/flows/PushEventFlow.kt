package com.gitcoins.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.contracts.utilities.of
import com.r3.corda.sdk.token.workflow.flows.IssueToken
import com.gitcoins.states.GitToken
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/**
 * Flow that delegates the issuing of a [GitToken] to the [IssueToken] subflow. This flow is triggered by a GitHub push
 * event that is linked to a github webhook.
 */
@StartableByRPC
class PushEventFlow(private val user: Party) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call() : SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val token = GitToken()
        //TODO Implement evaluation logic based on the commit

        // Initiator flow logic goes here.
        return subFlow(IssueToken.Initiator(token, user, notary, 1 of token, anonymous = false))
    }
}

