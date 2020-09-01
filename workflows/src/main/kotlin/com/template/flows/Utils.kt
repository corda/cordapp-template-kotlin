package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.Game
import com.template.flows.dice.PersistentPayerHand
import com.template.schemas.PersistentGame
import com.template.types.PlayerHand
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder

@Suspendable
fun FlowLogic<*>.notary() = serviceHub.networkMapCache.notaryIdentities.first()

@Suspendable
fun FlowLogic<*>.getGameByProposalId(proposalId: UniqueIdentifier): StateAndRef<Game>? {
    val query = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(proposalId))
    return serviceHub.vaultService.queryBy(Game::class.java, query).states.singleOrNull()
}

@Suspendable
fun FlowLogic<*>.getGameByGameId(gameId: SecureHash): StateAndRef<Game>? {
    val gameIdStr = gameId.toString()
    val query = QueryCriteria.VaultCustomQueryCriteria(builder { PersistentGame::gameId.equal(gameIdStr) })
    return serviceHub.vaultService.queryBy(Game::class.java, query).states.singleOrNull()
}

@Suspendable
fun FlowLogic<*>.getRoll(gameId: SecureHash, currentRound: Int): PlayerHand {
    return serviceHub.withEntityManager {
        val query = createQuery(
                "SELECT roll FROM ${PersistentPayerHand::class.java.name} roll WHERE roll.gameId = :gameId AND roll.roundId = :roundId",
                PersistentPayerHand::class.java
        )
        query.setParameter("gameId", gameId.toString())
        query.setParameter("roundId", currentRound)
        query.resultList.first().toPlayerHand()
    }
}