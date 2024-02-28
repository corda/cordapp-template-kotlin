package com.r3.developers.apples.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.ledger.utxo.StateAndRef
import java.security.PublicKey
import java.util.UUID

/**
 * The following is the base implementation of the Contract Tests for the Apples CorDapp template tutorial.
 *
 * - The AppleContractTest abstract class implements the ContractTest class.
 * - For full contract test coverage, we generally create a class for every command scenario for every state.
 * - Each of these classes will implement the abstract class to incorporate ContractTest general testing functionality
 *   as well as the functionality specific for this CordApp tutorial example.
 *
 * In this case, we have 3 scenarios (you can refer to contract files for the apples tutorial):
 *      1. AppleStamp state, AppleCommands.Issue command
 *      2. BasketOfApples state, AppleCommand.PackBasket command
 *      3. BasketOfApples state, AppleCommand.Redeem command
 *
 * The variables and methods within this abstract class are written to enable code re-use such that states are set up
 * automatically so that you only need to worry about the logic of your contracts
 **/

@Suppress("UnnecessaryAbstractClass", "UtilityClassWithPublicConstructor")
abstract class ApplesContractTest : ContractTest() {

    /**
     * The following are implementations of default values for the AppleStamp state for contract testing.
     * Some values such as the public keys already have default values implemented by the ContractTest class.
     * for more information, navigate to their declaration
     */
    // Default values for AppleStamp state
    protected val outputAppleStampStateId: UUID = UUID.randomUUID()
    protected val outputAppleStampStateStampDesc: String = "Can be exchanged for a single basket of apples"
    protected val outputAppleStampStateIssuer: PublicKey = bobKey
    protected val outputAppleStampStateHolder: PublicKey = daveKey
    protected val outputAppleStampStateParticipants: List<PublicKey> = listOf(bobKey, daveKey)
    protected val outputAppleStampState: AppleStamp = AppleStamp(
        outputAppleStampStateId,
        outputAppleStampStateStampDesc,
        outputAppleStampStateIssuer,
        outputAppleStampStateHolder,
        outputAppleStampStateParticipants
    )

    // Default values for BasketOfApples state
    protected val outputBasketOfApplesStateDescription: String = "Golden delicious apples, picked on 11th May 2023"
    protected val outputBasketOfApplesStateFarm: PublicKey = bobKey
    protected val outputBasketOfApplesStateOwner: PublicKey = bobKey
    protected val outputBasketOfApplesStateWeight: Int = 214
    protected val outputBasketOfApplesStateParticipants: List<PublicKey> = listOf(bobKey)
    internal val outputBasketOfApplesState: BasketOfApples = BasketOfApples(
        outputBasketOfApplesStateDescription,
        outputBasketOfApplesStateFarm,
        outputBasketOfApplesStateOwner,
        outputBasketOfApplesStateWeight,
        outputBasketOfApplesStateParticipants
    )

    // Helper function to create input AppleStamp state when building a transaction for contract testing.
    // The argument for outputState for this and the next helper function is set to the default apple state for happy path scenarios.
    // To capture negative test cases or other edge cases, they are written within individual tests.
    protected fun createInputStateAppleStamp(outputState: AppleStamp = outputAppleStampState): StateAndRef<AppleStamp> {
        val transaction = buildTransaction {
            addOutputState(outputState)
            addCommand(AppleCommands.Issue())
            addSignatories(outputState.participants)
        }
        return transaction.toLedgerTransaction().getOutputStateAndRefs(AppleStamp::class.java).single()
    }

    // Helper function to create input BasketOfApples state when building a transaction for contract testing.
    protected fun createInputStateBasketOfApples(outputState: BasketOfApples = outputBasketOfApplesState): StateAndRef<BasketOfApples> {
        val transaction = buildTransaction {
            addOutputState(outputState)
            addCommand(AppleCommands.PackBasket())
            addSignatories(outputState.participants)
        }
        return transaction.toLedgerTransaction().getOutputStateAndRefs(BasketOfApples::class.java).single()
    }
}