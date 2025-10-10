#!/bin/bash
# Always run from the folder where start.sh is located
cd "$(dirname "$0")" || exit 1

umask 002
java -jar examples/nemlogin-signing-webapp/target/nemlogin-signing-webapp-1.0.17-exe.jar
