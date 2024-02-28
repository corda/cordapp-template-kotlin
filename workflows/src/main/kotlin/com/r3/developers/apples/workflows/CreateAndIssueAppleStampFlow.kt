package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.AppleStamp
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@InitiatingFlow(protocol = "create-and-issue-apple-stamp")
class CreateAndIssueAppleStampFlow : ClientStartableFlow {

    internal data class CreateAndIssueAppleStampRequest(
        val stampDescription: String,
        val holder: MemberX500Name,
        val notary: MemberX500Name
    )

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
        val request = requestBody.getRequestBodyAs(
            jsonMarshallingService,
            CreateAndIssueAppleStampRequest::class.java)
        val stampDescription = request.stampDescription
        val holderName = request.holder

        val notaryInfo = notaryLookup.lookup(request.notary)
            ?: throw IllegalArgumentException("Notary ${request.notary} not found")

        val issuer = memberLookup.myInfo().ledgerKeys.first()
        val holder = memberLookup.lookup(holderName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The holder $holderName does not exist within the network")

        // Building the output AppleStamp state
        val newStamp = AppleStamp(
            id = UUID.randomUUID(),
            stampDesc = stampDescription,
            issuer = issuer,
            holder = holder,
            participants = listOf(issuer, holder)
        )

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addOutputState(newStamp)
            .addCommand(AppleCommands.Issue())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(issuer, holder))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(holderName)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, listOf(session))
            newStamp.id.toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}
