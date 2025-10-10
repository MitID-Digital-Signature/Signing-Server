# nemlogin-signing-xades

XAdES pre-signing.

## When to use

Only include this dependency if you produce XAdES-based DTBS (Data-To-Be-Signed) documents.

Alternatively, you could provide your own implementation of the SignatureStamperService and SourceAttachmentService services.

## Prerequisite

* Java 11+ 
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* Apache Santuario
  * xmlsec version 2.2.3
* JAXB (Only at compile time)
  * jaxb-api version 2.3.1
  * jaxb-runtime version 2.3.1
