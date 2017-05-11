package com.template.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.identity.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;

/**
 * Define your flow here.
 */
public class TemplateFlow {
    /**
     * You can add a constructor to each FlowLogic subclass to pass objects into the flow.
     */
    @InitiatingFlow
    public static class Initiator extends FlowLogic<Void> {
        /**
         * Define the initiator's flow logic here.
         */
        @Suspendable
        @Override public Void call() { return null; }
    }

    public static class Acceptor extends FlowLogic<Void> {
        private Party counterparty;

        public Acceptor(Party counterparty) {
            this.counterparty = counterparty;
        }

        /**
         * Define the acceptor's flow logic here.
         */
        @Suspendable
        @Override
        public Void call() { return null; }
    }
}
