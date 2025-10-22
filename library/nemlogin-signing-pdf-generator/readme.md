# nemlogin-signing-pdf-generator

HTML-to-PDF transformation based on the [Open HTML to PDF project](https://github.com/danfickle/openhtmltopdf).

## When to use

Only include this dependency if you produce PDF-based DTBS (Data-To-Be-Signed) documents 
from plain text, HTML or XML-based SD's (Signer's Documents).

Alternatively, you could provide your own implementation of the FormatTransformationService service.

## Prerequisite

* Java 11+ 
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* Open HTML to PDF
  * openhtmltopdf-core version 1.0.10
  * openhtmltopdf-pdfbox version 1.0.10
  * pdfbox version 2.0.26
  * xmpbox version 2.0.17
  * graphics2d version 0.25
* JSoup
  * jsoup version 1.14.2

## Transformation Properties

The service supports the following properties, which can be added to the
"TransformationContext.transformationProperties" field in order to control the 
PDF generation behaviour.

All properties have a "nemlogin.signing.pdf-generator." prefix, 
excluded for brevity below.

| Property        | Default Value      | Description |
|-----------------|--------------------|-------------|
| color-profile   | default            | "default" adds a default color profile.<br>"none" adds no profile.<br>All other values are treated as a path to a .icc file and should have protocol prefix like "classpath:" or "file:"|
| fonts           | default            | "default" adds support for 14 standard PDF fonts.<br>"embed" will embed the following list of fonts.<br>"default, embed" does both. |
| font[x].name    |                    | Name of the x'th font to embed. |
| font[x].path    |                    | Path of the x'th font to embed. Should have protocol prefix like "classpath:" or "file:" |
| body-font       | medium Helvetica   | The CSS font for the HTML &lt;body> element. The font must be a default PDF font or embedded. |    
| monospace-font  | medium Courier     | The CSS font for the HTML &lt;pre> element. The font must be a default PDF font or embedded. |   
| page-size       | a4 portrait        | The CSS 2.1 @page size. |
| page-margin     | 1cm                | The CSS 2.1 @page margin. |
| page-style      |                    | The page-style will be injected in the HTML as a &lt;style> element.<br>If defined, page-size and page-margin is ignored. |
