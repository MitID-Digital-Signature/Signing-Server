package dk.gov.nemlogin.signing.service.impl;

import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.service.SignersDocumentValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates plain text Signer's Documents.
 */
public class PlainTextValidationService implements SignersDocumentValidationService {
    private static final Logger LOG = LoggerFactory.getLogger(PlainTextValidationService.class);

    /**
     * {@inheritDoc}
     **/
    @Override
    public boolean supports(DocumentFormat dsFormat) {
        return DocumentFormat.TEXT == dsFormat;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public void validate(TransformationContext ctx) throws ValidationException {
        LOG.debug("Validating PLAIN_TEXT - Everything is good.");
    }
}
