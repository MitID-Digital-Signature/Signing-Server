package dk.gov.nemlogin.signing.broker.service;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used for loading SD's (Signer's Documents) from "documents" classpath folder.
 */
@Component
public class SignersDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(SignersDocumentService.class);

    static final String VALID_FILE_PATTERNS = ".+\\.(?i)(pdf|txt|html|xml)";
    static final String DEFAULT_DOCUMENT_ROOT = "classpath:/documents/";

    private ResourceLoader resourceLoader;

    /** Constructor **/
    public SignersDocumentService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Returns a stream of documents matching the given pattern.
     * If the "documents" folder is found in the local file system, this is used.
     * Otherwise, the default set of files in the class path is used.
     *
     * @param pattern the pattern to match.
     * @param regex whether to use regex or exact matching
     * @return the matching documents
     */
    Stream<Resource> resources(String pattern, boolean regex) throws IOException {
        return Stream.of(ResourcePatternUtils
            .getResourcePatternResolver(resourceLoader)
            .getResources(DEFAULT_DOCUMENT_ROOT + "*"))
            .filter(r -> Objects.nonNull(r.getFilename()))
            .filter(r -> regex ? r.getFilename().matches(pattern) : r.getFilename().equals(pattern));
    }


    /**
     * Returns the {@link Resource} for a specific SD document
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
     * @param regex whether to use regex or exact matching
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
            SignersDocument sd;
            switch (SigningUtils.fileExtension(fileName).toLowerCase()) {
                case "pdf":
                    sd = new PdfSignersDocument(sdf);
                    break;
                case "txt":
                    sd = new PlainTextSignersDocument(sdf, false);
                    break;
                case "html":
                    sd = new HtmlSignersDocument(sdf);
                    break;
                case "xml":
                    sd = new XmlSignersDocument(sdf, getXslt(fileName));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Signer's Document type: " + fileName);
            }
            return sd;
        } catch (IOException | NemLogInException e) {
            LOG.error("error in resourceToSignersDocument", e);
            return null;
        }
    }


    /**
     * For XML files, there must be a corresponding XSLT file with the same file name but
     * ".xslt" file extension
     * @param fileName the XML file name
     * @return the associated XSLT
     */
    private SignersDocumentFile getXslt(String fileName) throws IOException {

        String xsltFileName = SigningUtils.fileName(fileName,"xsl");
        Resource xslt = resource(xsltFileName);
        try {
            return SignersDocumentFile.builder()
                .setName(xsltFileName)
                .setUrl(xslt.getURL())
                .build();
        } catch (NemLogInException e) {
            throw new IOException(e);
        }
    }
}
