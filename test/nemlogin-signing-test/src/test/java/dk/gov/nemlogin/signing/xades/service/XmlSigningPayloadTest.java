package dk.gov.nemlogin.signing.xades.service;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import dk.gov.nemlogin.signing.AbstractSigningTest;
import dk.gov.nemlogin.signing.dto.SigningPayloadDTO;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Test generating a signing payload.
 */
@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XmlSigningPayloadTest extends AbstractSigningTest {

    public static final String XML_PATH = "/breakfast.xml";

    private final SigningPayloadService signingPayloadService = new SigningPayloadService();

    /**
     * Tests a full transformation of PDF SD -> PDF DTBS.
     */
    @Test
    void testSigningPayloadService() throws Exception {
        TransformationContext ctx = prepareTransformationContext(
            XML_PATH,
            DocumentFormat.XML,
            SignatureFormat.XAdES);

        SigningPayloadDTO result = null;
        try {
            result = signingPayloadService.produceSigningPayloadDTO(ctx);
        } catch (Exception e) {
            Assertions.fail("Should not fail producing signing payload for " + XML_PATH, e);
        }

        // Verify the signed parameters
        JWSObject jwsObject = JWSObject.parse(result.getSignatureParameters());
        JWSVerifier verifier = new RSASSAVerifier(signatureKeys.getPublicKey());
        Assertions.assertTrue(jwsObject.verify(verifier));
    }
}
