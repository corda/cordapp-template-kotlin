package com.template.contracts

data class DIDClaimPresentation (
        val context : List<String>,
        val type : List<String>,
        val verifiableCredential: List<Map<Any, Any>>,
        val proof : Any
)