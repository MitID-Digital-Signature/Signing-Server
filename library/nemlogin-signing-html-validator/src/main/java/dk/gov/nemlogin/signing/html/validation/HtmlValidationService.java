package dk.gov.nemlogin.signing.html.validation;

import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.service.SignersDocumentValidationService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This Validation service is used to validate HTML dtbs input
 */
public class HtmlValidationService implements SignersDocumentValidationService {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlValidationService.class);

    /**
     * {@inheritDoc}
     **/
    @Override
    public boolean supports(DocumentFormat sdFormat) {
        return DocumentFormat.HTML == sdFormat;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public void validate(TransformationContext ctx) throws NemLogInException {
        LOG.info("Validating HTML");
        String html = ctx.getSignersDocument().dataAsText();

        var validator = new HtmlSignTextValidator();
        if (!validator.validate(html)) {
            throw new ValidationException(ErrorCode.SDK010, ctx, validator.getStatus());
        }
    }
}
