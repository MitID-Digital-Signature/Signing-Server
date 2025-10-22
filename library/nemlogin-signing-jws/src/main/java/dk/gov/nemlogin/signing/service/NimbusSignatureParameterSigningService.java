package dk.gov.nemlogin.signing.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import dk.gov.nemlogin.signing.exceptions.SignatureParameterSigningException;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.util.SigningUtils;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

/**
 * Service for processing and signing {@link SignatureParameters} using the Nimbus JWS implementation:
 * https://connect2id.com/products/nimbus-jose-jwt
 */
@SuppressWarnings("unused")
public class NimbusSignatureParameterSigningService implements SignatureParameterSigningService {

    private static final Logger LOG = LoggerFactory.getLogger(NimbusSignatureParameterSigningService.class);

    private static final JWSAlgorithm JWS_ALGORITHM = JWSAlgorithm.PS256;

    static {
        // JWSAlgorithm.PS256 requires Bouncy Castle
        SigningUtils.addBouncyCastleProvider();
    }

    /** {@inheritDoc} **/
    @Override
    public String jwsSign(SignatureParameters params, SignatureKeys signatureKeys)
        throws SignatureParameterSigningException {

        try {
            long t0 = System.currentTimeMillis();
            LOG.debug("Start JWS-signing signature parameters");
            if (LOG.isTraceEnabled()) {
                LOG.trace("Signing Certificate: {}", signatureKeys.getCertificate().getSubjectDN());
                LOG.trace("Signature parameters: {}", params);
            }

            // The signing client should only be passed the actual SP VOCES certificate, not the rest of the chain
            List<Base64> encodedCertificateChain = Collections.singletonList(base64encode(params, signatureKeys.getCertificate()));

            JWSSigner signer = new RSASSASigner(signatureKeys.getPrivateKey());

            // Add the signing certificate to the "x5c" header
            var jwsHeader = new JWSHeader.Builder(JWS_ALGORITHM)
                .x509CertChain(encodedCertificateChain)
                .build();

            // Encode the SingingParameters as JSON
            var jsonSigningParams = new JSONObject(params.asMap());
            var jwsPayload = new Payload(jsonSigningParams);

            // Create the JWS payload and sign it
            var jwsObject = new JWSObject(jwsHeader, jwsPayload);
            jwsObject.sign(signer);

            LOG.info("JWS-signed signature parameters in {} ms", System.currentTimeMillis() - t0);

            return jwsObject.serialize();
        } catch (Exception e) {
            throw new SignatureParameterSigningException(params, "Error signing signature parameters", e);
        }
    }


    /**
     * Returns a {@link Base64} representation of the certificate suitable for the JWS header
     *
     * @param params
     * @param certificate the certificate to encode
     * @return the {@link Base64} representation of the certificate
     */
    private Base64 base64encode(SignatureParameters params, X509Certificate certificate) throws SignatureParameterSigningException {
        try {
            return new Base64(java.util.Base64.getEncoder().encodeToString(certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new SignatureParameterSigningException(params, "Cannot encode certificate " + certificate.getSerialNumber(), e);
        }
    }
}
