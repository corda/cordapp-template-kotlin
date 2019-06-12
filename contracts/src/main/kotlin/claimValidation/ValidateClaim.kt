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

    /**
     * calls all the library methods to validate a claim
     *
     * @param jwsProof string of serialized signed claim
     * @param partyToVerify Party the user is talking to that should be the subject of the claim
     * @param trustedSigners set of Party objects that are trusted by the user to sign claims
     * @param expectedMembershipName String that is the name of the membership type that is expected on the claim
     */
    fun validateClaim(jwsProof: String, partyToVerify: Party, trustedSigners: Set<Party>, expectedMembershipName: String){
        var claim: CordaNetworkClaim = deserializeJws(jwsProof, trustedSigners)
        var expected: List<String> = listOf("VerifiableCredential", "CordaBusinessNetworkCredential")
        validateClaimType(claim, expected)
        validateExpirationDate(claim)
        validateMembershipName(claim, expectedMembershipName)
        validateClaimSubject(claim, partyToVerify)
    }

    /**
     * deserializes the jws string if the signer is in the list of trusted signers
     *
     * @param jwsProof the serialized string of the signed claim
     * @param trustedSigners a set of Party objects that are trusted entities to sign a claim
     * @return CordaNetworkClaim object that is the deserialized form of the jws string
     */
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

    /**
     * validates that the claim type matches the passed in list
     *
     * @param claim the CordaNetworkClaim java object that was deserialized in deserializeJws
     * @param expected a list of Party objects who the user trusts to sign claims for the given network
     */
    fun validateClaimType(claim: CordaNetworkClaim, expected: List<String>) {
        assertTrue(claim.type.equals(expected))
    }

    /**
     * validates that the expiration date on the claim has not passed yes
     *
     * @param claim the CordaNetworkClaim java object that was deserialized in deserializeJws
     */
    fun validateExpirationDate(claim: CordaNetworkClaim) {
        val currTime: LocalDateTime? = LocalDateTime.now()
        assertTrue((LocalDateTime.parse(claim.expirationDate)) > currTime)
    }

    /**
     * validates that the credential subject membership name matches the expected name
     *
     * @param claim the CordaNetworkClaim java object that was deserialized in deserializeJws
     * @param expectedMembershipName String that is the expected credential subject membership name listed on the claim
     */
    fun validateMembershipName(claim: CordaNetworkClaim, expectedMembershipName: String) {
        assertEquals(((claim.credentialSubject).get("membership") as Map<String, String>).get("name"), expectedMembershipName)
    }

    /**
     * queries DID resolver with DID ID listed under credential subject id
     * validates that the key on the DID document matches the key of the party passed in
     *
     * @param claim the CordaNetworkClaim java object that was deserialized in deserializeJws
     * @param partyToVerify the Party that the user is talking to and validating the claim for
     */
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