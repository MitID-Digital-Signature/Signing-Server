package dk.gov.nemlogin.signing.pades.attach;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned.PadesDataToBeSigned;
import dk.gov.nemlogin.signing.model.SignersDocument.XmlSignersDocument;
import dk.gov.nemlogin.signing.model.ValidTransformation;
import dk.gov.nemlogin.signing.service.SourceAttachmentService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SignSDKVersion;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK008;

/**
 * Implementation of the {@link SourceAttachmentService} interface,
 * which attaches the original XML and XSLT to the PDF.
 * <p>
 * Note to self:
 * https://stackoverflow.com/questions/30549830/attachment-damages-signature-part-2
 */
public class PdfSourceAttachmentService implements SourceAttachmentService {

    private static final Logger LOG = LoggerFactory.getLogger(PdfSourceAttachmentService.class);
    public static final String ATTACHMENT_XML_NAME = "NemLog-In XML Attachment";
    public static final String ATTACHMENT_XSL_NAME = "NemLog-In XSL Attachment";


    /** {@inheritDoc} **/
    @Override
    public int getPriority() {
        // NB: Higher priority than the NoopSourceAttachmentService
        return 100;
    }


    /** {@inheritDoc} **/
    @Override
    public boolean supports(ValidTransformation transformation) {
        return transformation.getSdFormat() == DocumentFormat.XML &&
            transformation.getSignatureFormat() == SignatureFormat.PAdES;
    }


    /** {@inheritDoc} **/
    @Override
    public void attachSourceFiles(TransformationContext ctx) throws NemLogInException {

        long t0 = System.currentTimeMillis();
        LOG.debug("Adding XML and XSLT attachments to PDF");

        XmlSignersDocument sd = (XmlSignersDocument)ctx.getSignersDocument();

        try (var outputStream = new ByteArrayOutputStream();
             var is = ctx.getDataToBeSigned().dataInputStream();
             var pdDocument = PDDocument.load(is)) {

            // XML file
            PDComplexFileSpecification xmlFile = createAttachmentFile(
                pdDocument,
                sd.getName(),
                sd.getData(),
                sd.getFile().getCreationTime(),
                "text/xml");

            // XSLT file
            PDComplexFileSpecification xsltFile = createAttachmentFile(
                pdDocument,
                SigningUtils.fileName(sd.getXsltFile().getName(), "xsl"),
                sd.getXsltData(),
                sd.getXsltFile().getCreationTime(),
                "text/xsl");


            // Add the attachments to a map, and add map to a files name node
            Map<String, PDComplexFileSpecification> efMap = new HashMap<>();
            efMap.put(ATTACHMENT_XML_NAME, xmlFile);
            efMap.put(ATTACHMENT_XSL_NAME, xsltFile);
            var efTree = new PDEmbeddedFilesNameTreeNode();
            efTree.setNames( efMap );
            efTree.getCOSObject().removeItem(COSName.LIMITS);

            // attachments are stored as part of the "names" dictionary in the document catalog
            var namesDictionary = new PDDocumentNameDictionary(pdDocument.getDocumentCatalog());
            namesDictionary.setEmbeddedFiles(efTree);
            pdDocument.getDocumentCatalog().setNames(namesDictionary);
            efTree.getCOSObject().removeItem(COSName.LIMITS);

            // then change producer - so it is possible to trace which SignSDK has modified this PDF
            PDDocumentInformation documentInformation = pdDocument.getDocumentInformation();
            // handle documents without any document information (by creating one)
            if (documentInformation == null) {
                documentInformation = new PDDocumentInformation();
                documentInformation.setCreationDate(Calendar.getInstance());
                pdDocument.setDocumentInformation(documentInformation);
            }
            documentInformation.setProducer(SignSDKVersion.getProducer());
            documentInformation.setModificationDate(Calendar.getInstance());

            // Save the PDF with attachments
            pdDocument.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
            pdDocument.saveIncremental(outputStream);

            // Update the DTBS
            ctx.setDataToBeSigned(new PadesDataToBeSigned(
                outputStream.toByteArray(),
                ctx.getDataToBeSigned().getName()));

            LOG.info("Added XML and XSLT files as PDF attachments in {} ms", System.currentTimeMillis() - t0);
        } catch (IOException e) {
            throw new TransformationException(SDK008, ctx, "Error adding XML and XSLT files as attachments to PDF", e);
        }
    }

    /**
     * Creates a {@link PDComplexFileSpecification} file attachment
     * @param pdDocument the PDF document
     * @param name the file name
     * @param data the data
     * @param type the type
     * @return the attachment
     */
    private PDComplexFileSpecification createAttachmentFile(PDDocument pdDocument, String name, byte[] data, Long creationDate, String type) throws IOException {
        var date = Calendar.getInstance();
        if (creationDate != null) {
            date.setTimeInMillis(creationDate);
        }

        var fileSpec = new PDComplexFileSpecification();
        fileSpec.setFile(name);
        fileSpec.setFileUnicode(name);
        var embeddedFile = new PDEmbeddedFile(pdDocument, new ByteArrayInputStream(data));
        embeddedFile.setSubtype(type);
        embeddedFile.setSize(data.length);
        embeddedFile.setCreationDate(date);
        fileSpec.setEmbeddedFile(embeddedFile);
        return fileSpec;
    }

}
