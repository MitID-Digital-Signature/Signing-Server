package dk.gov.nemlogin.signing.pdf.transformation;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.service.impl.Abstract2PdfFormatTransformationService;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static dk.gov.nemlogin.signing.pdf.TransformationPropertiesHandler.*;

/**
 * Tests using transformation properties to control HTML -> PDF transformation.
 * <p>
 * The properties are documented in the "nemlogin-signing-pdf-generator" module.
 */
class PdfTransformationPropertiesTest {

    Properties props;

    @BeforeEach
    void init() {
        props = new Properties();
    }


    /**
     * Tests color space handling
     **/
    @Test
    void testColorProfileHandling() throws IOException, NemLogInException {
        props.setProperty(KEY_COLOR_PROFILE, "default");
        PDDocument doc = generatePdf(props, "", "<p>test</p>");
        List<String> colorProfile = colorProfiles(doc);
        Assertions.assertEquals(1, colorProfile.size());
        // sRGB
        Assertions.assertEquals("sRGB IEC61966-2.1", colorProfile.get(0));

        props.setProperty(KEY_COLOR_PROFILE, "none");
        doc = generatePdf(props, "", "<p>test</p>");
        colorProfile = colorProfiles(doc);
        Assertions.assertEquals(0, colorProfile.size());

        props.setProperty(KEY_COLOR_PROFILE, "classpath:/test-sRGB.icc");
        doc = generatePdf(props, "", "<p>test</p>");
        colorProfile = colorProfiles(doc);
        Assertions.assertEquals(1, colorProfile.size());
    }


    /**
     * Tests font handling
     **/
    @Test
    void testFontHandling() throws IOException, NemLogInException {
        props.setProperty(KEY_FONTS, "default");
        PDDocument doc = generatePdf(props, "body { font-family: Helvetica; }", "<p>test</p>");
        List<String> fontNames = fontNames(doc);
        Assertions.assertEquals(1, fontNames.size());
        Assertions.assertEquals("Helvetica", fontNames.get(0));

        props.setProperty(KEY_FONTS, "embed");
        props.setProperty(KEY_FONT_NAME.replace("[x]", "[0]"), "Karla");
        props.setProperty(KEY_FONT_PATH.replace("[x]", "[0]"), "classpath:/Karla-Bold.ttf");
        doc = generatePdf(props, "body { font-family: Karla; }", "<p>test</p>");
        fontNames = fontNames(doc);
        Assertions.assertEquals(1, fontNames.size());
        Assertions.assertTrue(fontNames.get(0).contains("Karla"));
    }


    /**
     * Tests page size and margin.
     **/
    @Test
    void testPageSizeAndMargin() throws IOException, NemLogInException {
        props.setProperty(KEY_PAGE_SIZE, "a3 landscape");
        props.setProperty(KEY_PAGE_MARGIN, "10cm 4cm");
        PDDocument doc = generatePdf(props, "body { font-family: Helvetica; }", "<p>test</p>");
        PDRectangle mediaBox = doc.getPage(0).getMediaBox();
        // Landscape
        // because of page orientation the Width and Height has been switched
        Assertions.assertEquals(Math.round(PDRectangle.A3.getHeight()), Math.round(mediaBox.getWidth()),
            "Page layout must match a3 landscape (height)");
        Assertions.assertEquals(Math.round(PDRectangle.A3.getWidth()), Math.round(mediaBox.getHeight()),
            "Page layout must match a3 landscape (width)");

        // Hack to extract a rough top-left position of "test" on PDF page
        PDFTextStripper textStripper = new PDFTextStripper() {
            int posToCm(float pos) { return (int)Math.round(pos * 25.4 / 72 / 10); }

            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                // Letter "t"
                TextPosition p0 = textPositions.get(0);
                writeString(String.format("x: %scm, y: %scm",
                    posToCm(p0.getXDirAdj()),
                    // Deduct font height
                    posToCm(p0.getYDirAdj() - p0.getFontSizeInPt())));
            }
        };
        Assertions.assertEquals("x: 4cm, y: 10cm", textStripper.getText(doc).trim());


        // Will override size and margin with free-style CSS 2.1 page declaration
        String pageStyle = "@page {\n" +
            "    size: a5 portrait;\n" +
            "    margin: 10mm;\n" +
            "}";
        props.setProperty(KEY_PAGE_STYLE, pageStyle);
        doc = generatePdf(props, "body { font-family: Helvetica; }", "<p>test</p>");
        mediaBox = doc.getPage(0).getMediaBox();
        // portrait
        Assertions.assertTrue(mediaBox.getWidth() < mediaBox.getHeight());
        Assertions.assertEquals(Math.round(PDRectangle.A5.getWidth()), Math.round(mediaBox.getWidth()));
        Assertions.assertEquals(Math.round(PDRectangle.A5.getHeight()), Math.round(mediaBox.getHeight()));
        Assertions.assertEquals("x: 1cm, y: 1cm", textStripper.getText(doc).trim());
    }


    /**
     * Generates a PDF using the given transformation properties
     * @param props the transformation properties
     * @return the PDF
     */
    @SuppressWarnings("SameParameterValue")
    private PDDocument generatePdf(Properties props, String style, String body) throws IOException, NemLogInException {
        String html = String.format("<html><head><style>%s</style></head><body>%s</html>", style, body);
        byte[] pdf = Abstract2PdfFormatTransformationService.generatePDF(html, "test.pdf", props);
        return PDDocument.load(pdf);
    }


    /** Extract color profiles from the pdf **/
    private List<String> colorProfiles(PDDocument doc) {
        List<String> colorProfiles = new ArrayList<>();
        for (PDOutputIntent e : doc.getDocumentCatalog().getOutputIntents()) {
            colorProfiles.add(e.getInfo());
        }
        return colorProfiles;
    }


    /** Extract font names from the pdf **/
    private List<String> fontNames(PDDocument doc) throws IOException {
        List<String> fontNames = new ArrayList<>();
        for(PDPage page : doc.getPages()) {
            PDResources res = page.getResources();
            for (COSName fontName : res.getFontNames()) {
                PDFont font = res.getFont(fontName);
                fontNames.add(font.getName());
            }
        }
        return fontNames;
    }
}
