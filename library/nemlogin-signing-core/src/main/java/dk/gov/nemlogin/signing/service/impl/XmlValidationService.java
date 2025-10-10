package dk.gov.nemlogin.signing.service.impl;

import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.model.SignersDocument.HtmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.XmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocumentFile;
import dk.gov.nemlogin.signing.service.ServiceLoaderFactory;
import dk.gov.nemlogin.signing.service.SignersDocumentValidationService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Validates the XML Signer's Document by performing OWASP checks on XML and XSL,
 * and by validating that the HTML produced from the XSLT is valid SignHTML.
 */
public class XmlValidationService implements SignersDocumentValidationService {
    private static final Logger LOG = LoggerFactory.getLogger(XmlValidationService.class);

    static final String XSL_VERSION_NAME = "version";
    static final String XSL_VERSION = "3.0";
    static final String W3C_XSL_SCHEME = "http://www.w3.org/1999/XSL/Transform";

    static final String XSL_IMPORT = "import";
    static final String XSL_INCLUDE = "include";


    /** {@inheritDoc} **/
    @Override
    public boolean supports(DocumentFormat sdFormat) {
        return DocumentFormat.XML == sdFormat;
    }


    /** {@inheritDoc} **/
    @Override
    public void validate(TransformationContext ctx) throws NemLogInException {
        LOG.info("Validating XML/XSL");
        checkWellformedXML(ctx);
        checkWellformedXSL(ctx);
        checkXsl(ctx);
        checkHTML(ctx);
    }


    /**
     * Checks that the HTML generated from the XSLT is valid SignHTML
     * @param ctx the {@link TransformationContext}
     */
    public void checkHTML(TransformationContext ctx) throws NemLogInException {
        XmlSignersDocument sd = (XmlSignersDocument) ctx.getSignersDocument();
        Source xslInputSource =  new StreamSource(new ByteArrayInputStream(sd.getXsltData()));
        Source xmlInputSource = new StreamSource(new ByteArrayInputStream(sd.getData()));

        var htmlResultStream = new ByteArrayOutputStream();
        try {
            final var transformerFactory = TransformerFactory.newDefaultInstance();
            // XML parsers should not be vulnerable to XXE attacks java:S2755
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            transformerFactory
                .newTransformer(xslInputSource)
                .transform(xmlInputSource, new StreamResult(htmlResultStream));
        } catch (TransformerException e) {
            throw new ValidationException(ErrorCode.SDK010, ctx, "Error while transforming XML to HTML", e);
        }

        SignersDocumentFile htmlResult = SignersDocumentFile.builder()
            .setName(SigningUtils.fileName(ctx.getSignersDocument().getName(), "html"))
            .setData(htmlResultStream.toByteArray())
            .build();

        var htmlValidator = ServiceLoaderFactory.getSignersDocumentValidationService(DocumentFormat.HTML);
        htmlValidator.validate(new TransformationContext(new HtmlSignersDocument(htmlResult), null, ctx.getSignatureParameters(), null));
    }



    /**
     * Simple error handler to be used in SAX parser
     */
    public static class SimpleErrorHandler implements ErrorHandler {
        private String errMsg;

        public String getErrMsg() {
            return errMsg;
        }
        public void warning(SAXParseException e) {
            setErrMsg(e.getMessage());
        }

        public void error(SAXParseException e) {
            setErrMsg(e.getMessage());
        }

        public void fatalError(SAXParseException e) {
            setErrMsg(e.getMessage());
        }

        public void setErrMsg(String errMsg) {
            if(errMsg.contains("DOCTYPE is disallowed")) {
                this.errMsg = "DOCTYPE is not allowed";
            } else {
                this.errMsg = errMsg;
            }
        }
    }


