# nemlogin-signing-validation

Simple service for validating a signed document by calling the public NemLog-In Signature Validation API.

## When to use

Only include this dependency if you actually perform signature validation in your application.

## Prerequisite

* Java 17
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* Apache HttpClient
  * Apache httpclient version 5.2.3
* Jackson
  * jackson-databind version 2.12.0
