package dk.gov.nemlogin.signing.xades.service;

import certifikat.gov.dk.nemlogin.v0_0.HTMLDocumentType;
import certifikat.gov.dk.nemlogin.v0_0.ObjectFactory;
import certifikat.gov.dk.nemlogin.v0_0.PDFDocumentType;
import certifikat.gov.dk.nemlogin.v0_0.PlainTextType;
import certifikat.gov.dk.nemlogin.v0_0.PropertiesType;
import certifikat.gov.dk.nemlogin.v0_0.PropertyType;
import certifikat.gov.dk.nemlogin.v0_0.SignTextType;
import certifikat.gov.dk.nemlogin.v0_0.SignedDocumentType;
import certifikat.gov.dk.nemlogin.v0_0.XMLDocumentType;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.model.SignProperties;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.model.ValidTransformation;
import dk.gov.nemlogin.signing.service.FormatTransformationService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.xades.util.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK009;
import static dk.gov.nemlogin.signing.util.SigningUtils.fileName;
import static java.lang.String.format;

/**
 * Implementation of the {@link FormatTransformationService} interface,
 * which handles all preliminary formats (XML, HTML, TXT, PDF) -> XML transformation.
 */
public class AllDocumentFormatTransformationService implements FormatTransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(AllDocumentFormatTransformationService.class);

    private static final ObjectFactory objectFactory = new ObjectFactory();

    /**
     * {@inheritDoc}
     **/
    @Override
    public boolean supports(ValidTransformation transformation) {
        return transformation.getSignatureFormat() == SignatureFormat.XAdES;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public void transform(TransformationContext ctx) throws NemLogInException {

        long t0 = System.currentTimeMillis();
        var sd = ctx.getSignersDocument();
        final String name = sd.getName();
        final DocumentFormat format = sd.getFormat();
        LOG.debug("Start transforming {} from {} to XML", format, name);

        // Create the correct SignText document type
        SignTextType signText = objectFactory.createSignTextType().withId("id-" + UUID.randomUUID().toString());
        try {
            switch (ctx.getSignersDocument().getFormat()) {
                case TEXT:
                    signText.withPlainText(createPlainText(ctx));
                    break;
                case HTML:
                    signText.withHTMLDocument(createHtmlDocument(ctx));
                    break;
                case PDF:
                    signText.withPDFDocument(createPdfDocument(ctx));
                    break;
                case XML:
                    signText.withXMLDocument(createXmlDocument(ctx));
                    break;
                default:
                    throw new TransformationException(SDK009, ctx, format("Error initially transforming %s to XML. Unknown preliminary input type '%s'", name, ctx.getSignersDocument().getFormat()));
            }
        } catch (Exception e) {
            throw new TransformationException(SDK009, ctx, "Error generating XAdES from " +
                ctx.getSignersDocument().getFormat(), e);
        }

        // If any properties is supplied, add them to SignText
        if (ctx.getSignersDocument().getProperties() != null && !ctx.getSignersDocument().getProperties().isEmpty()) {
            signText.withProperties(createProperties(ctx));
        }

        // Create DataToBeSigned
        try {
            SignedDocumentType signedDocument = objectFactory.createSignedDocumentType().withSignText(signText);
            DataToBeSigned dtbs = new DataToBeSigned.XadesDataToBeSigned(XmlUtils.marshal(signedDocument), fileName(name, "xml"));
            ctx.setDataToBeSigned(dtbs);
        } catch (Exception e) {
            throw new TransformationException(SDK009, ctx, "Error when marshalling SignedDocument", e);
        }
        final long timeMs = System.currentTimeMillis() - t0;
        LOG.info("Transformed {} from {} to XML in {} ms", name, format, timeMs);
    }

    private PlainTextType createPlainText(final TransformationContext ctx) throws NemLogInException {
        return objectFactory.createPlainTextType()
            .withDocument(ctx.getSignersDocument().getData())
            .withRendering(objectFactory.createRenderingType()
                .withUseMonoSpaceFont(((SignersDocument.PlainTextSignersDocument) ctx.getSignersDocument()).isUseMonoSpaceFont())
            );
    }

    private HTMLDocumentType createHtmlDocument(final TransformationContext ctx) throws NemLogInException {
        return objectFactory.createHTMLDocumentType().withDocument(ctx.getSignersDocument().getData());
    }

    private PDFDocumentType createPdfDocument(final TransformationContext ctx) throws NemLogInException {
        return objectFactory.createPDFDocumentType().withDocument(ctx.getSignersDocument().getData());
    }

    private XMLDocumentType createXmlDocument(final TransformationContext ctx) throws NemLogInException {
        return objectFactory.createXMLDocumentType()
            .withDocument(ctx.getSignersDocument().getData())
            .withTransformation(((SignersDocument.XmlSignersDocument) ctx.getSignersDocument()).getXsltData());
    }

    private PropertiesType createProperties(final TransformationContext ctx) {
        Collection<PropertyType> properties = new ArrayList<>();
        ctx.getSignersDocument().getProperties().keySet().forEach(key -> {
                final SignProperties.SignPropertyValue<?> value = ctx.getSignersDocument().getProperties().get(key);
                if (value instanceof SignProperties.StringValue) {
                    properties.add(objectFactory.createPropertyType()
                        .withKey(key)
                        .withStringValue(((SignProperties.StringValue) value).serialize()));
                } else if (value instanceof SignProperties.BinaryValue) {
                    properties.add(objectFactory.createPropertyType()
                        .withKey(key)
                        .withBinaryValue(((SignProperties.BinaryValue) value).serialize()));
                }
            }
        );
        return objectFactory.createPropertiesType().withProperty(properties);
    }
}
