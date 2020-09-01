package com.template.flows.dice

import com.template.types.PlayerHand
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignedData
import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.SerializedBytes
import java.security.PublicKey
import javax.persistence.*

object PlayerHandSchema

object PlayerHandSchemaV1 : MappedSchema(
        schemaFamily = PlayerHandSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPayerHand::class.java)
)

@Entity
@Table(name = "player_hand") //TODO combined key gameId + roundId
class PersistentPayerHand(
        @Id
        @GeneratedValue
        var id: Long = 0,

        @Column(name = "gameId", nullable = false)
        var gameId: String = "",

        @Column(name = " roundId", nullable = false)
        var roundId: Int = 0,

        @Column(name = "hand", nullable = false)
        var hand: String = "",

        @Column(name = "raw", length = 1024)
        var raw: ByteArray? = null,

        @Column(name = "sigBy")
        var sigBy: PublicKey? = null,

        @Column(name = "sigBytes")
        var sigBytes: ByteArray? = null
) {
    constructor(playerHand: PlayerHand) : this(
            0, //TODO need to have different ID so we don't have multiple gameID - roundId hands in db (it's taken care of in oracle, but who knows what user does
            gameId = playerHand.gameId.toString(),
            roundId = playerHand.roundId,
            hand = playerHand.hand.toString(),
            raw = playerHand.hashOfRoundHands.raw.bytes,
            sigBy = playerHand.hashOfRoundHands.sig.by,
            sigBytes = playerHand.hashOfRoundHands.sig.bytes
    )

    fun toPlayerHand(): PlayerHand {
        return PlayerHand(
                SecureHash.parse(gameId),
                roundId,
                hand.removeSurrounding("[","]").replace(" ","").split(",").map { it.toInt() },
                SignedData(
                        SerializedBytes(raw!!),
                        DigitalSignature.WithKey(sigBy!!, sigBytes!!)
                )
        )
    }
}