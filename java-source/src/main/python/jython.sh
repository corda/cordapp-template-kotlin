#!/usr/bin/env bash

# Edit the below as appropriate.
# Download jython from http://www.jython.org/
# We find 2.7.0 works acceptably
# Either put jython in your path or change the variable below to the correct location

export JYTHON=/path/to/jython

if ! [ -x "$(command -v $JYTHON)" ]; then
  echo "jython not in path or \$JYTHON variable is not set to the correct location (please edit $0)"
  exit
fi

export JYTHON_LIB_DIR=../../../build/jythonDeps

if ! [ -d $JYTHON_LIB_DIR ]; then
    echo "Please run './gradlew installJythonDeps' in the root folder of this project"
    exit
fi

export CLASSPATH=$JYTHON_LIB_DIR/*:../../../build/libs/*

$JYTHON $*
