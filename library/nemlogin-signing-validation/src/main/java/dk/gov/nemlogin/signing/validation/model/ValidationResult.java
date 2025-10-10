package dk.gov.nemlogin.signing.validation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Validation Result
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("unused")
public class ValidationResult {

    /** Document name */
    private String documentName;

    /** Number of contained signatures */
    private int signaturesCount;

    /** Number of valid signatures */
    private int validSignaturesCount;

    /** Validation result for each signature */
    private List<ValidationSignature> signatures;


    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public int getSignaturesCount() {
        return signaturesCount;
    }

    public void setSignaturesCount(int signaturesCount) {
        this.signaturesCount = signaturesCount;
    }

    public int getValidSignaturesCount() {
        return validSignaturesCount;
    }

    public void setValidSignaturesCount(int validSignaturesCount) {
        this.validSignaturesCount = validSignaturesCount;
    }

    public List<ValidationSignature> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<ValidationSignature> signatures) {
        this.signatures = signatures;
    }
}
