package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@InitiatingFlow(protocol = "redeem-apples")
class RedeemApplesFlow : ClientStartableFlow {

    internal data class RedeemApplesRequest(val buyer: MemberX500Name, val notary: MemberX500Name, val stampId: UUID)

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

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
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, RedeemApplesRequest::class.java)
        val buyerName = request.buyer
        val stampId = request.stampId

        // Retrieve the notary's public key (this will change)
        val notaryInfo = notaryLookup.lookup(request.notary)
            ?: throw IllegalArgumentException("Notary ${request.notary} not found")

        val myKey = memberLookup.myInfo().ledgerKeys.first()

        val buyer = memberLookup.lookup(buyerName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The buyer does not exist within the network")

        val appleStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(AppleStamp::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.id == stampId }
            ?: throw IllegalArgumentException("No apple stamp matching the stamp id $stampId")

        val basketOfApplesStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(BasketOfApples::class.java)
            .firstOrNull { basketStateAndRef -> basketStateAndRef.state.contractState.owner ==
                    appleStampStateAndRef.state.contractState.issuer }
            ?: throw IllegalArgumentException("There are no eligible baskets of apples")


        val originalBasketOfApples = basketOfApplesStampStateAndRef.state.contractState

        val updatedBasket = originalBasketOfApples.changeOwner(buyer)

        // Create the transaction
        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addInputStates(appleStampStateAndRef.ref, basketOfApplesStampStateAndRef.ref)
            .addOutputState(updatedBasket)
            .addCommand(AppleCommands.Redeem())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(myKey, buyer))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(buyerName)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, listOf(session)).transaction.id.toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}
