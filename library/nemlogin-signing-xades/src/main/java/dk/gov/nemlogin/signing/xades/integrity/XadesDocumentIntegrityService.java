package dk.gov.nemlogin.signing.xades.integrity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;

import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.SignedDocumentIntegrityException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.IntegrityValues;
import dk.gov.nemlogin.signing.service.DocumentIntegrityService;
import dk.gov.nemlogin.signing.service.TransformationContext;

public class XadesDocumentIntegrityService implements DocumentIntegrityService {

    private static final String SIGN_TEXT_PATH = "//*[local-name()='Reference' and @Type='http://dk.gov.certifikat/nemlogin#SignText']/*[local-name()='DigestValue']";

    @Override
    public IntegrityValues createIntegrityValues(TransformationContext ctx) throws NemLogInException {
        if (ctx.getDataToBeSigned().getFormat() != SignatureFormat.XAdES) {
            throw new NemLogInException(ErrorCode.SDK012, "Unable to create integrity values, unknown format: " + ctx.getDataToBeSigned().getFormat());
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            var doc = builder.parse(new ByteArrayInputStream(ctx.getDataToBeSigned().getData()));

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            return new IntegrityValues(
                    SignatureFormat.XAdES,
                    ctx.getSignatureParameters().getDtbsDigestAlgorithm(),
                    Base64.getDecoder().decode(xpath.evaluate(SIGN_TEXT_PATH, doc)),
                    0,
                    0,
                    0
                    );
        } catch (Exception e) {
            throw new NemLogInException(ErrorCode.SDK012, "Unable to extract embedded digest.", e);
        }
    }

    @Override
    public void verifySignedDocument(byte[] signedDocument, IntegrityValues integrityValues) throws SignedDocumentIntegrityException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            var doc = builder.parse(new ByteArrayInputStream( signedDocument ) );

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            if ( Arrays.compare(Base64.getDecoder().decode(xpath.evaluate(SIGN_TEXT_PATH, doc).getBytes()), integrityValues.getOriginalDigest()) != 0 ) {
                throw new SignedDocumentIntegrityException(integrityValues, "Document integrity verification failed: Digest mismatch.");
            }
        }
        catch (ParserConfigurationException | SAXException | XPathExpressionException | IOException e ) {
            throw new SignedDocumentIntegrityException(integrityValues, "Document integrity verification failed: Parse error." );
        }
    }

    @Override
    public boolean supports(SignatureFormat params) {
        return SignatureFormat.XAdES.equals(params);
    }
}
