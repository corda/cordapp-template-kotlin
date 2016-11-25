# CorDapp Template

Welcome to the CorDapp Template. This README is an abridged version of
the [CorDapp tutorial](http://docs.corda.net/tutorial-cordapp.html) found on
the Corda docsite.

The CorDapp template is a example CorDapp and accompanying tests which you can
use to bootstrap your own CorDapp projects.

**Instead, if you are interested in exploring the Corda codebase itself,
contributing to the core Corda platform or viewing and running sample
demos then clone the [corda repository](https://github.com/corda/corda).**

The code in the CorDapp template implements the _"Hello World"_ of
CorDapps. It allows users of a Corda node to generate and send purchase
orders to other nodes. You can also enumerate all the purchase orders
which have been agreed with other nodes. The nodes also provide a simple
web interface which can be used to interact with the CorDapp.

## Pre-requisites

You will need the following installed on your machine before you can start:

* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed and available on your path.
* Latest version of
  [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (note the
  community edition is free)
* [h2 web console](http://www.h2database.com/html/download.html)
  (download the "platform-independent zip)"
* git

For more detailed information, see the
[pre-requisites](https://docs.corda.net/pre-requisites.html) page on the
Corda docsite.

## Getting Started

To get started, clone this repository with:

     git clone https://github.com/corda/cordapp-template.git

Change directories to the newly cloned repo:

     cd cordapp-template
     
Build the CorDapp template:
 
**Unix:** 

     ./gradlew deployNodes
     
**Windows:**

     gradle.bat deployNodes
     
Note. You will be building the example CorDapp. If you want to make any
changes they should be made before building, of course!
     
Gradle will grab all the dependencies for you from Maven and then build 
the sample application and create several local Corda nodes. Once the
build concludes, change directories to the folder where the newly built
nodes are located:

     cd build/nodes
     
The Gradle build script will have created a folder for each node. You'll
see three folders, one for each node and a `runnodes` script. You can
run the nodes with:

**Unix:**

     sh runnodes

**Windows:**

    runnodes.bat

You should now have four Corda nodes running on your machine serving 
the example CorDapp.

When the nodes have booted up you should see a message like: 

     Node started up and registered in 5.007 sec
     
in the console.

For a much more detailed description of building and running the Example
CorDapp see the
[Cordapp tutorial](https://docs.corda.net/tutorial-cordapp.html) on the
Corda docsite.

## The Example CorDapp

The Example CorDapp implements a basic scenario where a buyer wishes to
submit purchase orders to a seller. The scenario defines four nodes:

* **Controller** which hosts the network map service and validating notary
  service.
* **NodeA** who is the buyer.
* **NodeB** who is the seller.
* **NodeC** an unrelated third party.

NodeA can generate purchase orders for lists and quantities of items and
associated metadata such as delivery address and delivery date. The
flows used to facilitate the agreement process always result in an
agreement with the seller as long as the purchase order meets the
contract constraints which are defined in `PurchaseOrderContract.kt`.

All agreed purchase orders between NodeA and NodeB become "shared facts"
between NodeA and NodeB. Note that NodeC won't see any of these
transactions or have copies of any of the resulting `PurchaseOrderState`
objects. This is because data is only propagated on a need-to-know
basis.

## Interacting with the CorDapp

The CorDapp defines a couple of HTTP API end-points and also serves some
static web content. The end-points allow you to list agreements and add
agreements.

The nodes can be found using the following port numbers, defined in
`build.gradle` and the respective `node.conf` file for each node found
in `build/nodes/NodeX`` etc:

     NodeA: localhost:10005
     NodeB: localhost:10007
     NodeC: localhost:10009

Also, as the nodes start-up they should tell you which host and port the
embedded web server is running on. The API endpoints served are as follows:

     /api/example/who-am-i
     /api/example/get-peers
     /api/example/purchase-orders
     /api/example/{COUNTERPARTY}/create-purchase-order
     
The static web content is served from:

     /web/example
     
A purchase order can be created via accessing the
`create-purchase-order` end-point directly or through the the web form
hosted at `/web/example`.

**NOTE: The content in `web/example` is only available for demonstration
purposes and does not implement any anti-XSS/XSRF security techniques. Do
not copy such code directly into products meant for production use.**

**Submitting a purchase order via HTTP API:**

To create a purchase order from NodeA to NodeB, use:

     echo '{"orderNumber": "1","deliveryDate": "2018-09-15","deliveryAddress": {"city": "London","country": "UK"},"items" : [{"name": "widget","amount": "3"},{"name": "thing","amount": "4"}]}' | curl -T - -H 'Content-Type: application/json' http://localhost:10005/api/example/NodeB/create-purchase-order

note the port number `10005` (NodeA) and `NodeB` referenced in the
end-point path. This command instructs NodeA to create and send an order
to NodeB. Upon verification and completion of the process, both nodes
(but not NodeC) will have a signed, notarised copy of the purchase order.

**Submitting a purchase order via ``web/example``:**

Click the "Create purchase order" button at the top left of the
page and enter the purchase order details, e.g.

     Counter-party: Node B
     Order Number:  1
     Delivery Date: 2018-09-15
     City:          London
     Country Code:  UK
     Item 1 name:   Things
     Item 1 amount: 5

and click "Create purchase order". The modal dialogue should close.

To check what validation is performed on the purchase order data, have a look 
at the `Place` class in `PurchaseOrderContract.kt`. For example, Entering a
'Country Code' other than 'UK' will cause the verify function to return an
Exception in the terminal and logs.

**Once a purchase order has been submitted:**

Inspect the terminal for the nodes. You should see some activity in the
terminal windows for NodeA and NodeB:

*NodeA:*

     Constructing proposed purchase order.
     Sending purchase order to seller for review.
     Received partially signed transaction from seller.
     Verifying signatures and contract constraints.
     Signing transaction with our private key.
     Obtaining notary signature.
     Recording transaction in vault.
     Sending fully signed transaction to seller.
     Done

*NodeB:*

     Receiving proposed purchase order from buyer.
     Generating transaction based on proposed purchase order.
     Signing proposed transaction with our private key.
     Sending partially signed transaction to buyer and wait for a response.
     Verifying signatures and contract constraints.
     Recording transaction in vault.
     Done

*NodeC:*

     You shouldn't see any activity.

Next you can view the newly created purchase order by accessing the
vault of NodeA or NodeB:

**Via the HTTP API:**

For NodeA. navigate to
`http://localhost:10005/api/example/purchase-orders`. For NodeB,
navigate to `http://localhost:10007/api/example/purchase-orders`.

**Via web/example:**

Navigate to `http://localhost:10005/web/example/` and click the refresh
button at the top left-hand side of the page. You should see the newly
created purchase order on the page.

## Accessing the h2 database via h2 web console

You can connect to the h2 database to see the current state of the
ledger, among other data such as the network map cache.

Firstly, navigate to the folder where you downloaded the h2 web console
as part of the pre-requisites section, above.

Change directories to the bin folder:

     ``cd h2/bin``
     
Where there are a bunch of shell scripts and batch files. Run the web
console:

**Unix:**

     ``sh h2.sh``
     
**Windows::**

     ``h2.bat``
     
The h2 web console should start up in a web browser tab. To connect we
first need to obtain a JDBC connection string. Each node outputs its
connection string in the terminal window as it starts up. In a terminal
window where a node is running, look for the following string:

     ``Database connection url is              : jdbc:h2:tcp://10.18.0.150:56736/node``
     
you can use the string on the right to connect to the h2 database: just
paste it in to the `JDBC URL` field and click *Connect*.

You will be presented with a web application that enumerates all the
available tables and provides an interface for you to query them using SQL.

## Using the Example RPC client

The `ExampleClientRPC.kt` file is a simple utility which uses the client
RPC library to connect to a node and log the 'placed' purchase orders.
It will log any existing purchase orders and listen for any future
purchase orders. To build the client use the following gradle task:

     ./gradlew runExampleClientRPC
     
To run the client:

**Via IntelliJ:**

Select the 'Run Example RPC Client' run configuration which, by default,
connects to NodeA (Artemis port 10004). Click the Green Arrow to run the
client. 

**Via command line:**

Run the following gradle task:

     ./gradlew runExampleClientRPC
     
The RPC client should output some purchase order objects to the console.

## Further Reading

Tutorials and developer docs for CorDapps and Corda are
[here](https://docs.corda.r3cev.com/tutorial-cordapp.html).

## TODO

* Make the progress tracker steps more instructive
* Amend the Jython RPC demo such that it works with PurchaseOrderStates
* Fix the `web/example` front end
* Add a schema for PurchaseOrderState
* Add an attachment to the 'Place' transaction
* Add contract unit tests
* Add flow unit tests
* Add an integration test
