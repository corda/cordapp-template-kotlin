# CorDapp Template 

This template contains the build system and an example application required to get started with Corda.

## Prerequisites

You will need to have [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
installed and available on your path.

## Getting Started

First clone this repository and the Corda repository locally. Then open a terminal window in the Corda (r3prototyping) directory and run:
 
**Unix:** 

     ./gradlew publishToMavenLocal
     
**Windows:**

     gradle.bat publishToMavenLocal
     
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

You will now have nodes running on your machine serving this CorDapp. You can now begin developing your CorDapp.

## Further Reading

Tutorials and developer docs for CorDapps and Corda are [here](https://docs.corda.r3cev.com/creating-a-cordapp.html).