package dk.gov.nemlogin.signing.pdf.validation;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;

/**
 * This is for holding information about fonts to be validated
 */
public class PdfFontDescriptor {
    private final COSObject cosObject;
    private final COSDictionary fontObject;
    private final COSName fontName;
    private final boolean embedded;

    private PdfFontDescriptor(COSObject cosObject, COSDictionary fontObject, COSName fontName, boolean embedded) {
        this.cosObject = cosObject;
        this.fontObject = fontObject;
        this.fontName = fontName;
        this.embedded = embedded;
    }

    public COSName getFontName() {
        return fontName;
    }

    public COSObject getCosObject() {
        return cosObject;
    }

    public COSDictionary getFontObject() {
        return fontObject;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public static PdfFontDescriptor fromFontDescriptor(COSObject cosObject, COSDictionary dict) {
        PdfFontDescriptor res = null;
        var cosBase = dict.getDictionaryObject("FontName");
        if (cosBase instanceof COSName) {
            var fontNameCosName = (COSName) cosBase;
            boolean embedded = dict.getDictionaryObject("FontFile") != null || dict.getDictionaryObject("FontFile2") != null || dict.getDictionaryObject("FontFile3") != null;
            res = new PdfFontDescriptor(cosObject, dict, fontNameCosName, embedded);
        }
        return res;
    }

    public static PdfFontDescriptor fromFont(COSObject cosObject, COSDictionary dict) {
        var baseFontCosBase = dict.getDictionaryObject("BaseFont");
        COSName baseFontCosName = null;
        if (baseFontCosBase instanceof COSName) {
            baseFontCosName = (COSName) baseFontCosBase;
        }
        String subType = null;
        var subTypeCosBase = dict.getDictionaryObject("Subtype");
        if (subTypeCosBase instanceof COSName) {
            subType = ((COSName) subTypeCosBase).getName();
        }

        if ("Type0".equals(subType)) {
            return new PdfFontDescriptor(cosObject, dict, baseFontCosName, true);
        } else if ("Type1".equals(subType) || "MMType1".equals(subType) || "TrueType".equals(subType)) {
            boolean embedded = dict.getDictionaryObject("FontDescriptor") != null;
            return new PdfFontDescriptor(cosObject, dict, baseFontCosName, embedded);
        } else if ("Type3".equals(subType) || "CIDFontType0".equals(subType) || "CIDFontType2".equals(subType)) {
            return new PdfFontDescriptor(cosObject, dict, baseFontCosName, true);
        } else {
// Unknown. Assume not embedded
            return new PdfFontDescriptor(cosObject, dict, baseFontCosName, false);
        }
    }
}
