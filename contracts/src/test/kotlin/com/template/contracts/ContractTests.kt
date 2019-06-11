package com.template.contracts

import net.corda.testing.node.MockServices
import org.junit.Test
import khttp.responses.Response
import org.json.JSONObject
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.internal.LinkedTreeMap
import java.io.BufferedReader
import java.io.File
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import junit.framework.Assert.*

class ContractTests {
    private val ledgerServices = MockServices()

    @Test
    fun `validateClaim`() {
        decryptProof()
//        jsonClaimToGson(fileC)
//        var claimPresentation: DIDClaimPresentation = jsonToGson(file)
//        var subjectDIDID : String = getSubjectDIDIDFromClaim(claimPresentation)
//        val subjectDIDDoc : JSONObject = queryDIDResolver(subjectDIDID)
//        var ownerKey: String = getOwnerKey(subjectDIDDoc)
        //TODO: Missing step: check that key matches the person I am talking to
//        assertTrue(validateOwnerKey(getOwnerKey(claim), "sample"))

//        val signerDIDID = getSignerDIDIDFromClaim(claimPresentation)
//        val signerDIDDoc : JSONObject = queryDIDResolver(signerDIDID)
//        val signerKey : String = getOwnerKey(signerDIDDoc)
//        assertTrue(validateClaimSigner(signerKey))
//
//        assertTrue(stillValidCheck(claimPresentation))
    }

    var claimID: String = "did:ion-test:EiDDNR0RyVI4rtKFeI8GpaSougQ36mr1ZJb8u6vTZOW6Vw"
    var host: String = "beta.discover.did.microsoft.com"

    //TODO: make this a list of parties - update add and remove method
    var approvedSigners = mutableListOf("[{\"publicKeyJwk\":{\"kty\":\"EC\",\"defaultSignAlgorithm\":\"ES256K\",\"crv\":\"P-256K\",\"use\":\"verify\",\"defaultEncryptionAlgorithm\":\"none\",\"kid\":\"#key-1\",\"x\":\"Y4ezHen9MPuJcowKwhc9jT1owEzNb65BMUqtS7NH_C8\",\"y\":\"wWDGd0PHYjIGRcP9owNvsSLYWzSbFLuCKE8KX75KFRY\"},\"id\":\"#key-1\",\"type\":\"Secp256k1VerificationKey2018\"}]")

    //adds party toAdd to the list of trusted signers
    fun addApprovedSigner(toAdd: String) {
        approvedSigners.add(toAdd)
    }

    //removes party toRemove from the list of trusted signers
    fun removeApprovedSigner(toRemove: String) {
        approvedSigners.remove(toRemove)
    }

    var file: String = "C:\\Users\\Administrator\\Documents\\corda-dapps\\cordapp-template-kotlin\\contracts\\sample claim presentation.json"

    private fun jsonToGson(claim : String) : DIDClaimPresentation {
        val gson : Gson = GsonBuilder().setPrettyPrinting().create()
        val buffreader : BufferedReader = File(claim).bufferedReader()
        val inputString = buffreader.use { it.readText() }
        var claimPresentation = gson.fromJson(inputString, DIDClaimPresentation::class.java)
        return claimPresentation
    }

    var fileC: String = "C:\\Users\\Administrator\\Documents\\corda-dapps\\cordapp-template-kotlin\\contracts\\sample claim.json"

    private fun jsonClaimToGson(claim : String) : DIDClaim {
        val gson : Gson = GsonBuilder().setPrettyPrinting().create()
        val buffreader : BufferedReader = File(claim).bufferedReader()
        val inputString = buffreader.use { it.readText() }
        var claim= gson.fromJson(inputString, DIDClaim::class.java)
        println(claim)
        return claim
    }

    private fun getSubjectDIDIDFromClaim(claim : DIDClaimPresentation) : String {
        return (claim.verifiableCredential.get(0).get("credentialSubject") as LinkedTreeMap<String, String>).get("id") as String
    }

    private fun getSignerDIDIDFromClaim(claim : DIDClaimPresentation) : String {
        return (claim.verifiableCredential.get(0).get("issuer") as String)
    }

