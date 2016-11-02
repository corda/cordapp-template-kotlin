#!/usr/bin/env bash

# Edit the below as appropriate.
# Download jython from http://www.jython.org/
# We find 2.7.0 works acceptably

VERSION=0.5-SNAPSHOT

export JYTHON=/Users/richardgreen/jython2.7.0/bin/jython

export CLASSPATH=~/.m2/repository/com/r3corda/client/$VERSION/client-$VERSION.jar:~/.m2/repository/com/r3corda/contracts/$VERSION/contracts-$VERSION.jar:~/.m2/repository/com/r3corda/corda/$VERSION/corda-$VERSION.jar:~/.m2/repository/com/r3corda/core/$VERSION/core-$VERSION.jar:~/.m2/repository/com/r3corda/node/$VERSION/node-$VERSION.jar:../../../../../r3prototyping/build/install/r3prototyping/lib/*

$JYTHON $*
