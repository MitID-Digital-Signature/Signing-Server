package dk.gov.nemlogin.signing.exceptions;

import dk.gov.nemlogin.signing.service.TransformationContext;

/**
 * Thrown when transforming a SD to an DTBS fails
 */
@SuppressWarnings("unused")
public class TransformationException extends NemLogInException {

    private final transient TransformationContext ctx;

    /**
     * Constructor
     **/
    public TransformationException(ErrorCode errorCode, TransformationContext ctx, String message) {
        super(errorCode, message);
        this.ctx = ctx;
    }

    /**
     * Constructor
     **/
    public TransformationException(ErrorCode errorCode, TransformationContext ctx, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.ctx = ctx;
    }

    public TransformationContext getCtx() {
        return ctx;
    }
}
