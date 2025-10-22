package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.model.ValidTransformation;

/**
 * After transforming an SD (Signer's Document) to a DTBS (Data To Be Signed) document
 * the specification states that the original source files may need to be attached to the DTBS for certain types
 * of transformation.
 * <p>
 * Specifically, for an XML -> PDF transformation, the original XML and XSLT must be attached to the PDF.
 */
public interface SourceAttachmentService extends NemLogInService<ValidTransformation> {

    /**
     * Attaches the source documents to the DTBS.
     *
     * @param ctx the {@link TransformationContext} containing DTBS and other relevant transformation parameters
     * @throws TransformationException if the transformation fails
     */
    void attachSourceFiles(TransformationContext ctx) throws NemLogInException;

}
