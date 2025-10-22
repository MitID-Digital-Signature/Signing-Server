package dk.gov.nemlogin.signing.exceptions;

import dk.gov.nemlogin.signing.service.TransformationContext;

/**
 * Thrown when validating SD fails
 */
@SuppressWarnings("unused")
public class ValidationException extends NemLogInException {

    private final TransformationContext ctx;

    /** Constructor **/
    public ValidationException(ErrorCode errorCode, TransformationContext ctx, String message) {
        super(errorCode, message);
        this.ctx = ctx;
    }

    /** Constructor **/
    public ValidationException(ErrorCode errorCode, TransformationContext ctx, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.ctx = ctx;
    }

    public TransformationContext getCtx() {
        return ctx;
    }
}
