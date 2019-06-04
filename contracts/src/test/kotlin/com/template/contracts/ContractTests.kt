package com.template.contracts

import junit.framework.Assert.assertTrue
import net.corda.testing.node.MockServices
import org.junit.Test
import khttp.get
import khttp.responses.Response
import net.corda.core.identity.Party
import org.json.JSONObject

class ContractTests {
    private val ledgerServices = MockServices()

    @Test
    fun `verify claim library main method`() {
        val claim : JSONObject = queryDID()
        assertTrue(verifyOwnerKey(getOwnerKey(claim)))
        assertTrue(verifyClaimSigner(getSignerKey(claim)))
    }

    var claimID: String = "did:ion-test:EiDDNR0RyVI4rtKFeI8GpaSougQ36mr1ZJb8u6vTZOW6Vw"
    var host: String = "beta.discover.did.microsoft.com"

    val request: String = "http://" + host + "/1.0/identifiers/" + claimID

    //TODO: make this a list of parties - update add and remove method
    var approvedSigners = mutableListOf("me")

    //adds party toAdd to the list of trusted signers
    fun addApprovedSigner(toAdd: String) {
        approvedSigners.add(toAdd)
    }

    //removes party toRemove from the list of trusted signers
    fun removeApprovedSigner(toRemove: String) {
        approvedSigners.remove(toRemove)
    }

    //connects to ION resolver
    //returns: claim as JSON Object
    private fun queryDID() : JSONObject {
        val response: Response = khttp.get(request)
        val claim : JSONObject = response.jsonObject
        print(claim)
        return claim
    }

    //parses json claim to return ownerkey
    //input: json claim
    //returns: string of owners key
    private fun getOwnerKey(claim : JSONObject) : String {
        var ownerKey: String = "sample"
        //TODO: get the key of the claim owner
        return ownerKey
    }

    //parses json clim to return the signers key
    //input: json claim
    //output: dthe signer of the claims key in strong form
    private fun getSignerKey(claim : JSONObject) : String {
        var signerKey: String = "sample"
        //TODO: get the key of the claim signer
        return signerKey
    }

    //checks that the owner of the claim is the node I am communicating with
    //input: owner key string
    //return: true if the keys match, false otherwise
    private fun verifyOwnerKey(ownerKey : String) : Boolean {
        //TODO: verify that owner key identity and the identity of the node I am talking to match
        return true
    }

    //checks that the signer of the claim is someone in my list of trusted signers
    //input: key of the signer of the claim as a string
    //return: true if the signer is in approvedSigners, false otherwise
    private fun verifyClaimSigner(signerKey : String) : Boolean {
        //TODO: verify that the signer key is in my list of trusted signers
        for(party in approvedSigners) {

        }
        return true
    }
}