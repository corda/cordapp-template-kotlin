# CorDapp Template 

This template contains the build system and an example application required to get started with Corda.

## Prerequisites

You will need to have [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
installed and available on your path.

## Getting Started

First clone this repository and the Corda repository locally. Then open a terminal window in the Corda (r3prototyping) directory and run:
 
**Unix:** 

     ./gradlew install
     
**Windows:**

     gradle.bat install
     
This will publish a copy of Corda to your local Maven repository for your CorDapp to use. Next open a terminal window
in your CorDapp directory (this one) and run:

**Unix:**

     ./gradlew deployNodes
     
**Windows:**

     gradlew.bat deployNodes
     
This command will create several nodes in `build/nodes` of the cordapp-template folder that you can now run with:

**Unix:**

     cd build/nodes
     ./runnodes

**Windows:**

Windows users currently have to manually enter each directory in `build/nodes` and run `java -jar corda.jar` in each.
This will be updated soon.

You will now have nodes running on your machine serving this CorDapp. 

## Interacting with the CorDapp

The CorDapp defines a couple of HTTP API end-points and serves some static web content. They allow you to list agreements and add agreements.

The nodes can be found using the following port numbers, defined in `build.gradle`:

     localhost:10005: NodeA
     localhost:10007: NodeB

To add an agreement, use the following command to add an agreement between NodeA and NodeB:

**Unix:**

     echo '{ "swapRef": "0", "data": "badger" }' | curl -T - -H 'Content-Type: application/json' http://localhost:10005/api/example/NodeB/create-deal

To view all agreements navigate to `http://localhost:10005/api/example/deals`.

## Technical webinar code

For those of you who watched the Corda technical Webinar on November 8th. The code from the session is available on the branch `technical-webinar-code`. 

You can access the web interface for the demo from:

     NodeA: http://localhost:10005/web/example/
     NodeB: http://localhost:10007/web/example/

You can add new agreements and view all agreements on the web page. Use the refresh button once you've added an agreement. The client is written using jQuery and Bootstrap. All JS libraries are obtained via a CDN, so the webpage wont work if you are trying it offline. If you are, then use `curl` instead to add agreements. E.g.:

     echo '{ "id": "0", "message": "badger" }' | curl -T - -H 'Content-Type: application/json' http://localhost:10005/api/example/NodeB/create-agreement

## Further Reading

Tutorials and developer docs for CorDapps and Corda are [here](https://docs.corda.r3cev.com/creating-a-cordapp.html).
