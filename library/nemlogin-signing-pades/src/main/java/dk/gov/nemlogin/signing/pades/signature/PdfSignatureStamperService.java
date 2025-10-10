package dk.gov.nemlogin.signing.pades.signature;

import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned.PadesDataToBeSigned;
import dk.gov.nemlogin.signing.service.SignatureStamperService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SignSDKVersion;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cms.CMSSignedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK005;

/**
 * Updates the DTBS PDF with a template Signature Dictionary where the fixed-length Contents field is filled with "0".
 * <p>
 * Also computes a signature element and updates the {@code ctx.signatureParameters.dtbsSignedInfo} field with
 * the Base-64 encoded version of the PKCS#7 SignedInfo part.
 * <p>
 * Give credit to whom credit is due:
 * The code borrows heavily from PDFBox and DSS
 */
public class PdfSignatureStamperService implements SignatureStamperService {

    public static final int SIGNATURE_SIZE = 16384;
    public static final String SIGNATURE_TYPE = "Sig";
    public static final String SIGNATURE_DEFAULT_FILTER = "Adobe.PPKLite";
    public static final String SIGNATURE_DEFAULT_SUBFILTER = "ETSI.CAdES.detached";
    public static final String SIGNATURE_NAME = "NemLog-In Signing SDK";
    public static final int NO_CHANGE_PERMITTED = 1;

    private static final Logger LOG = LoggerFactory.getLogger(PdfSignatureStamperService.class);

    static {
        SigningUtils.addBouncyCastleProvider();
    }


    /** {@inheritDoc} **/
    @Override
    public boolean supports(SignatureFormat signatureFormat) {
        return signatureFormat == SignatureFormat.PAdES;
    }


    /** {@inheritDoc} **/
    @Override
    public void preSign(TransformationContext ctx) throws TransformationException {

        long t0 = System.currentTimeMillis();
        LOG.debug("Start pre-signing PDF");

        try (var outputStream = new ByteArrayOutputStream();
             var is = ctx.getDataToBeSigned().dataInputStream();
             var pdDocument = PDDocument.load(is)) {

            // First check that the PDF does not already contain a Signature Dictionary
            checkNoExistingSignatures(ctx, pdDocument);

            // then change producer - so it is possible to trace which SignSDK has modified this PDF
            final PDDocumentInformation documentInformation = pdDocument.getDocumentInformation();
            documentInformation.setProducer(SignSDKVersion.getProducer());
            documentInformation.setModificationDate(Calendar.getInstance());

            CMSSignedData signature = signDocumentAndReturnDigest(ctx, outputStream, pdDocument);

            // Update the DTBS PDF document
            byte[] signedPDF = outputStream.toByteArray();
            ctx.setDataToBeSigned(new PadesDataToBeSigned(signedPDF, ctx.getDataToBeSigned().getName()));

            // Update the signature parameters with the CMS SignerInfo element
            var signerInfo = Base64.getEncoder().encodeToString(signature.getEncoded());
            ctx.updateDtbsSignedInfo(signerInfo);

            LOG.info("Pre-signed PDF in {} ms", System.currentTimeMillis() - t0);
            if (LOG.isDebugEnabled()) {
                LOG.info("Base64 CMS signature: {}", signerInfo);
            }
        } catch (IOException e) {
            throw new TransformationException(SDK005, ctx, "Error pre-signing PDF", e);
        }
    }


    /**
     * Checks and throws an {@link TransformationException} if the PDF already contains a Signature Dictionary
     * @param ctx the current {@link TransformationContext}
     * @param pdDocument in-memory representation of the DTBS PDF document
     */
    private void checkNoExistingSignatures(TransformationContext ctx, PDDocument pdDocument) throws TransformationException {
        try {
            List<PDSignature> allsigs = pdDocument.getSignatureDictionaries();
            if (!allsigs.isEmpty()) {
                throw new TransformationException(SDK005, ctx, "DTBS PDF already contains "
                    + allsigs.size() + " Signature Dictionaries");
            }
        } catch (IOException e) {
            throw new TransformationException(SDK005, ctx, "Error extracting DTBS PDF Signature Dictionaries", e);
        }
    }


    /**
     * Stamps the PDF with a signature template
     * @param ctx the current {@link TransformationContext}
     * @param outputStream the output stream of the signed PDF
     * @param pdDocument in-memory representation of the DTBS PDF document
     * @return the CMS signature
     */
    private CMSSignedData signDocumentAndReturnDigest(TransformationContext ctx,
                                                      OutputStream outputStream,
                                                      PDDocument pdDocument) throws TransformationException {

        // Use signature interface that computes CMS but inserts "0" in PDF
        var signatureInterface = new TemplateSignatureInterface(ctx);

        final var pdSignature = createSignatureDictionary();
        try (var options = new SignatureOptions()) {
            options.setPreferredSignatureSize(SIGNATURE_SIZE);
            pdDocument.addSignature(pdSignature, signatureInterface, options);
            saveDocumentIncrementally(ctx, outputStream, pdDocument);
            return signatureInterface.getSignedData();
        } catch (IOException e) {
            throw new TransformationException(SDK005, ctx, "Error pre-signing PDF", e);
        }
    }


