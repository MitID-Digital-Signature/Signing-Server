package dk.gov.nemlogin.signing.service.impl;

import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.SignersDocument.PlainTextSignersDocument;
import dk.gov.nemlogin.signing.model.ValidTransformation;
import dk.gov.nemlogin.signing.service.FormatTransformationService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK007;

/**
 * Implementation of the {@link FormatTransformationService} interface,
 * which handles TXT -> PDF transformation by first generating HTML and then PDF from the HTML
 */
public class Txt2PdfFormatTransformationService extends Abstract2PdfFormatTransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(Txt2PdfFormatTransformationService.class);

    /** {@inheritDoc} **/
    @Override
    public boolean supports(ValidTransformation transformation) {
        return transformation.getSdFormat() == DocumentFormat.TEXT &&
            transformation.getSignatureFormat() == SignatureFormat.PAdES;
    }


    /** {@inheritDoc} **/
    @Override
    protected String generateHtml(TransformationContext ctx) throws TransformationException {

        long t0 = System.currentTimeMillis();

        PlainTextSignersDocument sd = (PlainTextSignersDocument)ctx.getSignersDocument();
        String html;
        try {
            // Create a simple XML containing the plain text lines.
            // Example: <data><line>line 1</line><line>line 2</line></data>
            final var documentBuilderFactory = DocumentBuilderFactory.newInstance();
            // XML parsers should not be vulnerable to XXE attacks
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            var docBuilder = documentBuilderFactory.newDocumentBuilder();
            var doc = docBuilder.newDocument();
            var root = doc.createElement("data");
            doc.appendChild(root);
            sd.dataAsText().lines()
                .forEach(line -> {
                    var e = doc.createElement("line");
                    e.appendChild(doc.createTextNode(line));
                    root.appendChild(e);
                });

            // Transform the xml to html
            var xsltSource = new StreamSource(getClass().getResourceAsStream("/xslt/txt-to-html.xsl"));
            var xmlSource = new DOMSource(doc);
            var writer = new StringWriter();
            var transformer = TransformerFactory.newDefaultInstance()
                .newTransformer(xsltSource);
            transformer.setParameter("useMonoSpaceFont", String.valueOf(sd.isUseMonoSpaceFont()));
            transformer.transform(xmlSource, new StreamResult(writer));
            html = writer.toString();

            LOG.debug("Initially transformed {} from TXT to HTML in {} ms", sd.getName(), System.currentTimeMillis() - t0);

            return html;

        } catch (Exception e) {
            throw new TransformationException(
                SDK007,
                ctx,
                "Error initially transforming " + sd.getName() + " from TXT to HTML: " + e.getMessage(), e);
        }
    }
}
