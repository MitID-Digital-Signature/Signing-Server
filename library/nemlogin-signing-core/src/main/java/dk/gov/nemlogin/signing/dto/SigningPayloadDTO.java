package dk.gov.nemlogin.signing.dto;

import dk.gov.nemlogin.signing.model.SigningPayload;

import java.io.Serializable;
import java.util.Base64;

/**
 * Constitutes a DTO form of the {@link SigningPayload} class
 * suitable for passing on to the signing client.
 */
@SuppressWarnings("unused")
public class SigningPayloadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** WTS signed and encoded signature parameters **/
    private String signatureParameters;

    /** Base64-encoded DTBS (Data To Be Signed) **/
    private String dtbs;


    /** Constructor **/
    public SigningPayloadDTO() {
    }

    /** Constructor **/
    public SigningPayloadDTO(String signatureParameters, String dtbs) {
        this.signatureParameters = signatureParameters;
        this.dtbs = dtbs;
    }

    /** Constructor **/
    public SigningPayloadDTO(SigningPayload signingPayload) {
        this.signatureParameters = signingPayload.getSignatureParameters();
        this.dtbs = Base64.getEncoder().encodeToString(signingPayload.getDtbs().getData());
    }


    /** {@inheritDoc} **/
    @Override
    public String toString() {
        return "{ " +
            "\"signatureParameters\": \"" + signatureParameters + "\", " +
            "\"dtbs\": \"" + dtbs + "\"" +
            "}";
    }


    public String getSignatureParameters() {
        return signatureParameters;
    }

    public void setSignatureParameters(String signatureParameters) {
        this.signatureParameters = signatureParameters;
    }

    public String getDtbs() {
        return dtbs;
    }

    public void setDtbs(String dtbs) {
        this.dtbs = dtbs;
    }
}