    /**
     * Parse xml document to check for wellformed-ness
     *
     * @param ctx the {@link TransformationContext} containing the Signers document and other relevant transformation parameters
     */
    public void checkWellformedXML(TransformationContext ctx) throws NemLogInException {
        String errMsg = parseXML(new InputSource(new ByteArrayInputStream(ctx.getSignersDocument().getData())));
        if (errMsg != null) {
            throw new ValidationException(ErrorCode.SDK010, ctx, "Error validating XML for wellformed-ness: " + errMsg);
        }
    }

    /**
     * Parse xsl document to check for wellformed-ness
     *
     * @param ctx the {@link TransformationContext} containing the Signers document and other relevant transformation parameters
     */
    public void checkWellformedXSL(TransformationContext ctx) throws NemLogInException {
        String errMsg = parseXML(new InputSource(new ByteArrayInputStream(((XmlSignersDocument) ctx.getSignersDocument()).getXsltData())));
        if (errMsg != null) {
            throw new ValidationException(ErrorCode.SDK010, ctx, "Error validating XSL for wellformed-ness: " + errMsg);
        }
    }

    /**
     * Check XSL
     * - check for schema version 3.0
     * - check for disallowed include/import tags
     *
     * @param ctx the {@link TransformationContext}
     * @throws ValidationException if not valid
     */

    public void checkXsl(TransformationContext ctx) throws NemLogInException {
        try {
            var factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            var saxParser = factory.newSAXParser();
            // XML parsers should not be vulnerable to XXE attacks (java:S2755)
            saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            var isVersion30 = new AtomicBoolean(false);
            DefaultHandler defaultHandler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (isVersion30(attributes, uri)) {
                        isVersion30.set(true);
                    }
                    if (isImportOrInclude(localName, uri)) {
                        throw new SAXException(new ValidationException(ErrorCode.SDK010, ctx, "Import or Include in XSL schema is not allowed"));
                    }
                }
            };
            var inputSource = new InputSource(new ByteArrayInputStream(((XmlSignersDocument) ctx.getSignersDocument()).getXsltData()));
            saxParser.parse(inputSource, defaultHandler);
            if(!isVersion30.get()) {
                throw new ValidationException(ErrorCode.SDK010, ctx, "XSL schema must be version 3.0");
            }
        } catch (SAXException | ParserConfigurationException | IOException | ValidationException e) {
            throw new ValidationException(ErrorCode.SDK010, ctx, "Error when creating parser from SD", e);
        }
    }

    /**
     * Parse XML for wellformed-ness using SAX
     *
     * @param inputSource xml source to be parsed
     * @return errmsg or null if success
     */
    private String parseXML(final InputSource inputSource) {
        var factory = SAXParserFactory.newInstance();
        var errorHandler = new SimpleErrorHandler();
        try {
            // by disallowing doctype we prevent XML External Entity (XEE) attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            var reader = factory.newSAXParser().getXMLReader();
            reader.setErrorHandler(errorHandler);
            reader.parse(inputSource);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            // error set in errorHandler
            LOG.trace("error set in errorHandler", e);
        }

        return errorHandler.getErrMsg();
    }

    /**
     * Check for version 3.0
     *
     * @param attributes SAX attributes
     * @return if version 3.0
     */
    private boolean isVersion30(final Attributes attributes, final String elemUri) {
        for (var i = 0; i < attributes.getLength(); i++) {
            if ((W3C_XSL_SCHEME.equalsIgnoreCase(attributes.getURI(i)) || W3C_XSL_SCHEME.equalsIgnoreCase(elemUri))
                && XSL_VERSION_NAME.equalsIgnoreCase(attributes.getLocalName(i))
                && XSL_VERSION.equals(attributes.getValue(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for any import or include tags
     * @param localName tagname stripped from namespace
     * @return true if tag is import or include
     */
    private boolean isImportOrInclude(final String localName, final String elemUri) {
        return W3C_XSL_SCHEME.equalsIgnoreCase(elemUri) && (XSL_INCLUDE.equalsIgnoreCase(localName) || XSL_IMPORT.equalsIgnoreCase(localName));
    }
}
