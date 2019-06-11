package claimValidation

import junit.framework.Assert.assertEquals
import org.junit.Test

class ValidateClaimTests {

    val validateClaimInstance: ValidateClaim = ValidateClaim()

    @Test
    fun validateClaimAll(){

    }

    @Test
    fun testJsonClaimToJavaObject(){
        val testClaim: String = "C:\\Users\\Administrator\\Documents\\corda-dapps\\cordapp-template-kotlin\\contracts\\sample claim.json"
        val networkClaim: CordaNetworkClaim = validateClaimInstance.jsonClaimToJavaObject(testClaim)
        val expectedList: List<String> = listOf("VerifiableCredential", "CordaBusinessNetworkCredential")
        assertEquals(expectedList, networkClaim.type)
    }
}