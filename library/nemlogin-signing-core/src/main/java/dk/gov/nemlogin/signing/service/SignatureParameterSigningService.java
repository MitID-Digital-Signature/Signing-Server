package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.SignatureParameterSigningException;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.model.SignatureParameters;

/**
 * Defines the Service used for JWS-signing {@link SignatureParameters}.
 * <p>
 * The actual implementation is loaded using the Java ServiceLoader via
 * a call to {@link ServiceLoaderFactory}
 */
@SuppressWarnings("unused")
public interface SignatureParameterSigningService extends NemLogInService<Void> {

    /**
     * Signs the {@code params} using JWS
     *
     * @param params the {@link SignatureParameters} to sign
     * @param signatureKeys the Service Provider signature keys
     * @return the JWS-signed parameters
     */
    String jwsSign(SignatureParameters params, SignatureKeys signatureKeys)
        throws SignatureParameterSigningException;
}
