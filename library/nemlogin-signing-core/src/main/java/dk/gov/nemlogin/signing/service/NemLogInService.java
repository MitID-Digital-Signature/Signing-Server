package dk.gov.nemlogin.signing.service;

/**
 * Services loaded via the {@link ServiceLoaderFactory} should implement this interface.
 */
public interface NemLogInService<T> {

    /**
     * If multiple services implementing this interface are found,
     * the {@link ServiceLoaderFactory} returns the one with the highest priority.
     *
     * @return the priority of this service;
     */
    default int getPriority() {
        return 0;
    }


    /**
     * If this service only supports certain types of {@code parameters} it should implement this method.
     *
     * @param params parameters passed along to the {@link ServiceLoaderFactory} when looking up a matching service
     * @return if this service handles the given parameters.
     */
    default boolean supports(T params) { return true; }
}
