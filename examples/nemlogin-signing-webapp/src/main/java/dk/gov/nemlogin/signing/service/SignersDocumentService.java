package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.config.SigningConfigurationProperties;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.model.SignProperties;
import dk.gov.nemlogin.signing.model.SignProperties.StringValue;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.HtmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.PdfSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.PlainTextSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocument.XmlSignersDocument;
import dk.gov.nemlogin.signing.model.SignersDocumentFile;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used for loading SD's (Signer's Documents) from either a local file or from classpath.
 * <p>
 * The local file system is defined as a "documents" folder containing at least one .pdf, .xml, .txt or .html file.
 */
@Component
public class SignersDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(SignersDocumentService.class);

    static final String VALID_FILE_PATTERNS = ".+\\.(?i)(pdf|txt|html|xml)";

    // Use as root for loading documents if none is found locally
    static final String DEFAULT_DOCUMENT_ROOT = "classpath:/documents/";

    private final SigningConfigurationProperties signingConfigurationProperties;
    private final ResourceLoader resourceLoader;

    /**
     * Constructor
     **/
    public SignersDocumentService(
        SigningConfigurationProperties signingConfigurationProperties,
        ResourceLoader resourceLoader) {
        this.signingConfigurationProperties = signingConfigurationProperties;
        this.resourceLoader = resourceLoader;
    }


    /**
     * Returns if the Signer's Documents folder is defined
     *
     * @return if the Signer's Documents folder is defined
     */
    public boolean sdFolderDefined() {
        String sdFolder = signingConfigurationProperties.getSdFolder();
        return resourceLoader.getResource(sdFolder).exists();
    }


    /**
     * Creates and returns the Signer's Documents folder
     *
     * @return the Signer's Documents folder
     */
    public Path checkCreateSdFolder() throws IOException {
        String sdFolder = signingConfigurationProperties.getSdFolder();
        try {
            File folder = resourceLoader.getResource(sdFolder).getFile();
            if (!folder.exists() && !folder.mkdirs()) {
                throw new IOException("Cannot create Signer's Document folder " + sdFolder);
            }
            return folder.toPath();
        } catch (IOException e) {
            throw new IOException("Error creating Signer's Document folder " + sdFolder, e);
        }
    }


    /**
     * Returns a stream of documents matching the given pattern.
     * If the "documents" folder is found in the local file system, this is used.
     * Otherwise, the default set of files in the class path is used.
     *
     * @param pattern the pattern to match.
     * @param regex   whether to use regex or exact matching
     * @return the matching documents
     */
    Stream<Resource> resources(String pattern, boolean regex) throws IOException {
        // Check whether to use local file system or classpath
        String root = sdFolderDefined() ? signingConfigurationProperties.getSdFolder() : DEFAULT_DOCUMENT_ROOT;

        return Stream.of(ResourcePatternUtils
            .getResourcePatternResolver(resourceLoader)
            .getResources(root + "*"))
            .filter(r -> Objects.nonNull(r.getFilename()))
            .filter(r -> regex ? r.getFilename().matches(pattern) : r.getFilename().equals(pattern));
    }


    /**
     * Returns the {@link Resource} for a specific SD document
     *
     * @param name the name of the document
     * @return the {@link Resource} for a specific SD document
     */
    public Resource resource(String name) throws IOException {
        return resources(name, false)
            .findFirst()
            .orElseThrow(() -> new FileNotFoundException("Document " + name + " not found"));
    }


    /**
     * Returns the list of {@link SignersDocument} matching the file pattern
     *
     * @param pattern the pattern to match.
     * @param regex   whether to use regex or exact matching
     * @return the list of {@link SignersDocument} matching the file pattern
     */
    private Stream<SignersDocument> documents(String pattern, boolean regex) throws IOException {
        return resources(pattern, regex)
            .filter(Objects::nonNull)
            .map(this::resourceToSignersDocument)
            .filter(Objects::nonNull);
    }


    /**
     * Returns the list of {@link SignersDocument}
     *
     * @return the list of {@link SignersDocument}
     */
    public List<SignersDocument> documents() throws IOException {
        return documents(VALID_FILE_PATTERNS, true)
            .collect(Collectors.toList());
    }


    /**
     * Returns the {@link SignersDocument} with the given file name
     *
     * @param name the name of the document
     * @return the {@link SignersDocument} for a specific SD document
     */
    public SignersDocument document(String name) throws IOException {
        return documents(name, false)
            .findFirst()
            .orElseThrow(() -> new FileNotFoundException("Document " + name + " not found"));
    }


    /**
     * Loads a {@link SignersDocument} for the given resource
     *
     * @param resource the resource to load
     * @return a {@link SignersDocument} for the given resource
     */
    private SignersDocument resourceToSignersDocument(Resource resource) {
        try {
            // Instantiate the Signer's Document based on file extension
            String fileName = Objects.requireNonNull(resource.getFilename());
            SignersDocumentFile sdf = SignersDocumentFile.builder()
                .setName(fileName)
                .setUrl(resource.getURL())
                .build();
            SignProperties props = getProperties(fileName);
            SignersDocument sd;
            switch (SigningUtils.fileExtension(fileName).toLowerCase()) {
                case "pdf":
                    sd = new PdfSignersDocument(sdf, props);
                    break;
                case "txt":
                    sd = new PlainTextSignersDocument(sdf, useMonoSpaceFont(fileName), props);
                    break;
                case "html":
                    sd = new HtmlSignersDocument(sdf, props);
                    break;
                case "xml":
                    sd = new XmlSignersDocument(sdf, getXslt(fileName), props);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Signer's Document type: " + fileName);
            }
            return sd;
        } catch (NemLogInException | IOException e) {
            LOG.error("error accessing resource {}", resource, e);
            return null;
        }
    }


    /**
     * For XML files, there must be a corresponding XSLT file with the same file name but
     * or ".xslt" file extension
     *
     * @param fileName the XML file name
     * @return the associated XSLT
     */
    private SignersDocumentFile getXslt(String fileName) throws IOException, NemLogInException {

        String xsltFileName = SigningUtils.fileName(fileName, "xsl");
        Resource xslt = resource(xsltFileName);
        return SignersDocumentFile.builder()
            .setName(xsltFileName)
            .setUrl(xslt.getURL())
            .build();
    }


    /**
     * Check if there is a corresponding ".properties" file, which will be loaded as sign properties
     *
     * @param fileName the XML file name
     * @return the {@link SignProperties} or null, if undefined
     */
    private SignProperties getProperties(String fileName) {

        try {
            Resource propFile = resource(SigningUtils.fileName(fileName, "properties"));
            Properties properties = new Properties();
            try (InputStream in = propFile.getInputStream()) {
                properties.load(in);
                SignProperties signProperties = new SignProperties();
                properties.stringPropertyNames().forEach(
                    key -> signProperties.put(key, new StringValue(properties.getProperty(key)))
                );
                return signProperties;
            }
        } catch (IOException e) {
            LOG.trace("unable to read property in file : {}", fileName, e);
            return null;
        }
    }


    /**
     * For plain text files, check if the file name contains "-monospace",
     * which will cause the useMonoSpaceFont flag to be set for the document.
     *
     * @param fileName the XML file name
     * @return if monospace fonts should be used
     */
    private boolean useMonoSpaceFont(String fileName) {
        return fileName.toLowerCase().contains("-monospace");
    }

}
