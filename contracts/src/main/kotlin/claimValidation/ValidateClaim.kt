package claimValidation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import khttp.responses.Response
import net.corda.core.identity.Party
import org.json.JSONObject
import java.security.interfaces.RSAPublicKey
import java.time.LocalDateTime

class ValidateClaim () {

    var DIDDocHost: String = "beta.discover.did.microsoft.com"

    fun validateClaim(jwsProof: String, partyToVerify: Party, trustedSigners: Set<Party>, expectedMembershipName: String){
        var claim: CordaNetworkClaim = deserializeJws(jwsProof, trustedSigners)
        validateClaimType(claim)
        validateExpirationDate(claim)
        validateMembershipName(claim, expectedMembershipName)
        validateClaimSubject(claim, partyToVerify)
    }

    fun deserializeJws(jwsProof: String, trustedSigners: Set<Party>) : CordaNetworkClaim {
        var signerFound = false
        val jwsObject = JWSObject.parse(jwsProof)
        for (party in trustedSigners) {
            val verifier : JWSVerifier = RSASSAVerifier(party.owningKey as RSAPublicKey)
            if (jwsObject.verify(verifier)){
                signerFound = true
                break
            }
        }
        if (signerFound == false) {
            throw IllegalArgumentException("The claim is not signed by someone in the list of trusted signers")
        }
        val gson : Gson = GsonBuilder().setPrettyPrinting().create()
        return gson.fromJson(jwsObject.payload.toString(), CordaNetworkClaim::class.java)
    }

    fun validateClaimType(claim: CordaNetworkClaim) {
        var expected: List<String> = listOf("VerifiableCredential", "CordaBusinessNetworkCredential")
        assertTrue(claim.type.equals(expected))
    }

    fun validateExpirationDate(claim: CordaNetworkClaim) {
        val currTime: LocalDateTime? = LocalDateTime.now()
        assertTrue((LocalDateTime.parse(claim.expirationDate)) > currTime)
    }

    fun validateMembershipName(claim: CordaNetworkClaim, expectedMembershipName: String) {
        assertEquals(((claim.credentialSubject).get("membership") as Map<String, String>).get("name"), expectedMembershipName)
    }

    fun validateClaimSubject(claim: CordaNetworkClaim, partyToVerify: Party) {
        val DIDID = claim.credentialSubject.get("id")
        val request: String = "http://" + DIDDocHost + "/1.0/identifiers/" + DIDID
        val response: Response = khttp.get(request)
        assertEquals("<Response [200]>", response.toString())
        val DIDDoc : JSONObject = response.jsonObject
        var key: String = (((DIDDoc["document"]) as JSONObject).get("publicKey")).toString()
        assertEquals(key, partyToVerify.owningKey.toString())
    }
}