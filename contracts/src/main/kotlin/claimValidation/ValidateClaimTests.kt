package claimValidation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.security.interfaces.RSAPublicKey

class ValidateClaimTests {

    val validateClaimInstance: ValidateClaim = ValidateClaim()

    //read sample claim and convert it to a CordaNetwork claim object
    val gson : Gson = GsonBuilder().setPrettyPrinting().create()
    val claimString = "C:\\Users\\Administrator\\Documents\\corda-dapps\\cordapp-template-kotlin\\contracts\\sample claim.json"
    val buffreader : BufferedReader = File(claimString).bufferedReader()
    val inputString = buffreader.use { it.readText() }
    var claim= gson.fromJson(inputString, CordaNetworkClaim::class.java)

    val rsaJWK: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()
    val pubkey: RSAPublicKey = rsaJWK.toRSAPublicKey()
    val subjectName: CordaX500Name = CordaX500Name("Claim Subject", "test", "DID", "Redmond", "WA", "US")
    val partyToVerify: Party = Party(subjectName, pubkey)

    var trustedSigners: Set<Party> = setOf(partyToVerify)

    fun createSamplejws() : String{
        val rsaJWK: RSAKey = RSAKeyGenerator(2048).keyID("123").generate()
        val jObject = JWSObject(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), Payload("{\n" +
                "  \"context\": [\n" +
                "    \"https://www.w3.org/2018/credentials/v1\",\n" +
                "    \"https://www.w3.org/2018/credentials/examples/v1\"\n" +
                "  ],\n" +
                "  \"id\": \"http://example.edu/credentials/1872\",\n" +
                "  \"type\": [\"VerifiableCredential\", \"CordaBusinessNetworkCredential\"],\n" +
                "  \"issuer\": \"https://example.edu/issuers/565049\",\n" +
                "  \"issuanceDate\": \"2010-01-01T19:43:24\",\n" +
                "  \"expirationDate\": \"2020-01-01T19:59:24\",\n" +
                "  \"credentialSubject\": {\n" +
                "    \"id\": \"did:ion-test:EiDDNR0RyVI4rtKFeI8GpaSougQ36mr1ZJb8u6vTZOW6Vw\",\n" +
                "    \"membership\": {\n" +
                "      \"type\" : \"full member\",\n" +
                "      \"name\": \"Bank Consortium\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"proof\": {\n" +
                "    \"type\": \"RsaSignature2018\",\n" +
                "    \"created\": \"2017-06-18T21:19:10Z\",\n" +
                "    \"proofPurpose\": \"assertionMethod\",\n" +
                "    \"verificationMethod\": \"https://example.edu/issuers/keys/1\",\n" +
                "    \"jws\": \"eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5XsITJX1CxPCT8yAV-TVkIEq_PbChOMqsLfRoPsnsgw5WEuts01mq-pQy7UJiN5mgRxD-WUcX16dUEMGlv50aqzpqh4Qktb3rk-BuQy72IFLOqV0G_zS245-kronKb78cPN25DGlcTwLtjPAYuNzVBAh4vGHSrQyHUdBBPM\"\n" +
                "  }\n" +
                "}"))
        val signer: JWSSigner = RSASSASigner(rsaJWK)
        jObject.sign(signer)
        val jws: String = jObject.serialize()
        val signerName = CordaX500Name("Claim Signer", "test", "DID", "Redmond", "WA", "US")
        val signerpubkey: RSAPublicKey = rsaJWK.toRSAPublicKey()
        println("SignerKey: " + signerpubkey)
        val signingParty = Party(signerName, signerpubkey)
        trustedSigners = trustedSigners.plus(signingParty)
        return jws
    }


    @Test
    fun validateClaimAll(){
        val jwsString = createSamplejws()
        var claimExample = validateClaimInstance.deserializeJws(jwsString, trustedSigners)
        var expected: List<String> = listOf("VerifiableCredential", "CordaBusinessNetworkCredential")
        validateClaimInstance.validateClaimType(claimExample, expected)
        validateClaimInstance.validateExpirationDate(claimExample)
        validateClaimInstance.validateMembershipName(claimExample, "Bank Consortium")
    }

    @Test
    fun testDeserializeJws(){
        val jws = createSamplejws()
        println(validateClaimInstance.deserializeJws(jws, trustedSigners).toString())
    }

    @Test
    fun testValidateClaimType() {
        var expected: List<String> = listOf("VerifiableCredential", "CordaBusinessNetworkCredential")
        validateClaimInstance.validateClaimType(claim, expected)
    }

    @Test
    fun testValidateExpirationDate() {
        validateClaimInstance.validateExpirationDate(claim)
    }

    @Test
    fun testValidateMembershipName() {
        validateClaimInstance.validateMembershipName(claim, "Bank Consortium")
    }

    @Test
    fun testValidateClaimSubject() {
        validateClaimInstance.validateClaimSubject(claim, partyToVerify)
    }
}