package dk.gov.nemlogin.signing.pdf.service;

import dk.gov.nemlogin.signing.AbstractSigningTest;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test generating a signing payload.
 */
class PdfValidationTest extends AbstractSigningTest {

    public static final String PDF_PATH = "/whitelist-error.pdf";

    private final SigningPayloadService signingPayloadService = new SigningPayloadService();

    /**
     * Tests a validation.
     */
    @Test
    void testValidationService() throws NemLogInException {
        TransformationContext ctx = prepareTransformationContext(
            PDF_PATH,
            DocumentFormat.PDF,
            SignatureFormat.PAdES);

        try {
            signingPayloadService.produceSigningPayloadDTO(ctx);
        } catch (ValidationException e) {
            // Should throw ValidationException
            return;
        }
        Assertions.fail("Should throw ValidationException " + PDF_PATH);
    }
}
