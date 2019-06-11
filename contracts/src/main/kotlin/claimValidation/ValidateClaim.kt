package claimValidation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import junit.framework.Assert
import khttp.responses.Response
import net.corda.core.identity.Party
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File

class ValidateClaim () {

    var DIDDocHost: String = "beta.discover.did.microsoft.com"

    fun validateClaim(claimFile: String, partyToVerify: Party, trustedSigners: Set<Party>){
        val claim: CordaNetworkClaim = jsonClaimToJavaObject(claimFile)
        val signerDIDDoc : JSONObject = queryDIDResolver(claim.issuer)
    }

    //params: file name of Corda Network Claim to Verify
    //output: Corda Network Claim as java object
    //Takes the name of a json corda network claim and translates the json into a java object
    fun jsonClaimToJavaObject(claim : String) : CordaNetworkClaim {
        val gson : Gson = GsonBuilder().setPrettyPrinting().create()
        val buffreader : BufferedReader = File(claim).bufferedReader()
        val inputString = buffreader.use { it.readText() }
        var claim= gson.fromJson(inputString, CordaNetworkClaim::class.java)
        return claim
    }

    //params: DID ID of claim signer
    //returns: DID document as JSON Object
    //connects to ION resolver to retreive DID document
    private fun queryDIDResolver(DIDID: String) : JSONObject {
        println(DIDID)
        val request: String = "http://" + DIDDocHost + "/1.0/identifiers/" + DIDID
        val response: Response = khttp.get(request)
        Assert.assertEquals(response.toString(), "<Response [200]>")
        val DIDDoc : JSONObject = response.jsonObject
        println(DIDDoc)
        return DIDDoc
    }
}