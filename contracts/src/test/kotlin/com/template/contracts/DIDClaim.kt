package com.template.contracts

import org.json.JSONObject
import org.omg.CORBA.Object

data class DIDClaim (
        val context : List<String>,
        val type : List<String>,
        val issuer: String,
        val issuanceDate: String,
        val credentialSubject: Any,
        val credentialStatus: Any,
        val proof : Any
)
