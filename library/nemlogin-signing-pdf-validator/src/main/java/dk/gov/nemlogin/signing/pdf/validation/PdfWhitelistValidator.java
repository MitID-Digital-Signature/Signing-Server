package dk.gov.nemlogin.signing.pdf.validation;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates PDF document against a whitelist of COS names
 */
public class PdfWhitelistValidator {
    private long objNum = -1;
    private int generation = -1;

    /**
     * validates a COS object against a whitelist
     *
     * @param cosBase COS object to be validated
     * @return list of {@link PdfValidationResult}
     * sonar - brain-overload
     */
    @SuppressWarnings("java:S3776")
    public List<PdfValidationResult> whitelistValidation(COSBase cosBase) {
        List<PdfValidationResult> res = new ArrayList<>();
        if (cosBase instanceof COSName) {
            if (!WhiteList.isNameWhitelisted((COSName) cosBase)) {
                res.add(new PdfValidationResult(cosBase, (COSName) cosBase, objNum, generation));
            }
        } else if (cosBase instanceof COSDictionary) {
            ((COSDictionary) cosBase).entrySet().stream()
                .filter(cosNameCOSBaseEntry -> !WhiteList.isKeyExcluded(cosNameCOSBaseEntry.getKey()))
                .forEach(cosNameCOSBaseEntry -> {
                    if (!WhiteList.isNameWhitelisted(cosNameCOSBaseEntry.getKey())) {
                        res.add(new PdfValidationResult(cosBase, cosNameCOSBaseEntry.getKey(), objNum, generation));
                    }
                    if (!(cosNameCOSBaseEntry.getValue() instanceof COSObject) && !WhiteList.isKeyWhitelisted(cosNameCOSBaseEntry.getKey())) {
                        res.addAll(whitelistValidation(cosNameCOSBaseEntry.getValue()));
                    }
                });
        } else if (cosBase instanceof COSArray) {
            ((COSArray) cosBase).forEach(cosBase1 -> {
                if (!(cosBase1 instanceof COSObject)) {
                    res.addAll(whitelistValidation(cosBase1));
                }
            });
        } else if (cosBase instanceof COSObject) {
            var cosObject = ((COSObject) cosBase);
            this.objNum = cosObject.getObjectNumber();
            this.generation = cosObject.getGenerationNumber();
            res.addAll(whitelistValidation(cosObject.getObject()));
        }
        return res;
    }
}
