package com.template.contracts

import org.json.JSONObject
import org.omg.CORBA.Object

data class DIDClaimPresentation (
        val context : List<String>,
        val type : List<String>,
        val verifiableCredential: List<Map<Any, Any>>,
        val proof : Any
)

data class VerifiableCredential(
        val id : String,
        val type : String,
        val issuer : String,
        val issunceDate: String,
        val CredentialSubject: Any,
        val credentialStatus: Any,
        val proof : JSONObject
)