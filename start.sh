#!/bin/bash

umask 002
cd /var/www/signering
java -jar examples/nemlogin-signing-webapp/target/nemlogin-signing-webapp-1.0.17-exe.jar
