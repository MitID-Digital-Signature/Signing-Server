# nemlogin-signing-html-validator
HTML validation using the OWASP HTML Sanitizer with some minor modifications. HTML and CSS is validated against whitelists and must adhere to the SignHTML specification.

## When to use
Only include this dependency if you sign HTML or XML/XSL Signer's Documents. The XmlValidatorService will call this HtmlValidatorService to validate the transformed html output.
Alternatively, you could provide your own implementation of the ValidationService interface which supports for DocumentFormat.HTML.

## Prerequisite

* Java 11+ 
* Maven

## Transitive Dependencies and Credits

* SignSDK Core Library
  * nemlogin-signing-core + transitive dependencies.
* OWASP HTML Sanitizer 
  * owasp-java-html-sanitizer version 20191001.1
  * jsoup version 1.14.2
  * guava version 29.0-jre + transitive dependencies.

## Acknowledge
This validation has been heavily "inspired" from the OpenSign NemID project.

## OWASP HTML Sanitizer
To support for validating CSS styles in HTML, [OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer)
is added to the project, and a subset is replaced to limit HTML input.

### Files changed. Changes are marked in the files.
OWASP HTML Sanitizer source files are copied to the nemlogin-signing-html-validator jar and
* org.owasp.html.CssGrammar
* org.owasp.html.CssTokens
* org.owasp.html.HtmlChangeReporter
  is replaced to limit HTML input.

*NB* do not include java-html-sanitizer as a dependency!

### Copyrights
OWASP Java HTML Sanitizer is Apache License Version 2.0. 
http://www.apache.org/licenses/LICENSE-2.0.txt

Modified classes are Copyright (c) 2011, Mike Samuel
All rights reserved.

## Stand-Alone CLI Tool

The *nemlogin-signing-html-validator* module can also be built into a stand-alone CLI utility which may be used 
for validating individual or folders of HTML files according to the SignHTML standard.

Build the module using:

    mvn clean package -Pstandalone-html-validator

The resulting Jar is called to validate individual HTML files or folders of HTML files using:

    java -jar target/nemlogin-signing-html-validator-cli.jar path-to-files-or-folders
