package dk.gov.nemlogin.signing.service.impl;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.model.DataToBeSigned.PadesDataToBeSigned;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.model.SignersDocument.HtmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocumentFile;
import dk.gov.nemlogin.signing.model.ValidTransformation;
import dk.gov.nemlogin.signing.service.FormatTransformationService;
import dk.gov.nemlogin.signing.service.ServiceLoaderFactory;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK007;
import static dk.gov.nemlogin.signing.util.SigningUtils.fileName;

/**
 * Abstract implementation of the {@link FormatTransformationService} interface,
 * which may be subclassed by (XML | TEXT) -> PDF transformations by first generating HTML from the SD
 * and then PDF from the HTML.
 */
public abstract class Abstract2PdfFormatTransformationService implements FormatTransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(Abstract2PdfFormatTransformationService.class);

    /** {@inheritDoc} **/
    @Override
    public void transform(TransformationContext ctx) throws TransformationException {

        long t0 = System.currentTimeMillis();
        var sd = ctx.getSignersDocument();
        LOG.debug("Start transforming {} from {} to PDF", sd.getFormat(), sd.getName());

        // Step 1: Generate HTML by transforming the XML using the included XSLT
        String html = generateHtml(ctx);

        // Step 2: Generate PDF from the HTML
        try {
            byte[] pdf = generatePDF(html, sd.getName(), ctx.getTransformationProperties());

            DataToBeSigned dtbs = new PadesDataToBeSigned(pdf, fileName(sd.getName(), "pdf"));
            ctx.setDataToBeSigned(dtbs);
            LOG.info("Transformed {} from {} to PDF in {} ms", sd.getName(), sd.getFormat(), System.currentTimeMillis() - t0);

        } catch (Exception e) {
            throw new TransformationException(
                SDK007,
                ctx,
                "Error transforming " + sd.getName() + " from " + sd.getFormat() + " to PDF: " + e.getMessage(), e);
        }
    }


    /**
     * Generates HTML from the SD (Signer's Document)
     * @param ctx the transformation context
     * @return the HTML
     */
    protected abstract String generateHtml(TransformationContext ctx) throws TransformationException;


    /**
     * Generate PDF from the HTML
     * @param html the html to generate PDF from
     * @param name the file name
     * @param transformationProperties transformation properties to customize HTML -> PDF transformation
     * @return the PDF
     */
    public static byte[] generatePDF(String html, String name, Properties transformationProperties)
        throws NemLogInException {

        var transformation = ValidTransformation.transformationFor(
            DocumentFormat.HTML,
            SignatureFormat.PAdES);
        var pdfService = ServiceLoaderFactory.getFormatTransformationService(transformation);

        var sdFile = SignersDocumentFile.builder()
            .setData(html.getBytes(StandardCharsets.UTF_8))
            .setName(name)
            .build();

        var signatureParameters = SignatureParameters.builder()
            .setValidTransformation(transformation)
            .build();

        var ctx = new TransformationContext(
            new HtmlSignersDocument(sdFile),
            null,
            signatureParameters,
            transformationProperties);

        pdfService.transform(ctx);
        return ctx.getDataToBeSigned().getData();
    }
}