    //connects to ION resolver
    //returns: claim as JSON Object
    private fun queryDIDResolver(DIDID: String) : JSONObject {
        println(DIDID)
        val request: String = "http://" + host + "/1.0/identifiers/" + DIDID
        val response: Response = khttp.get(request)
        assertEquals(response.toString(), "<Response [200]>")
        val DIDDoc : JSONObject = response.jsonObject
        println(DIDDoc)
        return DIDDoc
    }

    //parses json claim to return ownerkey
    //input: json claim
    //returns: string of owners key
    private fun getOwnerKey(claim : JSONObject) : String {
        var key: Any = (((claim["document"]) as JSONObject).get("publicKey"))
        return key.toString()
    }

    //checks that the owner of the claim is the node I am communicating with
    //input: owner key string found on claim, key of node that sent the claim
    //return: true if the keys match, false otherwise
    private fun validateOwnerKey(claimOwnerKey : String, claimHolderKey: String) : Boolean {
        if (claimOwnerKey.contains(claimHolderKey)) {
            return true
        }
        return false
    }

    //checks that the signer of the claim is someone in my list of trusted signers
    //input: key of the signer of the claim as a string
    //return: true if the signer is in approvedSigners, false otherwise
    private fun validateClaimSigner(signerKey : String) : Boolean {
        //TODO: verify that the signer key is in my list of trusted signers
        for(party in approvedSigners) {
            if (party == signerKey) {
                return true
            }
        }
        return false
    }

    private fun stillValidCheck(claim : DIDClaimPresentation) : Boolean {
        val statusLink: String = (claim.verifiableCredential.get(0).get("credentialStatus") as LinkedTreeMap<String, String>).get("id") as String
        val response: Response = khttp.get(statusLink)
        if(response.toString().toLowerCase().contains("suspended") or response.toString().toLowerCase().contains("expired")){
            return false
        }
        return true
    }

    private fun decryptProof() : Boolean {
        val rsaJWK: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()
        val jObject: JWSObject = JWSObject(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), Payload("\"context\": [\n" +
                "    \"https://www.w3.org/2018/credentials/v1\",\n" +
                "    \"https://www.w3.org/2018/credentials/examples/v1\"\n" +
                "  ],\n" +
                "  \"id\": \"http://example.edu/credentials/1872\""))
        val rsaPublicJWK: RSAKey = rsaJWK.toPublicJWK()
        val signer: JWSSigner = RSASSASigner(rsaJWK)
        val claimset: JWTClaimsSet = JWTClaimsSet.Builder().subject("summer").issuer("http://microsoft.com").claim("id", "http://example.edu/credentials/1872").build()
        val signedJWT: SignedJWT = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.keyID).build(), claimset)
        signedJWT.sign(signer)
        jObject.sign(signer)
        println("signedJWT: " + signedJWT.payload)
        println("jObject: " + jObject.payload)
        println(rsaJWK)
        val jws: String = "eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5XsITJX1CxPCT8yAV-TVkIEq_PbChOMqsLfRoPsnsgw5WEuts01mq-pQy7UJiN5mgRxD-WUcX16dUEMGlv50aqzpqh4Qktb3rk-BuQy72IFLOqV0G_zS245-kronKb78cPN25DGlcTwLtjPAYuNzVBAh4vGHSrQyHUdBBPM"
//        val jws: String = "BavEll0/I1zpYw8XNi1bgVg/sCneO4Jugez8RwDg/+MCRVpjOboDoe4SxxKjkCOvKiCHGDvc4krqi6Z1n0UfqzxGfmatCuFibcC1wpsPRdW+gGsutPTLzvueMWmFhwYmfIFpbBu95t501+rSLHIEuujM/+PXr9Cky6Ed+W3JT24="
        val jwsObject: JWSObject = JWSObject.parse(jws)
        val verifier: JWSVerifier = RSASSAVerifier(rsaJWK)
        assertFalse(jwsObject.verify(verifier))
        println("jwsObject:")
        println(jwsObject.signature)
        println(jwsObject.header)
        println(jwsObject.payload)
        return true
    }
}