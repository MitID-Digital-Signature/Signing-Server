package dk.gov.nemlogin.signing.exceptions;

import dk.gov.nemlogin.signing.model.SignatureParameters;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK002;

/**
 * Thrown if the {@link SignatureParameters} are invalid
 */
@SuppressWarnings("unused")
public class InvalidSignatureParametersException extends NemLogInException {

    private final SignatureParameters signatureParameters;

    /** Constructor **/
    public InvalidSignatureParametersException(SignatureParameters signatureParameters, String message) {
        super(SDK002, message);
        this.signatureParameters = signatureParameters;
    }

    /** Constructor **/
    public InvalidSignatureParametersException(SignatureParameters signatureParameters, String message, Throwable cause) {
        super(SDK002, message, cause);
        this.signatureParameters = signatureParameters;
    }

    public SignatureParameters getSignatureParameters() {
        return signatureParameters;
    }
}
