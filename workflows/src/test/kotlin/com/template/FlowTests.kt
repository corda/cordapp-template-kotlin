package com.template

import com.template.contracts.AppointmentContract
import com.template.flows.BookAppointmentRequest
import com.template.flows.DecideAppointmentAnswer
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.template.states.AppointmentState
import java.util.concurrent.Future;
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import com.template.flows.RequestAvailability
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.StateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault.StateStatus
import org.junit.runner.Request
import java.time.LocalDateTime


class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")
        ),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB")))))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }


//    @Test
//    fun `DummyTest`() {
//        val flow = Initiator(b.info.legalIdentities[0], AppointmentContract.Commands.CreateAppointment())
//        val future: Future<SignedTransaction> = a.startFlow(flow)
//        network.runNetwork()
//
//        //successful query means the state is stored at node b's vault. Flow went through.
//         val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
//         val states = b.services.vaultService.queryBy(AppointmentState::class.java, inputCriteria).states[0].state.data
//    }


    @Test
    @Throws(IllegalArgumentException::class)
    fun `RequestAvailabilityOnlyShouldSuccess`() {
        val requestAvailabilityFlow = RequestAvailability(b.info.legalIdentities[0])
        val future: Future<List<LocalDateTime>> = a.startFlow(requestAvailabilityFlow)

        network.runNetwork()
        print("future values are:  ${future.get()}")
    }

    @Test
    @Throws(IllegalArgumentException::class)
    fun `BookAppointmentOnlyShouldSucess`() {
        val bookAppointmentRequestFlow = BookAppointmentRequest(b.info.legalIdentities[0], LocalDateTime.of(2022, 6, 6, 22, 10, 30), "I want to book an appointment due to severe knee pain")
        val future: Future<SignedTransaction> = a.startFlow(bookAppointmentRequestFlow)

        network.runNetwork()
        print("future values are:  ${future.get().sigs}")
    }

    @Test
    @Throws(IllegalArgumentException::class)
    fun `CreateStateInTheVaultShouldSuccess`() {
        val requestAvailabilityFlow = RequestAvailability(b.info.legalIdentities[0])
        val future: Future<List<LocalDateTime>> = a.startFlow(requestAvailabilityFlow)

        network.runNetwork()

        val bookAppointmentRequestFlow = BookAppointmentRequest(b.info.legalIdentities[0], LocalDateTime.of(2022, 6, 6, 22, 10, 30), "First Booking")
        val future2: Future<SignedTransaction> = a.startFlow(bookAppointmentRequestFlow)

        network.runNetwork()

        val bookAppointmentRequestFlow2 = BookAppointmentRequest(b.info.legalIdentities[0], LocalDateTime.of(2022, 6, 6, 21, 10, 30), "Second Booking")
        val future22: Future<SignedTransaction> = a.startFlow(bookAppointmentRequestFlow2)

        network.runNetwork()

        print("The signed transcation is ${future2.get()}")

        val states = a.services.vaultService.queryBy(AppointmentState::class.java)

        print("vault has1 ${states.states}     --")


        val decideAppointmentAnswerFlow = DecideAppointmentAnswer(a.info.legalIdentities[0], states.states.first().ref, false)
        val future3: Future<SignedTransaction> = b.startFlow(decideAppointmentAnswerFlow)
        network.runNetwork()

        print("The signed transcation2 is ${future3.get()}")
    }


//    @Test
//    @Throws(IllegalArgumentException::class)
//    fun `DecideAppointmentAnswerOnlyShouldSucess`() {
//
//        val states = b.services.vaultService.queryBy(AppointmentState::class.java)
//        print("the states are ${states}")

//        val decideAppointmentAnswerFlow = DecideAppointmentAnswer(a.info.legalIdentities[0],
//                AppointmentState("Test triggering appointment answer",
//                        b.info.legalIdentities[0],
//                        a.info.legalIdentities[0],
//                        LocalDateTime.now(),
//                        listOf(b.info.legalIdentities[0], a.info.legalIdentities[0])) as StateRef)
//
//        val future: Future<String> = b.startFlow(decideAppointmentAnswerFlow)

//        network.runNetwork()

//        print("final step return is : ${future.get()}")
//    }

//    @Test
//    @Throws(IllegalArgumentException::class)
//    fun `RequestAvailabilityShouldSuccess`(){
//        val requestAvailabilityFlow = RequestAvailability(b.info.legalIdentities[0])
//        val future: Future<LocalDateTime> =  a.startFlow(requestAvailabilityFlow)
//        network.runNetwork()
//
//        val bookAppointmentFlow =  BookAppointmentRequest(b.info.legalIdentities[0], future.get())
//        val result : Future<SignedTransaction> = a.startFlow(bookAppointmentFlow)
//        network.runNetwork()
//
//
//        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
//        print("input is ${inputCriteria}")
//        val states = b.services.vaultService.queryBy(AppointmentState::class.java, inputCriteria).states[0].state.data
//
//        print("Future is ${states}")
//    }
}