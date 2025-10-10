package dk.gov.nemlogin.signing.validation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Validation Report containing the validation result and XML-based ETSI report
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("unused")
public class ValidationReport {

    /** Validation Result */
    private ValidationResult result;

    /** ETSI Validation Report */
    private String etsiReport;


    public ValidationResult getResult() {
        return result;
    }

    public void setResult(ValidationResult result) {
        this.result = result;
    }

    public String getEtsiReport() {
        return etsiReport;
    }

    public void setEtsiReport(String etsiReport) {
        this.etsiReport = etsiReport;
    }
}
