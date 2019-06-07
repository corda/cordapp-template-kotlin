package com.template.contracts

import junit.framework.Assert.assertEquals
import net.corda.testing.node.MockServices
import org.junit.Test
import khttp.responses.Response
import org.json.JSONObject
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.internal.LinkedTreeMap
import junit.framework.Assert.assertTrue
import java.io.BufferedReader
import java.io.File

class ContractTests {
    private val ledgerServices = MockServices()

    @Test
    fun `validateClaim`() {
        var claimPresentation: DIDClaimPresentation = jsonToGson(file)
        var subjectDIDID : String = getSubjectDIDIDFromClaim(claimPresentation)
        val subjectDIDDoc : JSONObject = queryDIDResolver(subjectDIDID)
        var ownerKey: String = getOwnerKey(subjectDIDDoc)
        //TODO: Missing step: check that key matches the person I am talking to
//        assertTrue(validateOwnerKey(getOwnerKey(claim), "sample"))

        val signerDIDID = getSignerDIDIDFromClaim(claimPresentation)
        val signerDIDDoc : JSONObject = queryDIDResolver(signerDIDID)
        val signerKey : String = getOwnerKey(signerDIDDoc)
        assertTrue(validateClaimSigner(signerKey))

        assertTrue(stillValidCheck(claimPresentation))
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
}