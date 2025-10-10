package dk.gov.nemlogin.signing.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the format of an errors returned from the Signing Client via a postMessage() to the SP page that
 * contains the Signing Client in an iframe.<br>
 * The SP might then choose to return the error payload to the backend and parse it as a SigningClientError.
 * See SignSDK example webapp.
 * <p>
 * The error will either have occurred directly in the Signing Client, or in one of
 * the backend Signing API REST calls performed by the client.
 * <p>
 * The list of valid error codes included in the {@link #details} lists, and the proposed error texts that could be
 * displayed to end users, is included in the SignSDK.
 */
@SuppressWarnings("unused")
public class SigningClientError {

    private Integer httpStatusCode;
    private String timestamp;
    private String message;
    private String correlationId;

    private List<DetailedSigningClientError> details = new ArrayList<>();

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public SigningClientError setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public SigningClientError setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public SigningClientError setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<DetailedSigningClientError> getDetails() {
        return details;
    }

    public SigningClientError setDetails(List<DetailedSigningClientError> details) {
        this.details = details;
        return this;
    }
}

