#!/bin/bash

SCRIPT_DIR=`dirname "$BASH_SOURCE"`
DEST_DIR=${1:-file:../../examples/nemlogin-signing-webapp/signers-documents/}
pushd "$SCRIPT_DIR" > /dev/null

echo "*** Ensure that you have run 'mvn clean install' in the project root before running this function ***"

mvn compile exec:java \
  -D"exec.mainClass"="dk.gov.nemlogin.signing.SignersDocumentGenerator" \
  -Dexec.args="$DEST_DIR"

popd > /dev/null
