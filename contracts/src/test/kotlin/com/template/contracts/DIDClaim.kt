package com.template.contracts

import com.google.gson.internal.LinkedTreeMap
import org.json.JSONObject
import org.omg.CORBA.Object

data class DIDClaim (
        val context : List<String>,
        val type : List<String>,
        val issuer: String,
        val issuanceDate: String,
        val expirationDate: String,
        val credentialSubject: LinkedTreeMap<String,Any>,
        val proof : Any
)
