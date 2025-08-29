# nemlogin-signing-jws

Mandatory dependency for the SignSDK. Functionality for JWS-signing of Signature Payload.

## When to use

You must always include this dependency, or create your own implementation of the SignatureParameterSigningService service.

## Prerequisite

* Java 11+ 
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* Nimbus JOSE + JWT project
  * nimbus-jose-jwt version 8.4
  * jcip-annotations version 1.0-1
* Json-smart
  * json-smart version 2.4.7
  * accessors-smart version 1.2
  * asm 5.0.4
