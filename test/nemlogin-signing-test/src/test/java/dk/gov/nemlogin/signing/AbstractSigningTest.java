package dk.gov.nemlogin.signing;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.model.FlowType;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.PlainTextSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.HtmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.PdfSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.XmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocumentFile;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SignatureKeysLoader;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.junit.jupiter.api.BeforeEach;

import java.io.InputStream;
import java.util.Properties;

/**
 * Abstract class to be implemented by Signing tests
 */
@SuppressWarnings("SameParameterValue")
public abstract class AbstractSigningTest {

    protected Properties props = new Properties();
    protected String entityID;
    protected SignatureKeys signatureKeys;

    /**
     * Load the {@link SignatureKeys} before each test
     */
    @BeforeEach
    protected void loadSignatureKeys() throws Exception {

        // Load properties from application.properties
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            props.load(in);
        }

        // Load signing keys
        signatureKeys = new SignatureKeysLoader()
            .setKeystoreClassPath(props.getProperty("nemlogin.signing.keystore-class-path"))
            .setKeystorePassword(props.getProperty("nemlogin.signing.keystore-password"))
            .setKeyPairAlias(props.getProperty("nemlogin.signing.key-pair-alias"))
            .setPrivateKeyPassword(props.getProperty("nemlogin.signing.private-key-password"))
            .loadSignatureKeys();

        // Load SP or Broker entity ID
        entityID = props.getProperty("nemlogin.signing.entity-id");
    }


    /**
     * Creates a template {@link TransformationContext} given SD class path.
     * @param sdClassPath the path to the Signer's Document
     * @param sdFormat the Signer's Document format
     * @param signatureFormat the Data-To-Be-Signed format
     * @return the template {@link TransformationContext}
     */
    protected TransformationContext prepareTransformationContext(
        String sdClassPath,
        DocumentFormat sdFormat,
        SignatureFormat signatureFormat) throws NemLogInException {

        // Strip "/"
        String fileName = sdClassPath.substring(1);

        SignersDocumentFile file = SignersDocumentFile.builder()
            .setClassPath(sdClassPath)
            .setName(fileName)
            .build();

        SignersDocument signersDocument;
        switch (sdFormat) {
            case PDF:
                signersDocument = new PdfSignersDocument(file);
                break;
            case HTML:
                signersDocument = new HtmlSignersDocument(file);
                break;
            case TEXT:
                signersDocument = new PlainTextSignersDocument(file, false);
                break;
            case XML:
                signersDocument = new XmlSignersDocument(file, getXslt(sdClassPath));
                break;
            default:
                throw new IllegalArgumentException("Unknown SD format: " + sdFormat.name());
        }

        SignatureParameters signatureParameters = SignatureParameters.builder()
            .setFlowType(FlowType.ServiceProvider)
            .setEntityID(entityID)
            .setDocumentFormat(sdFormat)
            .setSignatureFormat(signatureFormat)
            .setReferenceText("Signing " + fileName)
            .build();

        return new TransformationContext(signersDocument, signatureKeys, signatureParameters);
    }


    /**
     * For XML files, look for a corresponding XSL file with the same file name but
     * or ".xsl" file extension
     * @param fileName the class path to the xml file
     * @return the associated XSL
     */
    private SignersDocumentFile getXslt(String fileName) throws NemLogInException {

        String xsltPath = SigningUtils.fileName(fileName, ".xsl");
        return SignersDocumentFile.builder()
            .setClassPath(xsltPath)
            .build();
    }
}