    /**
     * Creates a PDF signature dictionary
     * @return the signature
     */
    private PDSignature createSignatureDictionary() {

        var signature= new PDSignature();

        var currentType = COSName.getPDFName(SIGNATURE_TYPE);
        signature.setType(currentType);
        signature.setFilter(COSName.getPDFName(SIGNATURE_DEFAULT_FILTER));
        // sub-filter for basic and PAdES Part 2 signatures
        signature.setSubFilter(COSName.getPDFName(SIGNATURE_DEFAULT_SUBFILTER));

        signature.setName(SIGNATURE_NAME);

        // the signing date, needed for valid signature
        signature.setSignDate(Calendar.getInstance());

        return signature;
    }


    /**
     * Saves the document incrementally
     * @param ctx the current {@link TransformationContext}
     * @param outputStream the output stream of the signed PDF
     * @param pdDocument in-memory representation of the DTBS PDF document
     */
    public void saveDocumentIncrementally(TransformationContext ctx, OutputStream outputStream, PDDocument pdDocument)
        throws TransformationException {
        try {
            // the document needs to have an ID, if not a ID based on the current system
            // time is used, and then the
            // digest of the signed data is different
            if (pdDocument.getDocumentId() == null) {
                pdDocument.setDocumentId(System.currentTimeMillis());
            }

            final PDDocumentInformation documentInformation = pdDocument.getDocumentInformation();
            // saveIncremental will only update if setNeedToBeUpdated is set
            documentInformation.getCOSObject().setNeedToBeUpdated(true);

            pdDocument.saveIncremental(outputStream);
        } catch (IOException e) {
            throw new TransformationException(SDK005, ctx, "Error saving pre-signed DTBS PDF incrementally", e);
        }
    }


    /**
     * Credit: Code copied directly from DSS project -> PdfBoxSignatureService...
     *
     * Set the access permissions granted for this document in the DocMDP transform
     * parameters dictionary. Details are described in the table "Entries in the
     * DocMDP transform parameters dictionary" in the PDF specification.
     * <p/><b>
     * This part has been disabled because of "JIRA NLSIGN-472 Check PDF signature format with Adobe Reader."<br/>
     * by comparing PAdES signed with DSS and Signing Client - the biggest Level B difference was the DocMDP and
     *
     * "TS 102 778-4 - V1.1.1 - Electronic Signatures and Infrastructures (ESI); PDF Advanced Electronic Signature Profiles; Part 4: PAdES Long Term"
     * <b/><br/>
     * <code>
     *     NOTE: ISO 32000-1 [1], 12.8.2.2, discusses the DocMDP (Modification, Detection and Prevention) feature
     * whereby a set of permissions can be associated with a PDF in conjunction with a certification signature.
     * The permissions of DocMDP are present in the P key of the DocMDP transform parameters dictionary,
     * as an integer in the range 1 through 3. Values of 2 and 3 allow for additional signatures to be included
     * after the certification but a value of 1 does not allow any change so allow Document Time-stamps. This
     * provision will need to be changed from that in ISO 32000-1 [1], to allow for the inclusion of LTV,
     * including DSS and Document Time-stamps
     * </code>
     * <p/>
     * The DocMDP part does not validate correctly in Adobe when we have the signing in several steps.
     *
     * @param doc The document.
     * @param signature The signature object.
     * @param accessPermissions The permission value (1, 2 or 3).
     */
    public void setMDPPermission(PDDocument doc, PDSignature signature, int accessPermissions) {
        COSDictionary sigDict = signature.getCOSObject();

        // DocMDP specific stuff
        var transformParameters = new COSDictionary();
        transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
        transformParameters.setInt(COSName.P, accessPermissions);
        transformParameters.setName(COSName.V, "1.2");
        transformParameters.setNeedToBeUpdated(true);

        var referenceDict = new COSDictionary();
        referenceDict.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
        referenceDict.setItem("TransformMethod", COSName.DOCMDP);
        referenceDict.setItem("TransformParams", transformParameters);
        referenceDict.setNeedToBeUpdated(true);

        var referenceArray = new COSArray();
        referenceArray.add(referenceDict);
        sigDict.setItem("Reference", referenceArray);
        referenceArray.setNeedToBeUpdated(true);

        // Document Catalog
        COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
        var permsDict = new COSDictionary();
        catalogDict.setItem(COSName.PERMS, permsDict);
        permsDict.setItem(COSName.DOCMDP, signature);
        catalogDict.setNeedToBeUpdated(true);
        permsDict.setNeedToBeUpdated(true);
    }
}
