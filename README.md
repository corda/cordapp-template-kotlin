![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# CorDapp Template

Welcome to the CorDapp template. The CorDapp template is a stubbed-out CorDapp 
which you can use to bootstrap your own CorDapp projects.

**This repository previously contained a simple CorDapp example. This 
example has now been moved to 
[cordapp-tutorial](https://github.com/corda/cordapp-tutorial/).**

The template's source code is provided in both Kotlin (under `/kotlin-source`)
and Java (under `/java-source`), and users can write their CorDapps in
either language.

## Pre-Requisites

You will need the following installed on your machine before you can start:

* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
  installed and available on your path.
* Latest version of [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) 
  (note the community edition is free)
* [h2 web console](http://www.h2database.com/html/download.html)
  (download the "platform-independent zip")
* git

For more detailed information, see the
[getting set up](https://docs.corda.net/getting-set-up.html) page on the
Corda docsite.

## Getting Set Up

To get started, clone this repository with:

     git clone https://github.com/corda/cordapp-template.git

And change directories to the newly cloned repo:

     cd cordapp-template

### Releases

It is recommended to checkout a milestone release of Corda. To do this you must run: 

    git checkout release-M12

### Snapshots

**Warning**: Snapshots are very unstable, it is recommended to use a milestone instead.

When building from the latest SNAPSHOT release you must also clone the master
branch of the [corda repository](https://github.com/corda/corda) and then run `gradlew install`
to build the dependencies required for this template.
​     
## Building the CorDapp template:

**Unix:** 

     ./gradlew deployNodes

**Windows:**

     gradlew.bat deployNodes

Note: You'll need to re-run this build step after making any changes to
the template for these to take effect on the node.

## Running the Nodes

Once the build finishes, change directories to the folder where the newly
built nodes are located:

**Kotlin:**

     cd kotlin-source/build/nodes

**Java:**

     cd java-source/build/nodes

The Gradle build script will have created a folder for each node. You'll
see three folders, one for each node and a `runnodes` script. You can
run the nodes with:

**Unix:**

     sh runnodes --log-to-console --logging-level=DEBUG

**Windows:**

    runnodes.bat --log-to-console --logging-level=DEBUG

You should now have four Corda nodes running on your machine serving 
the template.

When the nodes have booted up, you should see a message like the following 
in the console: 

     Node started up and registered in 5.007 sec

## Interacting with the CorDapp via HTTP

The CorDapp defines a couple of HTTP API end-points and also serves some
static web content. Initially, these return generic template responses.

The nodes can be found using the following port numbers, defined in the 
`build.gradle`, as well as the `node.conf` file for each node found
under `kotlin/build/nodes/NodeX` or `java/build/nodes/NodeX`:

     NodeA: localhost:10007
     NodeB: localhost:10010
     NodeC: localhost:10013

As the nodes start up, they should tell you which host and port their
embedded web server is running on. The API endpoints served are:

     /api/template/templateGetEndpoint
     /api/template/templatePutEndpoint

And the static web content is served from:

     /web/template

## Using the Example RPC Client

The `ExampleClientRPC.kt` file is a simple utility which uses the client
RPC library to connect to a node and log its transaction activity.
It will log any existing states and listen for any future states. To build 
the client use the following Gradle task:

     ./gradlew runTemplateClientRPC

To run the client:

**Via IntelliJ:**

Select either the 'Run Template RPC Client - Java' or 'Run Template RPC Client - Kotlin'
run configuration which, by default, connect to NodeA (Artemis port 10005). Click the
Green Arrow to run the client.

**Via the command line:**

Run the following Gradle task:

     ./gradlew runTemplateClientRPC

By default this runs the Java version - to run the Kotlin version change to the
'kotlin-source' directory and run the Gradle task:

     ./gradlew runTemplateClientRPC

## Running the Nodes Across Multiple Machines

The nodes can also be set up to communicate between separate machines on the 
same subnet.

After deploying the nodes, navigate to the build folder (`kotlin/build/
nodes` or `java/build/nodes`) and move some of the individual node folders to 
separate machines on the same subnet (e.g. using a USB key). It is important 
that no nodes - including the controller node - end up on more than one 
machine. Each computer should also have a copy of `runnodes` and 
`runnodes.bat`.

For example, you may end up with the following layout:

* Machine 1: `controller`, `nodea`, `runnodes`, `runnodes.bat`
* Machine 2: `nodeb`, `nodec`, `runnodes`, `runnodes.bat`

You must now edit the configuration file for each node, including the 
controller. Open each node's config file (`[nodeName]/node.conf`), and make 
the following changes:

* Change the artemis address to the machine's ip address (e.g. 
  `artemisAddress="10.18.0.166:10005"`)
* Change the network map address to the ip address of the machine where the 
  controller node is running (e.g. `networkMapAddress="10.18.0.166:10002"`) 
  (please note that the controller will not have a network map address)

Each machine should now run its nodes using `runnodes` or `runnodes.bat` 
files. Once they are up and running, the nodes should be able to communicate 
among themselves in the same way as when they were running on the same machine.

## Further reading

Tutorials and developer docs for CorDapps and Corda are
[here](https://docs.corda.net/).
