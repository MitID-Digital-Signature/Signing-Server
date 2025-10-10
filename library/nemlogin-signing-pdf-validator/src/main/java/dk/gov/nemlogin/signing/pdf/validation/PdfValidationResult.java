package dk.gov.nemlogin.signing.pdf.validation;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;

import java.util.List;

import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 * This is used for holding the validation problems from validating the pdf document
 */
@SuppressWarnings("unused")
public class PdfValidationResult {
    private COSName cosName;
    private COSBase cosBase;
    private long objNum;
    private int generation;

    public PdfValidationResult(COSBase cosBase, COSName cosName, long objNum, int generation) {
        this.cosBase = cosBase;
        this.cosName = cosName;
        this.objNum = objNum;
        this.generation = generation;
    }

    public COSBase getCosBase() {
        return cosBase;
    }

    public long getObjNum() {
        return objNum;
    }

    public int getGeneration() {
        return generation;
    }

    public COSName getCosName() {
        return cosName;
    }

    public void setCosName(COSName cosName) {
        this.cosName = cosName;
    }

    public String getCosNameStr() {
        return cosName.getName();
    }

    public String getObjId() {
        return String.format("%d %d", objNum, generation);
    }


    /**
     * Returns a string representation for a list of validation results
     * @param validationResults the validation results
     * @param compact whether to generate a single-line string or a multiline string
     * @param maxLen the maximum length of the generated string representation
     * @return a string representation for list of validation results
     */
    public static String toString(List<PdfValidationResult> validationResults, boolean compact, int maxLen) {
        String delimiterFormat = compact ? ", " : "\n";
        String str = validationResults.stream()
            .collect(groupingBy(PdfValidationResult::getCosNameStr))
            .entrySet().stream()
            .sorted(comparingByKey(String::compareToIgnoreCase))
            .map(e -> String.format("%s [%s]",
                e.getKey(),
                e.getValue().stream().map(PdfValidationResult::getObjId).collect(joining(", "))))
            .collect(joining(delimiterFormat));
        return str.length() > maxLen ? str.substring(0, maxLen - 3) + "..." : str;
    }
}
