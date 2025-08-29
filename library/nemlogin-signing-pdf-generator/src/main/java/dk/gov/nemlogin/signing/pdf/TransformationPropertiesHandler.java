package dk.gov.nemlogin.signing.pdf;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK007;
import static dk.gov.nemlogin.signing.util.SignSDKVersion.getProducer;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER_BOLD;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER_BOLD_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.SYMBOL;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_BOLD;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_BOLD_ITALIC;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_ITALIC;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_ROMAN;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.ZAPF_DINGBATS;

/**
 * Used for customizing the HTML -> PDF transformation.
 * <p>
 * Properties are defined below. All properties have a "nemlogin.signing.pdf-generator." prefix.
 * <table>
 *   <tr>
 *     <th>Property</th>
 *     <th>Default Value</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>color-profile</td>
 *     <td>default</td>
 *     <td>"default" adds a default color profile.<br>"none" adds no profile.<br>
 *         All other values are treated as a file path to a .icc file.</td>
 *   </tr>
 *   <tr>
 *     <td>fonts</td>
 *     <td>default</td>
 *     <td>"default" adds support for 14 standard PDF fonts.<br>
 *         "embed" will embed the following list of fonts.</td>
 *   </tr>
 *   <tr>
 *     <td>font[x].name</td>
 *     <td></td>
 *     <td>Name of the x'th font to embed.</td>
 *   </tr>
 *   <tr>
 *     <td>font[x].path</td>
 *     <td></td>
 *     <td>Path of the x'th font to embed.</td>
 *   </tr>
 *   <tr>
 *     <td>page-size</td>
 *     <td>a4 portrait</td>
 *     <td>The CSS 2.1 @page size.</td>
 *   </tr>
 *   <tr>
 *     <td>page-margin</td>
 *     <td>1cm</td>
 *     <td>The CSS 2.1 @page margin.</td>
 *   </tr>
 *   <tr>
 *     <td>page-style</td>
 *     <td></td>
 *     <td>The page-style will be injected in the HTML as a &lt;style> element.<br>
 *         If defined, page-size and page-margin is ignored</td>
 *   </tr>
 * </table>
 */
public class TransformationPropertiesHandler {

    // Transformation properties
    public static final String KEY_PREFIX           = "nemlogin.signing.pdf-generator.";
    public static final String KEY_COLOR_PROFILE    = KEY_PREFIX + "color-profile";
    public static final String KEY_FONTS            = KEY_PREFIX + "fonts";
    public static final String KEY_FONT_NAME        = KEY_PREFIX + "font[x].name";
    public static final String KEY_FONT_PATH        = KEY_PREFIX + "font[x].path";
    public static final String KEY_BODY_FONT        = KEY_PREFIX + "body-font";
    public static final String KEY_MONOSPACE_FONT   = KEY_PREFIX + "monospace-font";
    public static final String KEY_PAGE_SIZE        = KEY_PREFIX + "page-size";
    public static final String KEY_PAGE_MARGIN      = KEY_PREFIX + "page-margin";
    public static final String KEY_PAGE_STYLE       = KEY_PREFIX + "page-style";
    // pdf colour profile default key value
    private static final String DEFAULT_COLOR_PROFILE_DEFAULT_VALUE = "default";

    /**
     * Default color profile
     * from https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/resources/org/apache/pdfbox/resources/pdfa/
     */
    private static final String DEFAULT_COLOR_PROFILE = "/sRGB.icc";

    /** 14 standard PDF fonts **/
    private static final PDType1Font[] DEFAULT_PDF_FONTS = {
        TIMES_ROMAN, TIMES_BOLD, TIMES_ITALIC, TIMES_BOLD_ITALIC,
        HELVETICA, HELVETICA_BOLD, HELVETICA_OBLIQUE, HELVETICA_BOLD_OBLIQUE,
        COURIER, COURIER_BOLD, COURIER_OBLIQUE, COURIER_BOLD_OBLIQUE,
        SYMBOL, ZAPF_DINGBATS
    };


    private final TransformationContext ctx;


    /** Constructor **/
    public TransformationPropertiesHandler(TransformationContext ctx) {
        this.ctx = ctx;
    }


    /**
     * Get the relevant HTML -> PDF transformation properties to the HTML
     * @return the updated HTML
     */
    public String getPageStyle() {
        var pageStyle = getStringProperty(KEY_PAGE_STYLE, null);

        if (SigningUtils.isEmpty(pageStyle)) {
            var pageSize = getStringProperty(KEY_PAGE_SIZE, "a4 portrait");
            var pageMargin = getStringProperty(KEY_PAGE_MARGIN, "1cm");

            pageStyle = String.format("@page {%n" +
                "    size: %s;%n" +
                "    margin: %s;%n" +
                "}", pageSize, pageMargin);
        }
        return pageStyle;
    }

