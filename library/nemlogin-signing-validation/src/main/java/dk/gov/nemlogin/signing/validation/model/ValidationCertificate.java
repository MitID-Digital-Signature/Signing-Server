package dk.gov.nemlogin.signing.validation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;

/**
 * Encapsulates a signature certificate
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("unused")
public class ValidationCertificate {

    /** Certificate subjectDN */
    private String subjectDN;

    /** HEX-encoded certificate serial number */
    private String serialNumber;

    /** Certificate not-before date */
    private Date notBefore;

    /** Certificate not-after date */
    private Date notAfter;

    /** Certificate policies - only defined for signing certificate */
    private List<String> policies;


    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getNotBefore() {
        return notBefore !=null ? (Date) notBefore.clone() : null;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore != null ? new Date(notBefore.getTime()) : null;
    }

    public Date getNotAfter() {
        return notAfter !=null ? (Date) notAfter.clone() : null;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter != null ? new Date(notAfter.getTime()) : null;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }
}
