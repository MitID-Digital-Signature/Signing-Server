package dk.gov.nemlogin.signing.exceptions;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK003;

/**
 * Thrown when no service implementation can be found using the Java ServiceLoader mechanism
 */
@SuppressWarnings("unused")
public class ServiceUnavailableException extends NemLogInException {

    private final Class<?> serviceClass;

    /** Constructor **/
    public ServiceUnavailableException(Class<?> serviceClass, String message) {
        super(SDK003, message);
        this.serviceClass = serviceClass;
    }

    /** Constructor **/
    public ServiceUnavailableException(Class<?> serviceClass, String message, Throwable cause) {
        super(SDK003, message, cause);
        this.serviceClass = serviceClass;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }
}
