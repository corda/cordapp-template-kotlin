package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class TokensVaultQueryFlow(private val party: Party) : FlowLogic<List<FungibleToken>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): List<FungibleToken> {
        serviceHub.vaultService.queryBy(FungibleToken::class.java).states.map { println(it) }
        return serviceHub.vaultService.queryBy(FungibleToken::class.java).states.map { it.state.data }
    }
}
@InitiatingFlow
@StartableByRPC
class SomeObjectVaultQuery(private val party: Party) : FlowLogic<List<SomeObject>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): List<SomeObject> {
        serviceHub.vaultService.queryBy(SomeObject::class.java).states.map { println(it) }
        return serviceHub.vaultService.queryBy(SomeObject::class.java).states.map { it.state.data }
    }
}