# nemlogin-signing-pdf-validator

Validation of PDF Signer's Documents based on the [Apache PDFBox project](https://pdfbox.apache.org/).

## When to use

Only include this dependency if you sign PDF documents as either PAdES or XAdES.

Alternatively, you could provide your own implementation of the SignersDocumentValidationService service.

## Prerequisite

* Java 11+ 
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* Apache PDFBox
  * pdfbox version 2.0.24
  * commons-logging version 1.2

## Stand-Alone CLI Tool

The *nemlogin-signing-pdf-validator* module can also be built into a stand-alone CLI utility which may be used 
for validating individual or folders of PDF files according to the SignPDF standard.

Build the module using:

    mvn clean package -Pstandalone-pdf-validator

The resulting Jar is called to validate individual PDF files or folders of PDF files using:

    java -jar target/nemlogin-signing-pdf-validator-cli.jar path-to-files-or-folders
