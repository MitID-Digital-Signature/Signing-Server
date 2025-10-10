package dk.gov.nemlogin.signing.exceptions;

/**
 * NemLog-In Signing SDK error codes
 */
public enum ErrorCode {
    SDK001("Error loading SD"),
    SDK002("Invalid Signature Parameters"),
    SDK003("Service Implementation unavailable"),
    SDK004("Error JWS-signing Signing Payload"),
    SDK005("Error generating DTBS signature template"),
    SDK006("Error computing DTBS digest"),
    SDK007("Error transforming SD to PDF DTBS document"),
    SDK008("Error adding attachments to PDF DTBS document"),
    SDK009("Error transforming SD to XML DTBS document"),
    SDK010("Error validating SD"),
    SDK011("Error validating document signature")
    ;


    private final String message;

    /** Constructor **/
    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
