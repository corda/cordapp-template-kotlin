package claimValidation

data class CordaNetworkClaim (
        val context : List<String>,
        val type : List<String>,
        val issuer: String,
        val issuanceDate: String,
        val credentialSubject: Any,
        val credentialStatus: Any,
        val proof : Any
)
