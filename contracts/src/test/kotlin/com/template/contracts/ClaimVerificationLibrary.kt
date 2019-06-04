import net.corda.core.crypto.PartialMerkleTree
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.security.cert.X509Certificate
import java.net.*
import java.io.*

class Verify_Library(claimID: Int){

    fun main(){
        println("in main")
        queryDID()
        println("done")
    }

//    var myCert:X509Certificate()
//    var me: Party = Party(myCert)
    //passed in
    var claimID: Int = 1
//    var partyToVerify: Party = null
    //variable for resolver (connection link?)

    var approvedSigners = mutableListOf("me")

    fun addApprovedSigner(toAdd: String) {
        approvedSigners.add(toAdd)
    }

    fun removeApprovedSigner(toRemove: String) {
        approvedSigners.remove(toRemove)
    }

    fun queryDID(){
//        curl https://beta.discover.did.microsoft.com/1.0/identifiers/did:ion-test:EiDDNR0RyVI4rtKFeI8GpaSougQ36mr1ZJb8u6vTZOW6Vw
//        RestAssured.get("https://beta.discover.did.microsoft.com/1.0/identifiers/did:ion-test:EiDDNR0RyVI4rtKFeI8GpaSougQ36mr1ZJb8u6vTZOW6Vw")
//        RestAssured.get("/1.0/identifiers/did:ion-test:EiDDNR0RyVI4rtKFeI8GpaSougQ36mr1ZJb8u6vTZOW6Vw HTTP/1.1")
//        Host: beta.discover.did.microsoft.com
//        Accept: application/json

    }

    //open/connect to resolver?
    //query resolver with climID
    //receive claim as Json
    //parse json for owner key
    //make sure owner key matches identity of node I am communicating with
    //parse json for endorsement key
    //iterate through approvedSigners and determine if any of the keys match
    //return true if everything checks out

}