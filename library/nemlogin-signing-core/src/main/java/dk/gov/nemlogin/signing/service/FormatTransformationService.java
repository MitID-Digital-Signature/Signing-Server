package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.model.ValidTransformation;

/**
 * Defines an API for transforming an SD (Signer's Document) to a DTBS (Data To Be Signed) document
 * according to the transformation specified by the {@link ValidTransformation}.
 * <p>
 * Implementations may e.g. handle XML or PDF transformations.
 */
public interface FormatTransformationService extends NemLogInService<ValidTransformation> {

    /**
     * Flags if this transformation service supports the given transformation.
     *
     * @param transformation the transformation
     * @return if this transformation service supports the given transformation
     */
    @Override
    default boolean supports(ValidTransformation transformation) {
        return false;
    }


    /**
     * Performs the transformation from SD to DTBS.
     * <p>
     * Upon a successful transformation the {@code ctx} is updated with the resulting DTBS.
     *
     * @param ctx the {@link TransformationContext} containing SD and other relevant transformation parameters
     * @throws TransformationException if the transformation fails
     */
    void transform(TransformationContext ctx) throws NemLogInException;
}
