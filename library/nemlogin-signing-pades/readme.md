# nemlogin-signing-pades

PAdES pre-signing and file attachments based on the [Apache PDFBox project](https://pdfbox.apache.org/).

## When to use

Only include this dependency if you produce PAdES-based DTBS (Data-To-Be-Signed) documents.

Alternatively, you could provide your own implementation of the SignatureStamperService and SourceAttachmentService services.

## Prerequisite

* Java 11+ 
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* Apache PDFBox
  * pdfbox version 2.0.24
  * commons-logging version 1.2

## Transformation Properties

The service supports the following properties, which can be added to the
"TransformationContext.transformationProperties" field in order to control the 
PDF generation behaviour.

All properties have a "nemlogin.signing.pades." prefix, 
excluded for brevity below.

| Property                | Default Value | Description |
|-------------------------|---------------|-------------|
| reset-signature-content | true          | If true, zero out the signature content.<br>If false, preserve the signature content |
