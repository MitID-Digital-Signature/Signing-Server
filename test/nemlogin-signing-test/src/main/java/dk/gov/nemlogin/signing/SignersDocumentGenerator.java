package dk.gov.nemlogin.signing;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.service.impl.Abstract2PdfFormatTransformationService;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Generates Signer's Documents test data, which may be used for signing tests.<br>
 * Copy the generated documents to "/examples/nemlogin-signing-webapp/signers-documents"
 * in order to use them from the example application.
 * <p>
 * Manually execute {@link #main(String[])} to generate the test data.
 * <p>
 * The produced HTML SD is deliberately designed to include a large percentage of the allowed HTML tags and
 * CSS elements of the SignHTML specification.<br>
 * The produced PDF SD is created from the HTML, and the produced XML SD (incl XSL) will in turn produce the HTML.
 * <p>
 * Credits: Test data for the Signer's Documents fetched from: https://kalliope.org
 */
public class SignersDocumentGenerator {

    // Use as root for loading documents if found locally
    static final String   DEFAULT_DOCUMENT_ROOT = "file:./target/";
    static final List<String> DEFAULT_KALLIOPE_URLS = Arrays.asList(
        "https://kalliope.org/static/api/texts/8/e9/petersen2017120113.json",
        "https://kalliope.org/static/api/texts/5/c5/heiberg2002011324.json"
    );

    private static final Logger LOG = LoggerFactory.getLogger(SignersDocumentGenerator.class);
    private static final UnaryOperator<String> xsltPath = name -> String.format("classpath:/test-data-xslt/%s", name);

    private final String documentRoot;
    private final List<String> kalliopeUrls;
    private final Properties transformationProperties;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    // NB: Use Saxon XSLT 3, since it allows for conversion of JSON -> XML.
    private final TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();


    /**
     * Called in order to generate test data
     * @param args optional first argument is the destination folder. Subsequent argument must be URLs to
     *             Kalliope JSON documents.
     */
    public static void main(String[] args) throws IOException {

        String documentRoot = args.length > 0 && !SigningUtils.isEmpty(args[0])
            ? args[0]
            : DEFAULT_DOCUMENT_ROOT;

        List<String> kalliopeUrls = args.length > 1
            ? Stream.of(args).skip(1).collect(Collectors.toList())
            : DEFAULT_KALLIOPE_URLS;

        var testDataGenerator = new SignersDocumentGenerator(documentRoot, kalliopeUrls);
        testDataGenerator.generateSignersDocuments();
    }


    /** Designated Constructor */
    public SignersDocumentGenerator(String documentRoot, List<String> kalliopeUrls, Properties transformationProperties) {
        this.kalliopeUrls = kalliopeUrls;
        this.documentRoot = Objects.requireNonNull(documentRoot);
        this.transformationProperties = transformationProperties;
        LOG.info("Initialized with documentRoot: {}, kalliopeUrls: {}",
            documentRoot, Collections.singletonList(kalliopeUrls));
    }

    /** Constructor */
    public SignersDocumentGenerator(String documentRoot, List<String> kalliopeUrls) {
        this(documentRoot, kalliopeUrls, null);
    }

    /** Constructor */
    public SignersDocumentGenerator(String documentRoot) {
        this(documentRoot, DEFAULT_KALLIOPE_URLS);
    }


    /**
     * Generates Signer's Documents for all configured Kalliope URLs
     */
    public void generateSignersDocuments() throws IOException {
        for (String kalliopeUrl : kalliopeUrls) {
            try {
                generateSignersDocuments(kalliopeUrl.trim());
            } catch (Exception e) {
                throw new IOException("Error generating test data for: " + kalliopeUrl, e);
            }
        }
    }


    /**
     * Generate Signer's Documents from the given URL
     * @param url the URL to load and generate Signer's Documents from
     */
    private void generateSignersDocuments(String url) throws IOException, NemLogInException {

        var json = loadJson(url);
        var jsonXml = String.format("<json><![CDATA[%s]]></json>", json);
        var fileName = urlFileName(url);
        var baseFileName = fileName.substring(0, fileName.lastIndexOf('.'));

        // Produce xml, html and plain text from JSON
        var xml = xslt(xsltPath.apply("json-to-xml.xsl"), jsonXml);
        var html = xslt(xsltPath.apply("xml-to-html.xsl"), xml);
        var text = xslt(xsltPath.apply("xml-to-text.xsl"), xml);

        // Save in destination folder
        Path dir = getDocumentDir();
        Files.write(dir.resolve(baseFileName + ".xml"), xml.getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve(baseFileName + ".html"), html.getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve(baseFileName + ".txt"), text.getBytes(StandardCharsets.UTF_8));

        // Also save XSLT
        Files.write(dir.resolve(baseFileName + ".xsl"), resourceLoader
            .getResource(xsltPath.apply("xml-to-html.xsl"))
            .getInputStream()
            .readAllBytes());

        // Generate PDF Data
        Files.write(dir.resolve(baseFileName + ".pdf"),
            Abstract2PdfFormatTransformationService.generatePDF(html, baseFileName + ".html", transformationProperties));
    }


    /**
     * Creates and returns the configured destination folder
     */
    private Path getDocumentDir() throws IOException {
        var documentDir = resourceLoader.getResource(documentRoot);
        var dir = documentDir.getFile();
        if (dir.exists() && !dir.isDirectory()) {
            LOG.error("Invalid directory '{}'", dir);
            throw new IOException("Invalid directory " + dir);
        } else if (!dir.exists()) {
            LOG.info("Creating documents directory '{}' : {}", dir, dir.mkdirs());
        }
        return dir.toPath();
    }


    /**
     * Loads JSON from the given URL, unless we have a local version of the file.
     * @param url the URL to load the JSON from
     * @return the JSON
     */
    private String loadJson(String url) throws IOException {

        // Check if we have a locally cached version of the requested file first
        var file = resourceLoader.getResource(String.format("classpath:kalliope/%s", urlFileName(url)));
        if (!file.exists()) {
            file = resourceLoader.getResource(String.format("url:%s", url));
        }

        LOG.info("Loading test data from: {}", file);
        try (var in = file.getInputStream()) {
            var json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // Clean up
            json = json.replace("\u0061\u030A", "\u00e5");
            return json;
        }
    }


    /** Returns the file name associated with an URL **/
    private String urlFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }


    /**
     * Performs an XSLT
     * @param xsltPath path to XSLT
     * @param xml the actual XML
     * @return the XSLT result
     */
    private String xslt(String xsltPath, String xml) throws IOException {
        try (var in = resourceLoader.getResource(xsltPath).getInputStream()) {
            var xsltSource = new StreamSource(in);
            var xmlSource = new StreamSource(new StringReader(xml));
            var writer = new StringWriter();
            try {
                factory.newTransformer(xsltSource)
                    .transform(xmlSource, new StreamResult(writer));
            } catch (TransformerException e) {
                throw new IOException(e);
            }
            return writer.toString();
        }
    }
}
