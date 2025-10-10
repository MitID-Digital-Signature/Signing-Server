package dk.gov.nemlogin.signing.exceptions;

import dk.gov.nemlogin.signing.model.SignatureParameters;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK004;

/**
 * Thrown when WTS signing the {@link SignatureParameters} fails
 */
@SuppressWarnings("unused")
public class SignatureParameterSigningException extends NemLogInException {

    private final SignatureParameters signatureParameters;

    /** Constructor **/
    public SignatureParameterSigningException(SignatureParameters signatureParameters, String message) {
        super(SDK004, message);
        this.signatureParameters = signatureParameters;
    }

    /** Constructor **/
    public SignatureParameterSigningException(SignatureParameters signatureParameters, String message, Throwable cause) {
        super(SDK004, message, cause);
        this.signatureParameters = signatureParameters;
    }

    public SignatureParameters getSignatureParameters() {
        return signatureParameters;
    }
}
