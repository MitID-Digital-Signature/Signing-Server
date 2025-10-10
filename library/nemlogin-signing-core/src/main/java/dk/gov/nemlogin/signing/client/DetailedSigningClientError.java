package dk.gov.nemlogin.signing.client;

/**
 * Detailed error code and message which may be returned from the Signing Client
 * and included in the {@link SigningClientError}.
 * <p>
 * The error will either have occurred directly in the Signing Client, or in one of
 * the backend Signing API REST calls performed by the client.
 * <p>
 * The list of valid {@link #errorCode} values, and proposed error texts that could be displayed
 * to end users, is included in the SignSDK.
 */
@SuppressWarnings("unused")
public class DetailedSigningClientError {
  private String errorCode;
  private String errorMessage;

    public String getErrorCode() {
        return errorCode;
    }

    public DetailedSigningClientError setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DetailedSigningClientError setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }
}

