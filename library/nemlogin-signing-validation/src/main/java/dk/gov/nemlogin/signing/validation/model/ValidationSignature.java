package dk.gov.nemlogin.signing.validation.model;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Encapsulates the validation result for a Signature
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("unused")
public class ValidationSignature {

    /**
     * An indication of the validation result.
     * Inspired by the DSS implementation of {@code eu.europa.esig.dss.enumerations.Indication}.
     * Source ETSI EN 319 102-1
     */
    public enum Indication {
        TOTAL_PASSED,
        TOTAL_FAILED,
        INDETERMINATE
    }

    /**
     * An sub-indication of the validation result.
     * Based on the DSS implementation of {@code eu.europa.esig.dss.enumerations.SubIndication}.
     * Source ETSI EN 319 102-1
     */
    public enum SubIndication {
        FORMAT_FAILURE,
        HASH_FAILURE,
        SIG_CRYPTO_FAILURE,
        REVOKED,
        SIG_CONSTRAINTS_FAILURE,
        CHAIN_CONSTRAINTS_FAILURE,
        CERTIFICATE_CHAIN_GENERAL_FAILURE,
        CRYPTO_CONSTRAINTS_FAILURE,
        EXPIRED,
        NOT_YET_VALID,
        POLICY_PROCESSING_ERROR,
        SIGNATURE_POLICY_NOT_AVAILABLE,
        TIMESTAMP_ORDER_FAILURE,
        NO_SIGNING_CERTIFICATE_FOUND,
        NO_CERTIFICATE_CHAIN_FOUND,
        REVOKED_NO_POE,
        REVOKED_CA_NO_POE,
        OUT_OF_BOUNDS_NO_POE,
        OUT_OF_BOUNDS_NOT_REVOKED,
        CRYPTO_CONSTRAINTS_FAILURE_NO_POE,
        NO_POE,
        TRY_LATER,
        SIGNED_DATA_NOT_FOUND
    }

    /**
     * Signature profiles (form+level) handled by the SD-DSS framework.
     * Inspired by DSS implementation of {@code eu.europa.esig.dss.enumerations.SignatureLevel}.
     */
    @SuppressWarnings("java:S115")
    public enum SignatureFormat {
        XML_NOT_ETSI,
        XAdES_C,
        XAdES_X,
        XAdES_XL,
        XAdES_A,
        XAdES_BASELINE_LTA,
        XAdES_BASELINE_LT,
        XAdES_BASELINE_T,
        XAdES_BASELINE_B,
        CMS_NOT_ETSI,
        CAdES_BASELINE_LTA,
        CAdES_BASELINE_LT,
        CAdES_BASELINE_T,
        CAdES_BASELINE_B,
        CAdES_101733_C,
        CAdES_101733_X,
        CAdES_101733_A,
        PDF_NOT_ETSI,
        PAdES_BASELINE_LTA,
        PAdES_BASELINE_LT,
        PAdES_BASELINE_T,
        PAdES_BASELINE_B,
        PKCS7_B,
        PKCS7_T,
        PKCS7_LT,
        PKCS7_LTA,
        UNKNOWN
    }

    /**
     * ETSI signature qualification levels.
     * Inspired by the DSS implementation of {@code eu.europa.esig.dss.enumerations.SignatureQualification}.
     */
    public enum SignatureLevel {
        QESIG,
        QESEAL,
        QES,
        ADESIG_QC,
        ADESEAL_QC,
        ADES_QC,
        ADESIG,
        ADESEAL,
        ADES,
        INDETERMINATE_QESIG,
        INDETERMINATE_QESEAL,
        INDETERMINATE_QES,
        INDETERMINATE_ADESIG_QC,
        INDETERMINATE_ADESEAL_QC,
        INDETERMINATE_ADES_QC,
        INDETERMINATE_ADESIG,
        INDETERMINATE_ADESEAL,
        INDETERMINATE_ADES,
        NOT_ADES_QC_QSCD,
        NOT_ADES_QC,
        NOT_ADES,
        NA
    }

    /** Signature Format **/
    private SignatureFormat signatureFormat;

    /** Signature Level **/
    private SignatureLevel signatureLevel;

    /** Main validation result indication - ETSI EN 319 102-1 **/
    private Indication indication;

    /** Validation result sub-indication - ETSI EN 319 102-1 **/
    private SubIndication subIndication;

    /** Signing time **/
    private Date signingTime;

    /** Signer name **/
    private String signedBy;

    /** Signer e-mail **/
    private String email;

    /** Certificate chain. First certificate is the actual signer certificate **/
    private List<ValidationCertificate> certificateChain = new ArrayList<>();

    /** List of error messages **/
    private List<String> errors;

    /** List of warning messages **/
    private List<String> warnings;

    /** List of information messages **/
    private List<String> infos;

    public SignatureFormat getSignatureFormat() {
        return signatureFormat;
    }

    public void setSignatureFormat(SignatureFormat signatureFormat) {
        this.signatureFormat = signatureFormat;
    }

    public SignatureLevel getSignatureLevel() {
        return signatureLevel;
    }

    public void setSignatureLevel(SignatureLevel signatureLevel) {
        this.signatureLevel = signatureLevel;
    }

    public Indication getIndication() {
        return indication;
    }

    public void setIndication(Indication indication) {
        this.indication = indication;
    }

    public SubIndication getSubIndication() {
        return subIndication;
    }

    public void setSubIndication(SubIndication subIndication) {
        this.subIndication = subIndication;
    }

    public Date getSigningTime() {
        return signingTime != null ? (Date) signingTime.clone() : null;
    }

    public void setSigningTime(Date signingTime) {
        this.signingTime = signingTime != null ? new Date(signingTime.getTime()) : null;
    }

    public String getSignedBy() {
        return signedBy;
    }

    public void setSignedBy(String signedBy) {
        this.signedBy = signedBy;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<ValidationCertificate> getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain(List<ValidationCertificate> certificateChain) {
        this.certificateChain = certificateChain;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getInfos() {
        return infos;
    }

    public void setInfos(List<String> infos) {
        this.infos = infos;
    }
}
