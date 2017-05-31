package com.template.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.identity.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow

/**
 * Define your flow here.
 */
object TemplateFlow {
    /**
     * You can add a constructor to each FlowLogic subclass to pass objects into the flow.
     */
    @InitiatingFlow
    class Initiator: FlowLogic<Unit>() {
        /**
         * Define the initiator's flow logic here.
         */
        @Suspendable
        override fun call() {}
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val counterparty: Party) : FlowLogic<Unit>() {
        /**
         * Define the acceptor's flow logic here.
         */
        @Suspendable
        override fun call() {}
    }
}