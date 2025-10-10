# nemlogin-broker-mock

Example Spring Boot-based web application, which illustrates how Brokers may integrate
with SignSDK and NemLog-In Signing API.

This module should be disregarded by Service Providers.

The code is intended as mock code and is not for production usage.

Furthermore, the details of updating and extending the PAdES and XAdES 
into PAdES-B-LTA respectively XAdES-B-LTA are left to the Broker, 
and must follow the specification detailed in the NemLog-In documentation.

## Documentation
* NL3 Signature SP implementation guidelines
* NL3 Signature Broker implementation guidelines

## Prerequisite

* Java 11+
* Maven

## Running the Example Webapp

First, build the entire SDK from the root project folder:

    mvn clean install
    
Next, in the _nemlogin-broker-mock_ folder, start the webapp using:

    mvn spring-boot:run
    
or start the main class from your favourite IDE.

Access the webapp at: https://localhost:8667
