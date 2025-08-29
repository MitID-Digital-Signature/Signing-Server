package dk.gov.nemlogin.signing.service.impl;

import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.service.SourceAttachmentService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link SourceAttachmentService} does nothing.
 * <p>
 * In order to cater with XML -> PDF transformations, where the original XML and XSLT must be attached,
 * the "nemlogin-signing-pades" project contains an implementation for this.
 */
public class DefaultSourceAttachmentService implements SourceAttachmentService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSourceAttachmentService.class);

    /** {@inheritDoc} **/
    @Override
    public void attachSourceFiles(TransformationContext ctx) throws TransformationException {
        LOG.debug("Source files not attached for transformation {}", ctx.getTransformation());
    }
}
