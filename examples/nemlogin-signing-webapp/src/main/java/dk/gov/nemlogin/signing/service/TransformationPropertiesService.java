package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.SignersDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.awt.Font;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

import static dk.gov.nemlogin.signing.pdf.TransformationPropertiesHandler.KEY_PREFIX;
import static dk.gov.nemlogin.signing.service.SignersDocumentService.DEFAULT_DOCUMENT_ROOT;
import static dk.gov.nemlogin.signing.util.SigningUtils.stripFileExtension;

/**
 * When transforming SD's to PAdES, fonts can be embedded in the generated PDF using transformation properties
 * as detailed in /library/nemlogin-signing-pdf-generator/readme.md.
 * <p>
 * This service serves as a demonstration of this, by naively embedding a ttf or otf font with the same
 * file name (excl. extension) as the SD being signed.
 */
@Service
public class TransformationPropertiesService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformationPropertiesService.class);
    private static final String FONT_EXTENSION = "\\.(?i)(ttf|otf)";
    private final SignersDocumentService signersDocumentService;

    /** Constructor **/
    public TransformationPropertiesService(SignersDocumentService signersDocumentService) {
        this.signersDocumentService = signersDocumentService;
    }


    /**
     * As a simple way of testing embedded PDF fonts, if there is a ttf file with the same name as the SD,
     * then it will be embedded in the generated PDF
     *
     * @return transformation properties
     */
    public Properties getTransformationProperties(SignersDocument sd, SignatureFormat signatureFormat) {
        // Check that we actually generate PDF
        if (signatureFormat == SignatureFormat.PAdES && sd.getFormat() != DocumentFormat.PDF) {

            try {
                // Look for ttf & otf fonts with the same file name as the SD
                String fontFileMatch = Pattern.quote(stripFileExtension(sd.getName())) + FONT_EXTENSION;
                return signersDocumentService.resources(fontFileMatch, true)
                    .map(FontSpec::read)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(font -> {
                        LOG.info("Embedding font '{}'", font.getName());
                        Properties props = new Properties();
                        // Embed the found font along with the 14 standard PDF fonts
                        props.setProperty(KEY_PREFIX + "fonts", "embed, default");
                        props.setProperty(KEY_PREFIX + "font[0].name", font.getName());
                        props.setProperty(KEY_PREFIX + "font[0].path", font.getPath());
                        props.setProperty(KEY_PREFIX + "body-font", "medium " + font.getName());
                        props.setProperty(KEY_PREFIX + "monospace-font", "medium Courier, " + font.getName());
                        return props;
                    })
                    .orElse(null);

            } catch (Exception e) {
                LOG.trace("error ignored", e);
            }
        }
        return null;
    }


    /**
     * Encapsulates a font path and name
     */
    static class FontSpec {

        private final String path;
        private final String name;

        /** Constructor **/
        public FontSpec(String path, String name) {
            this.path = path;
            this.name = name;
        }

        /**
         * Reads the TTF or OTF font resource as a {@link FontSpec}, or returns null if unreadable.
         *
         * @param resource the font resource
         * @return the font {@link FontSpec}, or null if unreadable
         */
        public static FontSpec read(Resource resource) {
            try (InputStream in = resource.getInputStream()) {
                String path = resource.isFile()
                    ? "file:" + resource.getFile().getPath()
                    : DEFAULT_DOCUMENT_ROOT + resource.getFilename();
                return new FontSpec(path, Font.createFont(Font.TRUETYPE_FONT, in).getName());
            } catch (Exception e) {
                LOG.error("Error reading font {}", resource, e);
            }
            return null;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }
    }
}
