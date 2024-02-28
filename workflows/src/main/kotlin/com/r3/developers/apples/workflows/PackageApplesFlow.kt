package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit

class PackageApplesFlow : ClientStartableFlow {

    internal data class PackApplesRequest(val appleDescription: String, val weight: Int, val notary: MemberX500Name)

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, PackApplesRequest::class.java)
        val appleDescription = request.appleDescription
        val weight = request.weight
        val notary = notaryLookup.lookup(request.notary)
            ?: throw IllegalArgumentException("Notary ${request.notary} not found")
        val myKey = memberLookup.myInfo().ledgerKeys.first()

        // Building the output BasketOfApples state
        val basket = BasketOfApples(
            description = appleDescription,
            farm = myKey,
            owner = myKey,
            weight = weight,
            participants = listOf(myKey)
        )

        // Create the transaction
        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(basket)
            .addCommand(AppleCommands.PackBasket())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(myKey))
            .toSignedTransaction()

        return try {
            // Record the transaction, no sessions are passed in as the transaction is only being
            // recorded locally
            utxoLedgerService.finalize(transaction, emptyList()).transaction.id.toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}
