package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.SensitiveFlowContract
import com.template.states.SensitiveState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi
import java.util.stream.Collectors

/**
 * EXP: Encrypting a property of a state for improved backchain privacy.
 */

@InitiatingFlow
@StartableByRPC
class SecretFlow(
    private val sensitiveData: String,
    private val message: String,
    private val pw: String,
    private val receiver: Party
) :
    FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val sender = ourIdentity

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val msgHash = AesUtil.genMD5(sensitiveData)
        val encryptedData = AesUtil.encrypt(sensitiveData, pw)
        val output = SensitiveState(
            secretMsg = encryptedData,
            secretMsgHash = msgHash,
            msg = message,
            sender = sender,
            receiver = receiver
        )

        logger.info("[TEST] Sensitive data hash: $msgHash\n")
        val builder = TransactionBuilder(notary)
            .addCommand(SensitiveFlowContract.Commands.Create(), listOf(sender.owningKey, receiver.owningKey))
            .addOutputState(output)

        // Verify and sign it with our KeyPair.
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> =
            output.participants.stream().map { el: AbstractParty? -> el as Party? }.collect(Collectors.toList())
        otherParties.remove(ourIdentity)
        val sessions = otherParties.stream().map { el: Party? -> initiateFlow(el!!) }.collect(Collectors.toList())

        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        // Assuming no exceptions, we can now finalise the transaction.
        return subFlow<SignedTransaction>(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(SecretFlow::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Additional checks -> I "think" verification logic is run before reaching here.
                val data = stx.tx.outputs.single().data as SensitiveState
                requireThat {
                    "Invalid type of state.".using(data is SensitiveState)
                }

                val password = "password" // Assume it's somehow available on the counter party node.
                val sensitiveData = AesUtil.decrypt(data.secretMsg, password)
                logger.info("[TEST] decrypted sensitive message: $sensitiveData\n")

                val msgHash = data.secretMsgHash // MD5 hash
                val calMsgHash = AesUtil.genMD5(sensitiveData)
                logger.info("[TEST] check MD5 hash: $calMsgHash == $msgHash\n")

            }
        }

        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

