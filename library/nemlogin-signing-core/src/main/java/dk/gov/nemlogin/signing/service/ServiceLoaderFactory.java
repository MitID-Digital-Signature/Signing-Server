package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.ServiceUnavailableException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.model.ValidTransformation;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparingInt;

/**
 * Looks up services using the Java {@link ServiceLoader} interface.
 * <p>
 * The mechanism is intended to reduce the number of dependencies induced on the Service Provider.<br>
 * If, for instance, the service provider supports PDF SD or DTBS documents, they must either add
 * "nemlogin-signing-pades" as a dependency (including transitive dependencies) or provide their own implementation.
 * Otherwise, they can omit the "nemlogin-signing-pades" dependency.
 */
@SuppressWarnings("unused")
public class ServiceLoaderFactory {

    private ServiceLoaderFactory() {
    }

    /**
     * Returns a new {@link SignersDocumentValidationService} service that supports the validation of Signer's Documents.
     *
     * @param sdFormat the {@link DocumentFormat}
     * @return a new {@link SignersDocumentValidationService} service that supports validation of SD of the given {@code sdFormat}
     * @throws ServiceUnavailableException if no implementation can be found
     */
    public static SignersDocumentValidationService getSignersDocumentValidationService(DocumentFormat sdFormat) throws ServiceUnavailableException {
        return loadService(SignersDocumentValidationService.class, sdFormat);
    }


    /**
     * Returns a new {@link SignatureParameterSigningService} service.
     *
     * @return a new {@link SignatureParameterSigningService} service
     * @throws ServiceUnavailableException if no implementation can be found
     */
    public static SignatureParameterSigningService getSignatureParameterSigningService() throws ServiceUnavailableException {
        return loadService(SignatureParameterSigningService.class, null);
    }


    /**
     * Returns a new {@link SourceAttachmentService} service that supports source file attachments for
     * the transformation given by {@code ctx}.
     *
     * @param transformation the {@link ValidTransformation}
     * @return a new {@link SourceAttachmentService} service that supports source file attachments for
     * the transformation given by {@code ctx}
     * @throws ServiceUnavailableException if no implementation can be found
     */
    public static SourceAttachmentService getSourceAttachmentService(ValidTransformation transformation) throws ServiceUnavailableException {
        return loadService(SourceAttachmentService.class, transformation);
    }


    /**
     * Returns a new {@link FormatTransformationService} service that supports the transformation given by {@code ctx}.
     *
     * @param transformation the {@link ValidTransformation}
     * @return a new {@link FormatTransformationService} service that supports the transformation given by {@code ctx}
     * @throws ServiceUnavailableException if no implementation can be found
     */
    public static FormatTransformationService getFormatTransformationService(ValidTransformation transformation) throws ServiceUnavailableException {
        return loadService(FormatTransformationService.class, transformation);
    }


    /**
     * Returns a new {@link SignatureStamperService} service that supports pre-signing a DTBS of the given {@code signatureFormat}
     *
     * @param signatureFormat the {@link SignatureFormat}
     * @return a new {@link SignatureStamperService} service that supports pre-signing a DTBS of the given {@code signatureFormat}
     * @throws ServiceUnavailableException if no implementation can be found
     */
    public static SignatureStamperService getSignatureStamperService(SignatureFormat signatureFormat) throws ServiceUnavailableException {
        return loadService(SignatureStamperService.class, signatureFormat);
    }


    /**
     * Loads a service that implements the give {@code serviceClass} and supports the given {@code params}.
     * If multiple matching services are found, the one with the highest priority is returned.
     *
     * @param serviceClass the class of the service to load
     * @param params the parameters that the service must support
     * @return the highest prioritized matching service
     * @throws ServiceUnavailableException if no match is found.
     */
    static <P, T extends NemLogInService<P>> T loadService(Class<T> serviceClass, P params) throws  ServiceUnavailableException {
        return StreamSupport.stream(ServiceLoader.load(serviceClass).spliterator(), false)
            .filter(service -> service.supports(params))
            .max(comparingInt(NemLogInService::getPriority))
            .orElseThrow(() -> new ServiceUnavailableException(
                serviceClass,
                String.format("No matching %s service found in classpath."+
                    " Add the proper dependency or implement your own.", serviceClass.getSimpleName())));
    }

}
