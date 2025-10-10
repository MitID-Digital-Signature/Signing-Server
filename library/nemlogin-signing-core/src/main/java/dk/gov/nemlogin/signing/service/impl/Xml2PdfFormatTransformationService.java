package dk.gov.nemlogin.signing.service.impl;

import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.SignersDocument.XmlSignersDocument;
import dk.gov.nemlogin.signing.model.ValidTransformation;
import dk.gov.nemlogin.signing.service.FormatTransformationService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK007;

/**
 * Implementation of the {@link FormatTransformationService} interface,
 * which handles XML -> PDF transformation by first generating HTML and then PDF from the HTML.
 */
public class Xml2PdfFormatTransformationService extends Abstract2PdfFormatTransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(Xml2PdfFormatTransformationService.class);

    /** {@inheritDoc} **/
    @Override
    public boolean supports(ValidTransformation transformation) {
        return transformation.getSdFormat() == DocumentFormat.XML &&
            transformation.getSignatureFormat() == SignatureFormat.PAdES;
    }


    /** {@inheritDoc} **/
    @Override
    protected String generateHtml(TransformationContext ctx) throws TransformationException {

        long t0 = System.currentTimeMillis();

        XmlSignersDocument sd = (XmlSignersDocument)ctx.getSignersDocument();
        String html;
        try {
            String xml = sd.dataAsText();
            String xslt = sd.xsltAsText();
            html = xslt(xslt, xml);

            LOG.debug("Initially transformed {} from XML to HTML in {} ms", sd.getName(), System.currentTimeMillis() - t0);

            return html;

        } catch (Exception e) {
            throw new TransformationException(
                SDK007,
                ctx,
                "Error initially transforming " + sd.getName() + " from XML to HTML: " + e.getMessage(), e);
        }

    }


    /**
     * Performs an XSLT of the SD XML document. Should yield HTML.
     *
     * @param xslt the actual XSLT
     * @param xml the actual XML
     * @return the result of the XSLT transformation
     */
    private String xslt(String xslt, String xml) throws TransformerException {
        var xsltSource = new StreamSource(new StringReader(xslt));
        var xmlSource = new StreamSource(new StringReader(xml));
        var writer = new StringWriter();
        TransformerFactory.newDefaultInstance()
            .newTransformer(xsltSource)
            .transform(xmlSource, new StreamResult(writer));
        return writer.toString();
    }
}
