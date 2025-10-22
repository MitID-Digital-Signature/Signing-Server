package dk.gov.nemlogin.signing.pdf.service;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import dk.gov.nemlogin.signing.AbstractSigningTest;
import dk.gov.nemlogin.signing.dto.SigningPayloadDTO;
import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.model.FlowType;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Test generating a signing payload.
 * <p>
 * Error codes tested {@link ErrorCode#SDK010}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PdfSigningPayloadTest extends AbstractSigningTest {

    public static final String PDF_PATH = "/EmptyPage.pdf";
    public static final String SIGNED_PDF_PATH = "/EmptyPage-signed.pdf";

    private final SigningPayloadService signingPayloadService = new SigningPayloadService();

    /**
     * Tests a full transformation of PDF SD -> PDF DTBS.
     */
    @Test
    @Order(1)
    void testSigningPayloadService() throws Exception {
        TransformationContext ctx = prepareTransformationContext(
            PDF_PATH,
            DocumentFormat.PDF,
            SignatureFormat.PAdES);

        SigningPayloadDTO result = signingPayloadService.produceSigningPayloadDTO(ctx);

        // Verify the signed parameters
        JWSObject jwsObject = JWSObject.parse(result.getSignatureParameters());
        JWSVerifier verifier = new RSASSAVerifier(signatureKeys.getPublicKey());
        Assertions.assertTrue(jwsObject.verify(verifier));
    }


    /**
     * Test that we cannot sign a PDF that already contains a signature element
     */
    @Test
    @Order(2)
    void testAlreadySignedPayloadService() throws NemLogInException {
        TransformationContext ctx = prepareTransformationContext(
            SIGNED_PDF_PATH,
            DocumentFormat.PDF,
            SignatureFormat.PAdES);

        Assertions.assertThrows(
            ValidationException.class,
            () -> signingPayloadService.produceSigningPayloadDTO(ctx));
    }

    /**
     * Test sanity check of missing transformation context
     */
    @Test
    void testMissingCtx() throws NemLogInException {
        try {
            signingPayloadService.produceSigningPayloadDTO(null);
            Assertions.fail("Should fail with ValidationException");
        } catch (ValidationException e) {
            Assertions.assertEquals(ErrorCode.SDK010, e.getErrorCode());
            Assertions.assertEquals("Transformation context is null", e.getMessage());
        }
    }

    /**
     * Test sanity check of missing signer's document
     */
    @Test
    void testMissingSignersDocument() {
        SignatureParameters signatureParameters = SignatureParameters.builder()
            .setFlowType(FlowType.ServiceProvider)
            .setEntityID(entityID)
            .setDocumentFormat(DocumentFormat.TEXT)
            .setSignatureFormat(SignatureFormat.PAdES)
            .setReferenceText(null)
            .build();

        try {
            new TransformationContext(null, signatureKeys, signatureParameters);
            Assertions.fail("Should fail with ValidationException");
        } catch (ValidationException e) {
            Assertions.assertEquals(ErrorCode.SDK010, e.getErrorCode());
            Assertions.assertEquals("Signer's document is null", e.getMessage());
        }
    }

    /**
     * Test sanity check on missing file containing signer's document
     */
    @Test
    void testMissingFile() throws NemLogInException {
        SignatureParameters signatureParameters = SignatureParameters.builder()
            .setFlowType(FlowType.ServiceProvider)
            .setEntityID(entityID)
            .setDocumentFormat(DocumentFormat.TEXT)
            .setSignatureFormat(SignatureFormat.PAdES)
            .setReferenceText(null)
            .build();

        SignersDocument signersDocument = new SignersDocument.PlainTextSignersDocument(null, false);
        try {
            TransformationContext ctx = new TransformationContext(signersDocument, signatureKeys, signatureParameters);
            signingPayloadService.produceSigningPayloadDTO(ctx);
            Assertions.fail("Should fail with ValidationException");
        } catch (ValidationException e) {
            Assertions.assertEquals(ErrorCode.SDK010, e.getErrorCode());
            Assertions.assertEquals("File containing Signer's document is null", e.getMessage());
        }
    }
}
