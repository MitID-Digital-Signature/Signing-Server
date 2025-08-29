# nemlogin-signing-validation

Simple service for validating a signed document by calling the public NemLog-In Signature Validation API.

## When to use

Only include this dependency if you actually perform signature validation in your application.

## Prerequisite

* Java 11+ 
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* Apache HttpClient
  * Apache httpclient version 4.5.13
  * Apache httpmime version 4.5.13
* Jackson
  * jackson-databind version 2.12.0
