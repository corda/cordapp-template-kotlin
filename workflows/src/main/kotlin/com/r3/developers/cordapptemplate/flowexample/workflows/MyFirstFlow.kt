package com.r3.developers.cordapptemplate.flowexample.workflows

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import org.slf4j.LoggerFactory

// A class to hold the deserialized arguments required to start the flow.
class MyFirstFlowStartArgs(val otherMember: MemberX500Name)


// A class which will contain a message, It must be marked with @CordaSerializable for Corda
// to be able to send from one virtual node to another.
@CordaSerializable
class Message(val sender: MemberX500Name, val message: String)


// MyFirstFlow is an initiating flow, it's corresponding responder flow is called MyFirstFlowResponder (defined below)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatingFlow(protocol = "my-first-flow")
// MyFirstFlow should inherit from ClientStartableFlow, which tells Corda it can be started via an REST call from a client
class MyFirstFlow: ClientStartableFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    // Corda has a set of injectable services which are injected into the flow at runtime.
    // Flows declare them with @CordaInjectable, then the flows have access to their services.

    // JsonMarshallingService provides a Service for manipulating json
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // FlowMessaging provides a service for establishing flow sessions between Virtual Nodes and
    // sending and receiving payloads between them
    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup



    // When a flow is invoked its call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services.
    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        // Useful logging to follow what's happening in the console or logs
        log.info("MFF: MyFirstFlow.call() called")

        // Show the requestBody in the logs - this can be used to help establish the format for starting a flow on corda
        log.info("MFF: requestBody: ${requestBody.getRequestBody()}")

        // Deserialize the Json requestBody into the MyfirstFlowStartArgs class using the JsonSerialisation Service
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, MyFirstFlowStartArgs::class.java)

        // Obtain the MemberX500Name of counterparty
        val otherMember = flowArgs.otherMember

        // Get our identity from the MemberLookup service.
        val ourIdentity = memberLookup.myInfo().name

        // Create the message payload using the MessageClass we defined.
        val message = Message(otherMember, "Hello from $ourIdentity.")

        // Log the message to be sent.
        log.info("MFF: message.message: ${message.message}")

        // Start a flow session with the otherMember using the FlowMessaging service
        // The otherMember's Virtual Node will run the corresponding MyFirstFlowResponder responder flow
        val session = flowMessaging.initiateFlow(otherMember)

        // Send the Payload using the send method on the session to the MyFirstFlowResponder Responder flow
        session.send(message)

        // Receive a response from the Responder flow
        val response = session.receive(Message::class.java)

        // The return value of a ClientStartableFlow must always be a String, this String will be passed
        // back as the REST response when the status of the flow is queried on Corda.
        return response.message
    }
}

// MyFirstFlowResponder is a responder flow, it's corresponding initiating flow is called MyFirstFlow (defined above)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatedBy(protocol = "my-first-flow")
// Responder flows must inherit from ResponderFlow
class MyFirstFlowResponder: ResponderFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup


    // Responder flows are invoked when an initiating flow makes a call via a session set up with the Virtual
    // node hosting the Responder flow. When a responder flow is invoked, its call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services/
    // The Call method has the flow session passed in as a parameter by Corda so the session is available to
    // responder flow code, you don't need to inject the FlowMessaging service.
    @Suspendable
    override fun call(session: FlowSession) {

        // Useful logging to follow what's happening in the console or logs
        log.info("MFF: MyFirstResponderFlow.call() called")

        // Receive the payload and deserialize it into a Message class
        val receivedMessage = session.receive(Message::class.java)

        // Log the message as a proxy for performing some useful operation on it.
        log.info("MFF: Message received from ${receivedMessage.sender}: ${receivedMessage.message} ")

        // Get our identity from the MemberLookup service.
        val ourIdentity = memberLookup.myInfo().name

        // Create a response to greet the sender
        val response = Message(ourIdentity,
            "Hello ${session.counterparty.commonName}, best wishes from ${ourIdentity.commonName}")

        // Log the response to be sent.
        log.info("MFF: response.message: ${response.message}")

        // Send the response via the send method on the flow session
        session.send(response)
    }
}
/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "r1",
    "flowClassName": "com.r3.developers.cordapptemplate.flowexample.workflows.MyFirstFlow",
    "requestBody": {
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */