package dk.gov.nemlogin.signing.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.util.XRLog;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.model.DataToBeSigned.PadesDataToBeSigned;
import dk.gov.nemlogin.signing.model.ValidTransformation;
import dk.gov.nemlogin.signing.service.FormatTransformationService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK007;
import static dk.gov.nemlogin.signing.util.SignSDKVersion.getProducer;

/**
 * Implementation of the {@link FormatTransformationService} interface,
 * which handles HTML -> PDF transformation by generating a PDF from the HTML.
 * <p>
 * As per the SignPDF specification, the generated PDF will conform to PDF/A-2 (basic level).
 */
public class Html2PdfFormatTransformationService implements FormatTransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(Html2PdfFormatTransformationService.class);

    /** Generated PDF conforms to PDF/A-2b - see https://en.wikipedia.org/wiki/PDF/A **/
    private static final PdfAConformance PDF_A_2_BASIC = PdfAConformance.PDFA_2_B;

    /** Generated PDF version is 1.7 **/
    private static final float PDF_VERSION_1_7 = 1.7f;


    // Configure logging
    static {
        XRLog.setLoggingEnabled(true);
        XRLog.setLoggerImpl(new Slf4jLogger());
    }


    /** {@inheritDoc} **/
    @Override
    public boolean supports(ValidTransformation transformation) {
        return transformation.getSdFormat() == DocumentFormat.HTML &&
            transformation.getSignatureFormat() == SignatureFormat.PAdES;
    }


    /** {@inheritDoc} **/
    @Override
    public void transform(TransformationContext ctx) throws TransformationException {

        long t0 = System.currentTimeMillis();
        var sd = ctx.getSignersDocument();
        var transformationPropertyHandler = new TransformationPropertiesHandler(ctx);

        LOG.debug("Start transforming {} from HTML to PDF", sd.getName());

        try {
            var html = sd.dataAsText();

            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(html);

            // If defined, add a CSS style element to control page size, margin, etc.
            var pageStyle = transformationPropertyHandler.getPageStyle();

            // The "Open HTML to PDF" library has a mildly neurotic font-handling behaviour.
            // To avoid problems, we specify fonts for body and <pre> (used e.g. for monospace plain-text SDs),
            // both of which can be overridden using transformation properties.
            // Furthermore, add css-styles to ensure that long words do not get cropped, but wrapped to the next line.
            var bodyStyle = String.format("body { font: %s; word-wrap: break-word; }",
                transformationPropertyHandler.getBodyFont());
            var monospaceStyle = String.format("pre { font: %s; word-wrap: break-word; white-space: pre-wrap; }",
                transformationPropertyHandler.getMonospaceFont());

            var style = String.format("<style>%n%s%n%s%n%s</style>", pageStyle, bodyStyle, monospaceStyle);
            htmlDoc.head().prepend(style);

            // Parse HTML as xhtml
            var xhtmlDoc = new W3CDom().fromJsoup(htmlDoc);

            // Convert to PDF
            var result = new ByteArrayOutputStream();
            var pdfRendererBuilder = new PdfRendererBuilder()
                .useFastMode()
                .withW3cDocument(xhtmlDoc, "/")
                .usePdfAConformance(PDF_A_2_BASIC)
                .usePdfVersion(PDF_VERSION_1_7)
                .withProducer(getProducer())
                .toStream(result);

            // Update the PDF renderer according to transformation properties.
            // Updates fonts and color profile
            transformationPropertyHandler.applyProperties(pdfRendererBuilder);

            // Create the PDF
            pdfRendererBuilder.run();

            DataToBeSigned dtbs = new PadesDataToBeSigned(
                result.toByteArray(),
                SigningUtils.fileName(sd.getName(), ".pdf"));
            ctx.setDataToBeSigned(dtbs);

            LOG.info("Transformed {} from HTML to PDF in {} ms", sd.getName(), System.currentTimeMillis() - t0);

        } catch (Exception e) {
            throw new TransformationException(
                SDK007,
                ctx,
                "Error transforming " + sd.getName() + " from HTML to PDF: " + e.getMessage(), e);
        }
    }

}
