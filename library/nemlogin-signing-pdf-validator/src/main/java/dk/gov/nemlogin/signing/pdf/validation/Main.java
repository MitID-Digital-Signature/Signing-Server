package dk.gov.nemlogin.signing.pdf.validation;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.model.SignersDocument.PdfSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocumentFile;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * CLI utility class for validating PDF files according the the SignPDF standard defined by NemLog-In.
 * <p>
 * Usage:<br>
 * Build this sub-project with the "standalone-pdf-validator" maven profile, i.e.
 * <pre>
 *     mvn clean package -Pstandalone-pdf-validator
 * </pre>
 * <p>
 * The resulting Jar is called to validate individual PDF files or folders of PDF files using:
 * <pre>
 *     java -jar target/nemlogin-signing-pdf-validator-cli.jar path-to-files-or-folders
 * </pre>
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    // "%PDF-"
    private static final byte[] pdfHeader = {0x25, 0x50, 0x44, 0x46, 0x2D};
    private static final PdfValidationService validationService = new PdfValidationService();
    private static final AtomicBoolean success = new AtomicBoolean(true);

    /**
     * Validates the PDF or PDFs pointed out by the arguments
     *
     * @param args the paths to PDF files or folders with PDF files
     */
    public static void main(String[] args) {
        // Sanity check
        if (args == null || args.length == 0) {
            LOG.error("Specify one or more path to a PDF file or a folder to PDF files");
            success.set(false);

        } else {
            for (String arg : args) {
                try {
                    Files.find(Paths.get(arg), Integer.MAX_VALUE, Main::isPdf)
                        .forEach(Main::validatePdf);
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
     * Check if the file is a PDF file
     *
     * @param path the file path
     * @param attr the file attributes
     * @return if the given file is a PDF file
     */
    private static boolean isPdf(Path path, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            // Check for PDF File header ... slightly more fun than looking at file extensions
            try (var fc = FileChannel.open(path)) {
                var bb = ByteBuffer.allocate(pdfHeader.length);
                fc.read(bb);
                bb.flip();
                return IntStream.range(0, pdfHeader.length).allMatch(i -> pdfHeader[i] == bb.get(i));
            } catch (Exception e) {
                LOG.trace("error ignored", e);
            }
        }
        return false;
    }


    /**
     * Validates that the PDF has a proper SignPDF format.
     * Emits the validation result to the console.
     *
     * @param path the PDF file to validate
     */
    private static void validatePdf(Path path) {
        try {
            SignersDocumentFile file = SignersDocumentFile.builder()
                .setPath(path.toString())
                .build();
            var pdf = new PdfSignersDocument(file);
            validationService.validate(new TransformationContext(pdf, null, null));
            LOG.info("Successfully Validated {}", path);
        } catch (NemLogInException e) {
            LOG.error("Error validating {}:\n        {}", path, e.getMessage(), e);
            success.set(false);
        }
    }
}
