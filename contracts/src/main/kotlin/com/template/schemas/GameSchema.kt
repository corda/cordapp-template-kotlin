package com.template.schemas

import com.template.contracts.Game
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.*

object GameSchema

object GameSchemaV1 : MappedSchema(
        schemaFamily = GameSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentGame::class.java)
)

@Entity
@Table(name = "game", indexes = [
    Index(name = "game_id_idx", columnList = "gameId"),
    Index(name = "game_status_idx", columnList = "status")
])
class PersistentGame(
        @Column(name = "proposer", nullable = false)
        var proposer: Party,

        @Column(name = "oracle", nullable = false)
        var oracle: Party,

        @Column(name = "gameId", nullable = true)
        var gameId: String?,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: Game.Status,

        @Column(name = "current_round", nullable = false)
        var currentRound: Int
) : PersistentState()

