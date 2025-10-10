package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.SignatureFormat;

/**
 * Defines an API for stamping the DTBS (Data To Be Signed) document with a signature template.
 * <p>
 * For PDF this amounts to adding a Signature Dictionary template and for XML and XMLDSig signature template.
 * The signature templates will subsequently be completed in the Signing Client.
 */
public interface SignatureStamperService extends NemLogInService<SignatureFormat> {

    /**
     * Flags if this service supports the given DTBS signature format.
     *
     * @param signatureFormat the DTBS signature format
     * @return if this service supports the given DTBS signature format
     */
    @Override
    default boolean supports(SignatureFormat signatureFormat) {
        return false;
    }


    /**
     * Adds a signature template to the DTBS.
     * <p>
     * Upon a successful stamping the DTBS with a signature, the following must be updated:
     * <ul>
     *     <li>The {@code ctx.dataToBeSigned} field
     *         must contain the PDF Signature Directory or XMLDSig Signature element.</li>
     *     <li>The {@code ctx.signatureParameters.dtbsSignedInfo} field
     *         must contain the Base64 encoded PDF/XML SignedInfo blob.</li>
     * </ul>
     *
     * @param ctx the {@link TransformationContext} containing the DTBS document and other relevant transformation parameters
     * @throws TransformationException if the pre-signing fails
     */
    void preSign(TransformationContext ctx) throws TransformationException;
}