    /**
     * Applies the relevant HTML -> PDF transformation properties to the {@link PdfRendererBuilder}
     * @param pdfRendererBuilder the PDF renderer to update according to transformation properties
     */
    public void applyProperties(PdfRendererBuilder pdfRendererBuilder) throws TransformationException {

        // Update color profile
        var colorProfile = getStringProperty(KEY_COLOR_PROFILE, DEFAULT_COLOR_PROFILE_DEFAULT_VALUE);
        try {
            if (DEFAULT_COLOR_PROFILE_DEFAULT_VALUE.equalsIgnoreCase(colorProfile)) {
                pdfRendererBuilder.useColorProfile(SigningUtils.loadBytes(DEFAULT_COLOR_PROFILE));
            } else if (!"none".equalsIgnoreCase(colorProfile)) {
                pdfRendererBuilder.useColorProfile(SigningUtils.loadBytesByProtocol(colorProfile));
            }
        } catch (IOException e) {
            throw new TransformationException(SDK007, ctx, "Error applying color profile: " + colorProfile, e);
        }

        // Update fonts
        String[] fonts = getStringProperty(KEY_FONTS, DEFAULT_COLOR_PROFILE_DEFAULT_VALUE).split("\\s*,\\s*");
        if (Stream.of(fonts).anyMatch(DEFAULT_COLOR_PROFILE_DEFAULT_VALUE::equalsIgnoreCase)) {
            addDefaultPdfFonts(pdfRendererBuilder);
        }
        if (Stream.of(fonts).anyMatch("embed"::equalsIgnoreCase)) {
            addSpecifiedPdfFonts(pdfRendererBuilder);
        }

        // add signSDK as Producer
        pdfRendererBuilder.withProducer(getProducer());
    }


    /**
     * Returns the property with the given key
     * @param key the key
     * @param defaultValue default value
     * @return the property value
     */
    private String getStringProperty(String key, String defaultValue) {
        return ctx.getTransformationProperties() == null
            ? defaultValue
            : ctx.getTransformationProperties().getProperty(key, defaultValue);
    }


    /**
     * Returns the property with the given key and index.
     * The "x" of the "[x]" part of the key will be replaced with the proper index.
     * @param key the key
     * @param x the index
     * @return the property value
     */
    private String getIndexedStringProperty(String key, int x) {
        return getStringProperty(key.replace("[x]", String.format("[%d]", x)), null);
    }


    /**
     * Adds support for the default 14 PDF fonts to the PDF
     * @param pdfRendererBuilder the PDF renderer
     */
    private void addDefaultPdfFonts(PdfRendererBuilder pdfRendererBuilder) {
        Stream.of(DEFAULT_PDF_FONTS)
            .forEach(font -> pdfRendererBuilder.useFont(new PDFontSupplier(font), font.getName()));
    }


    /**
     * Adds the fonts defined by the properties
     * @param pdfRendererBuilder the PDF renderer
     */
    private void addSpecifiedPdfFonts(PdfRendererBuilder pdfRendererBuilder) {
        for (var x = 0; true; x++) {
            var fontName = getIndexedStringProperty(KEY_FONT_NAME, x);
            var fontPath = getIndexedStringProperty(KEY_FONT_PATH, x);
            if (SigningUtils.isEmpty(fontName) || SigningUtils.isEmpty(fontPath)) {
                return;
            }
            pdfRendererBuilder.useFont(new FontSupplier(ctx, fontPath), fontName);
        }
    }

    /**
     * Get body font defined in transformation properties
     * @return default font
     */
    public String getBodyFont() {
        return getStringProperty(KEY_BODY_FONT, "medium Helvetica");
    }

    /**
     * Get monospace font defined in transformation properties
     * @return default monospace font
     */
    public String getMonospaceFont() {
        return getStringProperty(KEY_MONOSPACE_FONT, "medium Courier");
    }

    /**
     * Provides an implementation to lazy load fonts
     */
    public static class FontSupplier implements FSSupplier<InputStream> {

        private final String fontPath;
        private final TransformationContext ctx;

        /** Constructor **/
        public FontSupplier(TransformationContext ctx, String fontPath) {
            this.ctx = ctx;
            this.fontPath = fontPath;
        }


        /** {@inheritDoc} **/
        @Override
        @SuppressWarnings("java:S112")
        public InputStream supply() {
            try {
                byte[] font = SigningUtils.loadBytesByProtocol(fontPath);
                return new ByteArrayInputStream(font);
            } catch (IOException e) {
                throw new RuntimeException(new TransformationException(SDK007, ctx, "Invalid font path: " + fontPath, e));
            }
        }
    }
}
