package dk.gov.nemlogin.signing.xades.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import dk.gov.nemlogin.signing.AbstractSigningTest;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.model.FlowType;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.service.ServiceLoaderFactory;
import dk.gov.nemlogin.signing.service.SignatureParameterSigningService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Tests of the {@link SignatureParameterSigningService} service.
 *
 * This test is not dependent on Spring.
 */
class XmlSignatureParameterSigningServiceTest extends AbstractSigningTest {

    private static final Logger LOG = LoggerFactory.getLogger(XmlSignatureParameterSigningServiceTest.class);

    /**
     * Test a programmatically configured SD PDF -> DTBS PDF transformation.
     * Test the content of the generated signing payload.
     */
    @Test
    void testSigningOfSigningParameters() throws Exception {

        // Construct signature parameters
        SignatureParameters signatureParameters = SignatureParameters.builder()
            .setFlowType(FlowType.ServiceProvider)
            .setEntityID(entityID)
            .setDocumentFormat(DocumentFormat.HTML)
            .setSignatureFormat(SignatureFormat.XAdES)
            .setDtbsDigest("XXX")
            .setDtbsSignedInfo("XXX")
            .setReferenceText("signering af xxx")
            .build();

        // Sign the parameters
        String jwsSignedParam = ServiceLoaderFactory.getSignatureParameterSigningService()
            .jwsSign(signatureParameters, signatureKeys);
        LOG.info("Signed content: {}", jwsSignedParam);

        // Parse the generated token
        JWSObject jwsObject = JWSObject.parse(jwsSignedParam);
        JWSHeader header = jwsObject.getHeader();
        Map<String, Object> payload = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject()).getClaims();

        // Check digest algorithm is correct
        Assertions.assertEquals(JWSAlgorithm.PS256, header.getAlgorithm());

        // Validate that the certificate embedded in the JOSE header is the expected one
        Assertions.assertNotNull(header.getX509CertChain());
        X509Certificate cert = header.getX509CertChain().stream()
            .map(base64 -> X509CertUtils.parse(base64.decode()))
            .findFirst()
            .orElse(null);
        Assertions.assertEquals(signatureKeys.getCertificate(), cert);

        // Validate the signature
        JWSVerifier verifier = new RSASSAVerifier(signatureKeys.getPublicKey());
        Assertions.assertTrue(jwsObject.verify(verifier));

        // Validate that the encoded payload is identical to the signature parameters
        Assertions.assertTrue(payloadsEquals(signatureParameters.asMap(), payload));
    }


    /**
     * Compares two payload maps.
     * We compare the values in their string representation, since a serialized int may be de-serialized as a long.
     */
    private boolean payloadsEquals(Map<String, Object> m1, Map<String, Object> m2) {
        return m1.size() == m2.size() &&
            m1.entrySet().stream().allMatch(e -> e.getValue().toString().equals(m2.get(e.getKey()).toString()));
    }
}
