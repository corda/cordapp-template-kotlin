package claimValidation

import com.google.gson.internal.LinkedTreeMap

data class CordaNetworkClaim (
        val context : List<String>,
        val type : List<String>,
        val issuer: String,
        val issuanceDate: String,
        val expirationDate: String,
        val credentialSubject: LinkedTreeMap<String, Any>,
        val proof : Any
)
