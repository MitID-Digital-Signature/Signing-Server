package dk.gov.nemlogin.signing;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.model.FlowType;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.PlainTextSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.HtmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.PdfSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.XmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocumentFile;
import dk.gov.nemlogin.signing.model.SigningPayload;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Test using the {@link SigningPayloadService} for
 * transforming every type of SD's (Signer's Documents) to DTBS (Data-To-Be-Signed)
 * using Service Provider flow and Broker flows
 * <p>
 * Steps:
 * <ul>
 *   <li>Uses the {@link SignersDocumentGenerator} class to generate SD's of type
 *       XML, PDF, HTML and Pain text.</li>
 *   <li>Transform all SD's to DTBS of type PDF for Service Provider flow.</li>
 *   <li>Transform all SD's to DTBS of type XML for Service Provider flow.</li>
 *   <li>Transform all SD's to DTBS of type PDF for Broker flow.</li>
 *   <li>Transform all SD's to DTBS of type XML for Broker flow.</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SigningPayloadServiceTest extends AbstractSigningTest {

    private static final Logger LOG = LoggerFactory.getLogger(SigningPayloadServiceTest.class);

    public static final String SD_ROOT = "./target/signers-documents";
    public static final String DTBS_ROOT = "./target/data-to-be-signed";
    static final String TRANSFORM_FILE_PATTERNS = ".+\\.(?i)(pdf|txt|html|xml)";

    private final SignersDocumentGenerator generator = new SignersDocumentGenerator("file:" + SD_ROOT);
    private final SigningPayloadService signingPayloadService = new SigningPayloadService();


    /**
     * Generate Signers Documents of type XML, PDF, HTML and Pain text.
     */
    @Test
    @Order(1)
    void testGenerateSignersDocuments() {
        try {
            generator.generateSignersDocuments();
        } catch (Exception e) {
            Assertions.fail("Failed generating Signer's Documents", e);
        }
    }


    /** Transform all generated SD to DTBS of type PDF using Service Provider flow */
    @TestFactory
    @Order(2)
    Collection<DynamicTest> transformSdToPdfUsingSpFlow() throws IOException {
        return transform(FlowType.ServiceProvider, SignatureFormat.PAdES);
    }


    /** Transform all generated SD to DTBS of type XML using Service Provider flow */
    @TestFactory
    @Order(3)
    Collection<DynamicTest> transformSdToXmlUsingSpFlow() throws IOException {
        return transform(FlowType.ServiceProvider, SignatureFormat.XAdES);
    }


    /** Transform all generated SD to DTBS of type PDF using Service Provider flow */
    @TestFactory
    @Order(4)
    Collection<DynamicTest> transformSdToPdfUsingBrokerFlow() throws IOException {
        return transform(FlowType.Broker, SignatureFormat.PAdES);
    }


    /** Transform all generated SD to DTBS of type XML using Service Provider flow */
    @TestFactory
    @Order(5)
    Collection<DynamicTest> transformSdToXmlUsingBrokerFlow() throws IOException {
        return transform(FlowType.Broker, SignatureFormat.XAdES);
    }


    /**
     * Transform all generated SD to DTBS of the given type for the given flow
     * @param flowType the flow type
     * @param signatureFormat the signature format
     */
    private Collection<DynamicTest> transform(FlowType flowType, SignatureFormat signatureFormat) throws IOException {
        return Files.list(Paths.get(SD_ROOT))
            .filter(f -> TRANSFORM_FILE_PATTERNS.matches(Objects.requireNonNull(f.getFileName()).toString()))
            .map(file -> DynamicTest.dynamicTest(
                String.format("transform_%s_%s", flowType, Objects.requireNonNull(file.getFileName().toString())),
                () -> transform(file, flowType, signatureFormat)))
            .collect(Collectors.toList());
    }


    /**
     * Transform the SD to DTBS of the given type
     * @param file the file to transform
     * @param flowType the flow type
     * @param signatureFormat the signature format
     */
    private void transform(Path file, FlowType flowType, SignatureFormat signatureFormat) throws IOException, NemLogInException {
        // Instantiate
        String fileName = Objects.requireNonNull(file.getFileName().toString());
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        SignersDocumentFile sdf = SignersDocumentFile.builder()
            .setPath(file.toString())
            .build();
        SignersDocument sd;
        switch (extension) {
            case ".pdf":
                sd = new PdfSignersDocument(sdf);
                break;
            case ".txt":
                sd = new PlainTextSignersDocument(sdf, false);
                break;
            case ".html":
                sd = new HtmlSignersDocument(sdf);
                break;
            case ".xml":
                sd = new XmlSignersDocument(sdf, getXslt(file));
                break;
            default:
                throw new IllegalArgumentException("Invalid Signer's Document type: " + fileName);
        }


        String referenceText = flowType == FlowType.ServiceProvider ? "Signing " + fileName : null;
        SignatureParameters signatureParameters = SignatureParameters.builder()
            .setFlowType(flowType)
            .setEntityID(entityID)
            .setDocumentFormat(sd.getFormat())
            .setSignatureFormat(signatureFormat)
            .setReferenceText(referenceText)
            .build();

        // Transform the SD to PDF
        TransformationContext ctx = new TransformationContext(sd, signatureKeys, signatureParameters);
        SigningPayload signingPayload = null;
        try {
            signingPayload = signingPayloadService.produceSigningPayload(ctx);
        } catch (Exception e) {
            Assertions.fail("Should not fail producing DTBS for " + file, e);
        }

        // Save the generated DTBS to a file. Prepend SD format to file name to get uniqueness
        DataToBeSigned dtbs = signingPayload.getDtbs();
        String dtbsFileName = String.format("%s_%s_%s", flowType, sd.getFormat(), dtbs.getName());
        Path dtbsFile = Paths.get(DTBS_ROOT, dtbsFileName);
        if (!Files.exists(dtbsFile.getParent())) {
            Files.createDirectory(dtbsFile.getParent());
        }
        try (FileOutputStream fos = new FileOutputStream(dtbsFile.toFile())) {
            fos.write(dtbs.getData());
            LOG.info("Wrote DTBS document to: {}", dtbsFile);
        } catch (Exception e) {
            Assertions.fail("Error writing DTBS document to " + file, e);
        }
    }


    /**
     * For XML files, look for a corresponding XSL file with the same file name but
     * or ".xsl" file extension
     * @param file the XML file name
     * @return the associated XSL
     */
    private SignersDocumentFile getXslt(Path file) throws NemLogInException {

        String fileName = Objects.requireNonNull(file.getFileName().toString());
        Path xsltPath = file.getParent().resolve(SigningUtils.fileName(fileName, ".xsl"));
        if (!Files.exists(xsltPath)) {
            throw new IllegalArgumentException("Missing xsl file for " + file);
        }
        return SignersDocumentFile.builder()
            .setPath(xsltPath.toString())
            .build();
    }
}
