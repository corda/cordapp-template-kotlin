package com.template.flows.utilities

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.PartyAndCertificate
import java.util.*

@StartableByRPC
@StartableByService
class GenerateKey(val acctId: UUID) : FlowLogic<PartyAndCertificate>(){

    @Suspendable
    override fun call(): PartyAndCertificate {
        return serviceHub.keyManagementService.freshKeyAndCert(
                identity = ourIdentityAndCert,
                revocationEnabled = false,
                externalId = acctId
        )
    }
}