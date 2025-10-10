package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;

/**
 * Defines an API for validating the SD (Signer's Document).
 * <p>
 * For PDF this amounts to checking if the COS objects are whitelisted.
 */
public interface SignersDocumentValidationService extends NemLogInService<DocumentFormat> {

    /**
     * Flags if this service supports the given SD format.
     *
     * @param sdFormat the SD format format
     * @return if this service supports the given Signers document format
     */
    @Override
    default boolean supports(DocumentFormat sdFormat) {
        return false;
    }


    /**
     * Validates the input document.
     *
     * @param ctx the {@link TransformationContext} containing the Signers document and other relevant transformation parameters
     * @throws {@link ValidationException} if the validation of the input document fails
     */
    void validate(TransformationContext ctx) throws NemLogInException;
}
