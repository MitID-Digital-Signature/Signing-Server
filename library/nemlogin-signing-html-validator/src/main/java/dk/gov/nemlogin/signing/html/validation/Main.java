package dk.gov.nemlogin.signing.html.validation;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.model.SignersDocument.HtmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocumentFile;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

import static dk.gov.nemlogin.signing.util.SigningUtils.fileExtension;

/**
 * CLI utility class for validating HTML files according the the SignHTML standard defined by NemLog-In.
 * <p>
 * Usage:<br>
 * Build this sub-project with the "standalone-html-validator" maven profile, i.e.
 * <pre>
 *     mvn clean package -Pstandalone-html-validator
 * </pre>
 *
 * The resulting Jar is called to validate individual HTML files or folders of HTML files using:
 * <pre>
 *     java -jar target/nemlogin-signing-html-validator-cli.jar path-to-files-or-folders
 * </pre>
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final HtmlValidationService validationService = new HtmlValidationService();
    private static AtomicBoolean success = new AtomicBoolean(true);

    /**
     * Validates the HTML or HTMLs pointed out by the arguments
     *
     * @param args the paths to HTML files or folders with HTML files
     */
    public static void main(String[] args) {
        // Sanity check
        if (args == null || args.length == 0) {
            LOG.error("Specify one or more path to a HTML file or a folder to HTML files");
            success.set(false);

        } else {
            for (String arg : args) {
                try {
                    Files.find(Paths.get(arg), Integer.MAX_VALUE, Main::isHtml)
                        .forEach(Main::validateHtml);
                } catch (IOException e) {
                    LOG.error("Skipping invalid file path {}: {}", arg, e.getMessage(), e);
                    success.set(false);
                }
            }
        }

        // Pass on result to caller
        System.exit(success.get() ? 0 : 1);
    }


    /**
     * Check if the file is a HTML file
     *
     * @param path the file path
     * @param attr the file attributes
     * @return if the given file is a HTML file
     */
    private static boolean isHtml(Path path, BasicFileAttributes attr) {
        return attr.isRegularFile() && "html".equalsIgnoreCase(fileExtension(path.getFileName().toString()));
    }


    /**
     * Validates that the HTML has a proper SignHTML format.
     * Emits the validation result to the console.
     *
     * @param path the HTML file to validate
     */
    private static void validateHtml(Path path) {
        try {
            SignersDocumentFile file = SignersDocumentFile.builder()
                .setPath(path.toString())
                .build();
            var html = new HtmlSignersDocument(file);
            validationService.validate(new TransformationContext(html, null, null));
            LOG.info("Successfully Validated {}", path);
        } catch (NemLogInException e) {
            LOG.error("Error validating {}:\n        {}", path, e.getMessage(), e);
            success.set(false);
        }
    }
}
