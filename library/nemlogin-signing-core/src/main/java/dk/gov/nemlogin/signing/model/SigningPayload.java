package dk.gov.nemlogin.signing.model;

import java.io.Serializable;

/**
 * Encapsulates the JWS-encoded {@link SignatureParameters} and {@link DataToBeSigned} as a singing payload
 */
public class SigningPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String signatureParameters;
    private final DataToBeSigned dtbs;

    /** Constructor **/
    public SigningPayload(String signatureParameters, DataToBeSigned dtbs) {
        this.signatureParameters = signatureParameters;
        this.dtbs = dtbs;
    }

    public String getSignatureParameters() {
        return signatureParameters;
    }

    public DataToBeSigned getDtbs() {
        return dtbs;
    }
}
