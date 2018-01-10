package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.ProgressTracker

@InitiatedBy(BuyCurrency::class)
class SellCurrency(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    private companion object {
        val ASKING_RATE_FROM_PROVIDER = object : ProgressTracker.Step("Asking exchange rate from oracle") {}
        val GENERATING_SPEND = object : ProgressTracker.Step("Generating spend to fulfil exchange") {}
        val PROPOSING_EXCHANGE_TO_SELLER = object : ProgressTracker.Step("Proposing currency exchange to seller") {}
        val WAITING_FOR_SELLER_REPLY = object : ProgressTracker.Step("Waiting for seller's reply") {}
        val CHECKING_SELLER_TRANSACTION = object : ProgressTracker.Step("Checking seller's transaction") {}
        val SIGNING_TRANSACTION = object : ProgressTracker.Step("Signing transaction") {}
        val COMMITTING_TRANSACTION = object : ProgressTracker.Step("Committing transaction to the ledger") {}
    }

    override val progressTracker = ProgressTracker(ASKING_RATE_FROM_PROVIDER, GENERATING_SPEND, PROPOSING_EXCHANGE_TO_SELLER, WAITING_FOR_SELLER_REPLY, CHECKING_SELLER_TRANSACTION, SIGNING_TRANSACTION, COMMITTING_TRANSACTION)

    @Suspendable
    override fun call() {
        return Unit
    }
}

@InitiatedBy(QueryRate::class)
class ProvideRate(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    private companion object {
        val ASKING_RATE_FROM_PROVIDER = object : ProgressTracker.Step("Asking exchange rate from oracle") {}
        val PROVIDING_RATE = object : ProgressTracker.Step("Generating spend to fulfil exchange") {}
    }

    override val progressTracker = ProgressTracker(ASKING_RATE_FROM_PROVIDER, PROVIDING_RATE)

    @Suspendable
    override fun call() {
        return Unit
    }
}