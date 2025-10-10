package dk.gov.nemlogin.signing.pdf.validation;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is used to validate fonts.
 * Only embedded fonts or standard fonts are allowed
 */
public class PdfFontValidator {
    private static final HashSet<String> standardFonts = new HashSet<>();

    static {
        standardFonts.add("Helvetica");
        standardFonts.add("Helvetica-Oblique");
        standardFonts.add("Helvetica-Bold");
        standardFonts.add("Helvetica-BoldOblique");
        standardFonts.add("Times-Roman");
        standardFonts.add("Times-Italic");
        standardFonts.add("Times-Bold");
        standardFonts.add("Times-BoldItalic");
        standardFonts.add("Courier");
        standardFonts.add("Courier-Oblique");
        standardFonts.add("Courier-Bold");
        standardFonts.add("Courier-BoldOblique");
        standardFonts.add("Symbol");
        standardFonts.add("ZapfDingbats");
    }

    private PdfFontValidator() {
    }

    /**
     * This will scan for dictionary objects of type Font or FontDescriptor
     *
     * @param cosDocument COSDocument holding the pdf documents COS structure
     * @return list of {@link PdfFontDescriptor} holding the font definitions in the pdf document
     */
    public static List<PdfFontDescriptor> scanForFonts(final COSDocument cosDocument) {
        return cosDocument.getObjects().stream()
            .filter(cosObject -> cosObject.getObject() instanceof COSDictionary)
            .map(cosObject -> {
                COSDictionary dict = (COSDictionary) cosObject.getObject();
                var cosBase = dict.getDictionaryObject("Type");
                if (cosBase instanceof COSName) {
                    if ("Font".equals(((COSName) cosBase).getName())) {
                        if (dict.getDictionaryObject("FontDescriptor") == null) {
                            return PdfFontDescriptor.fromFont(cosObject, dict);
                        }
                    } else if ("FontDescriptor".equals(((COSName) cosBase).getName())) {
                        return PdfFontDescriptor.fromFontDescriptor(cosObject, dict);
                    }
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


    /**
     * Returns if the given font is one of the standard 14 PDF fonts
     *
     * @param fontName the name of the font to check
     * @return if the given font is one of the standard 14 PDF fonts
     */
    private static boolean isStandardFont(COSName fontName) {
        if (fontName == null) {
            return false;
        }
        return standardFonts.contains(fontName.getName());
    }

    /**
     * Run through the fonts defined in the document and assure that only standard fonts or embedded fonts are used
     *
     * @param cosDocument COSDocument holding the pdf documents COS structure
     * @return list of {@link PdfValidationResult} holding the font-validation errors in the pdf document
     */
    public static List<PdfValidationResult> validateFonts(final COSDocument cosDocument) {
        return scanForFonts(cosDocument).stream()
            .filter(font -> !font.isEmbedded() && !isStandardFont(font.getFontName()))
            .map(font -> new PdfValidationResult(font.getFontObject(), font.getFontName(),
                font.getCosObject().getObjectNumber(), font.getCosObject().getGenerationNumber()))
            .collect(Collectors.toList());
    }
}
