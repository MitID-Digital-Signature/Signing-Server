# NemLog-In Java SignSDK

This project is the official NemLog-In Signing SDK Java implementation for Service Providers and Brokers.

# Documentation

The documentation for using NemLog-In Java SignSDK, along with additional relevant documentation, is found at:
* https://migrering.nemlog-in.dk/nemlog-in-broker/test-og-dokumentation/ 

## Public APIs

The example web applications included in the SDK are configured to use the Test Signing and Validation API endpoints, as defined below:

| API                        | URL                                       |
|----------------------------|-------------------------------------------|
| Test Signing API           | https://underskrift.test-nemlog-in.dk     |
| Production Signing API     | https://underskrift.nemlog-in.dk          |
| Test Validation API        | https://validering.test-nemlog-in.dk      |
| Production Validation API  | https://validering.nemlog-in.dk           |

# SignSDK Library Structure

The SignSDK library has been organized into a set of sub-projects with the aim of reducing 
the number of transitive dependencies for the Service Provider or Broker to a minimum.

The Service Provider should always include *nemlogin-signing-core* and *nemlogin-signing-jws* in their project. 
The rest of the libraries depend on which SD (Signer's Document) formats and DTBS (Data To Be Signed) signature formats
the Service Provider will want to support.

| Library Project                  | SD Formats    | DTBS Format | Description |
|----------------------------------|---------------|-------------|-------------|
| nemlogin-signing-core            | All           | All         | Mandatory dependency. Contains core models and service definitions. |
| nemlogin-signing-jws             | All           | All         | JWS signing of Signature Parameters. |
| nemlogin-signing-xades           |               | XAdES       | Generating and pre-signing XAdES. |
| nemlogin-signing-pades           |               | PAdES       | Pre-signing PAdES and handle file attachments. |
| nemlogin-signing-pdf-generator   | TEXT,HTML,XML | PAdES       | HTML-to-PDF transformation used for generating PDF from Signer's Documents of type TEXT, HTML and XML. |
| nemlogin-signing-pdf-validator   | PDF           |             | SignPDF validating Signer's Documents of type PDF. |
| nemlogin-signing-html-validator  | HTML,XML      |             | SignHTML validating Signer's Documents of type HTML and XML+XSL. |
| nemlogin-signing-spring-boot     |               |             | Thin wrapper of *nemlogin-signing-core* for use in Spring Boot projects. |
| nemlogin-signing-validation      |               |             | Service for using the public NemLog-In Validation API for signature validation. |

The core project will load needed services using the standard Java service loader mechanism. The Service Provider
may thus choose to provide her own implementation of specific functionality, such as JWS signing.

There is also a test project:

| Library Project                  | Description |
|----------------------------------|-------------|
| nemlogin-signing-test            | Relevant tests which serves to demonstrate how to use the SignSDK library.  |

SignSDK also ships with an example project, in the form of a simple Spring Boot web application,
which demonstrates how to use the SignSDK library.

| Example Project                  | Description |
|----------------------------------|-------------|
| nemlogin-signing-webapp          | Example Service Provider web application written in Spring Boot. |
| nemlogin-broker-mock             | Example Broker mock web application written in Spring Boot. The module should be disregarded by Service Providers. |

## Prerequisite

* Java 17
* Maven

# Building

    mvn clean install

# Testing Example Web Application

    java -jar examples/nemlogin-signing-webapp/target/nemlogin-signing-webapp-0.0.4.exe.jar

Access webapp on http://localhost:8080

## Changelog

### Version 2.0.2

Moved readme.md to README.md
Changes to start.sh to do shutdown and restart. And work without hardcoded paths. 
Made changes to not have fixed folders for signed documunents. It can now be set via the appl....
* examples/nemlogin-signing-webapp/src/main/java/dk/gov/nemlogin/signing/service/DocumentSigningService.java
* examples/nemlogin-signing-webapp/src/main/java/dk/gov/nemlogin/signing/service/SigningResultService.java

Modifies sign-complete.html to redirect to sign.php as not to break the flow with the drupal modules. 
* examples/nemlogin-signing-webapp/src/main/resources/templates/sign-complete.html

Added settings for the application settings file for changing folder paths for signing documents. If the settings are not there the defaults are used. 
* examples/nemlogin-signing-webapp/src/main/resources/default.application.yaml

### Version 2.0.2

Fixed  bug related to wrong .pom file uploaded in version 2.0.1 of the zip

### Version 2.0.1

Updated package Java version from 11.0.2 to jdk : temurin-17.0.9 for Windows and jdk-17.0.9+9 for Mac
Updated Spring Boot version to 3.2 in example webapps

### Version 1.0.4
Updated internal IdP test certificate used by both sample applications from:
```"nemlog-in signsdk demo !0028funktionscertifikat!0029-fe9eac67-356c-40dc-8623-502e39bb64a5)"```
to OCES3 certificate:
```"nemlog-in qualifiedsigning.testsp - interntest"```

### Version 1.0.3
* Upgraded libraries with Vulnerabilities

### Version 1.0.2
* Protect signing webapp client with iframe sandbox mode
* Protect signing webapp client from unsafe web messaging
* Adding Correlation-Id to signing webapp client
* Add PDF producer as 'NemLog-In SP Java SDK (version)' for PDF->PDF transformations
* Upgraded libraries with Vulnerabilities

### Version 0.0.4
* Updated internal IdP test certificate used by both sample applications from:  
```"sp and wsc (oiosaml-net.dk test) (funktionscertifikat)"```
to: 
```"nemlog-in signsdk demo !0028funktionscertifikat!0029-fe9eac67-356c-40dc-8623-502e39bb64a5)"```
* Entity-id changed from "https://saml.serviceprovider.dk/login" to: "https://signsdk-demo.nemlog-in.dk"
* Changed broker mock web application to use Cryptomathic Signer 5.2 API

### Version 0.0.3
* initial release (before completed CTI environment)

### Version 0.0.2
* Added broker mock web application

### Version 0.0.1
* Initial release (internal)
