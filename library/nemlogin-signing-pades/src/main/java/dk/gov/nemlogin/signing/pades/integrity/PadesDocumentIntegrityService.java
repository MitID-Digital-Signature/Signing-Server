package dk.gov.nemlogin.signing.pades.integrity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.SignedDocumentIntegrityException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.IntegrityValues;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.service.DocumentIntegrityService;
import dk.gov.nemlogin.signing.service.TransformationContext;

public class PadesDocumentIntegrityService implements DocumentIntegrityService {

    @Override
    public IntegrityValues createIntegrityValues(TransformationContext ctx) throws NemLogInException{
        if (ctx.getDataToBeSigned().getFormat() != SignatureFormat.PAdES) {
            throw new NemLogInException(ErrorCode.SDK012, "Unable to create integrity values, unknown format: " + ctx.getDataToBeSigned().getFormat());

        }
        try {
            byte[] dtbs = ctx.getDataToBeSigned().getData();
            var is = ctx.getDataToBeSigned().dataInputStream();
            var pdDocument = PDDocument.load(is);

            List<PDSignature> sigs = pdDocument.getSignatureDictionaries();
            if (sigs.size() != 1) {
                throw new NemLogInException(ErrorCode.SDK012, "Expected exactly 1 signature, but found " + sigs.size());
            }
            int[] byteRange = sigs.get(0).getByteRange();

            var md = MessageDigest.getInstance(SignatureParameters.DTBS_DIGEST_ALGORITHM);
            md.update(dtbs, byteRange[0], byteRange[1]);
            md.update(dtbs, byteRange[2], byteRange[3]);
            byte[] digest = md.digest();
            var result = new IntegrityValues(
                    ctx.getDataToBeSigned().getFormat(),
                    SignatureParameters.DTBS_DIGEST_ALGORITHM,
                    digest,
                    byteRange[1],
                    byteRange[2],
                    byteRange[3]
                    );
            return result;
        }
        catch(Exception ex) {
            throw new NemLogInException(ErrorCode.SDK012, "Error capturing integrity values", ex);
        }
    }

    @Override
    public void verifySignedDocument(byte[] signedDocument, IntegrityValues integrityValues) throws SignedDocumentIntegrityException {
        try {
            int byteRange1End = integrityValues.getByteRange1End();
            int byteRange2Start = integrityValues.getByteRange2Start();
            int byteRange2Length = integrityValues.getByteRange2Length();

            if (signedDocument.length < byteRange2Start + byteRange2Length) {
                throw new SignedDocumentIntegrityException( integrityValues, "The documents legnth: " + signedDocument.length + " is less than the expected " + (byteRange2Start + byteRange2Length));
            }

            var md = MessageDigest.getInstance(integrityValues.getDigestAlgorithm());
            md.update(signedDocument, 0, byteRange1End);
            md.update(signedDocument, byteRange2Start, byteRange2Length);
            byte[] actualDigest = md.digest();

            if (!MessageDigest.isEqual(actualDigest, integrityValues.getOriginalDigest())) {
                throw new SignedDocumentIntegrityException(integrityValues, "Document integrity verification failed: Digest mismatch.");
            }
        }
        catch (NoSuchAlgorithmException e) {
            throw new SignedDocumentIntegrityException(integrityValues, "Invalid digest algorithm: " + integrityValues.getDigestAlgorithm());
        }
    }

    @Override
    public boolean supports(SignatureFormat params) {
        return SignatureFormat.PAdES.equals(params);
    }
}
