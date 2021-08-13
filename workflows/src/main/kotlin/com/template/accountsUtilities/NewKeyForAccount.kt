package com.template.accountsUtilities


import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.PartyAndCertificate
import java.util.*

@StartableByRPC
@StartableByService
class NewKeyForAccount(private val accountId: UUID) : FlowLogic<PartyAndCertificate>() {
    @Suspendable
    override fun call(): PartyAndCertificate {
        return serviceHub.keyManagementService.freshKeyAndCert(
                identity = ourIdentityAndCert,
                revocationEnabled = false,
                externalId = accountId
        )
    }
}