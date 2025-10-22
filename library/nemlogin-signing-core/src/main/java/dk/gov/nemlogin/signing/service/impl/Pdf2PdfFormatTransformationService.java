package dk.gov.nemlogin.signing.service.impl;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.model.DataToBeSigned.PadesDataToBeSigned;
import dk.gov.nemlogin.signing.model.ValidTransformation;
import dk.gov.nemlogin.signing.service.FormatTransformationService;
import dk.gov.nemlogin.signing.service.TransformationContext;

/**
 * Default implementation of the {@link FormatTransformationService} interface,
 * which handles PDF -> PDF transformation by setting DTBS to SD when both are PDF.
 */
public class Pdf2PdfFormatTransformationService implements FormatTransformationService {

    /** {@inheritDoc} **/
    @Override
    public boolean supports(ValidTransformation transformation) {
        return transformation.getSdFormat() == DocumentFormat.PDF &&
            transformation.getSignatureFormat() == SignatureFormat.PAdES;
    }


    /** {@inheritDoc} **/
    @Override
    public void transform(TransformationContext ctx) throws NemLogInException {
        var sd = ctx.getSignersDocument();
        DataToBeSigned dtbs = new PadesDataToBeSigned(sd.getData(), sd.getName());
        ctx.setDataToBeSigned(dtbs);
    }
}
