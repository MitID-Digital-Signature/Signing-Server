package dk.gov.nemlogin.signing.exceptions;

/**
 * Base NemLog-In exception class.
 */
public class NemLogInException extends Exception {

    private final ErrorCode errorCode;

    /** Constructor **/
    public NemLogInException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    /** Constructor **/
    public NemLogInException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, errorCode.getMessage(), cause);
    }

    /** Constructor **/
    public NemLogInException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /** Designated Constructor **/
    public NemLogInException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
