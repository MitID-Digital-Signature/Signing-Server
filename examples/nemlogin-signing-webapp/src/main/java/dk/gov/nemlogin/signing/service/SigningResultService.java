package dk.gov.nemlogin.signing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gov.nemlogin.signing.client.SigningClientError;
import dk.gov.nemlogin.signing.config.SigningConfigurationProperties;
import dk.gov.nemlogin.signing.util.SigningUtils;
import dk.gov.nemlogin.signing.validation.model.SignatureValidationContext;
import dk.gov.nemlogin.signing.validation.model.ValidationReport;
import dk.gov.nemlogin.signing.validation.service.SignatureValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Used for handling the signed document
 */
@Service
public class SigningResultService {

    private static final Logger LOG = LoggerFactory.getLogger(SigningResultService.class);

    private final SignatureValidationService signatureValidationService;
    private final SignatureValidationContext.Builder signatureValidationContextBuilder;
    private final SigningConfigurationProperties signingConfigurationProperties;


    /** Constructor **/
    public SigningResultService(
        SignatureValidationService signatureValidationService,
        SignatureValidationContext.Builder signatureValidationContextBuilder,
        SigningConfigurationProperties signingConfigurationProperties
    ) {
        this.signatureValidationService = signatureValidationService;
        this.signatureValidationContextBuilder = signatureValidationContextBuilder;
        this.signingConfigurationProperties = signingConfigurationProperties;
    }


    /**
     * Check if the result should be saved to a local file
     * @param name the document name
     * @param format the signed document format - either "xml" (XAdES-B-LTA) or "pdf" (PAdES-B-LTA)
     * @param document the Base64-encoded document
     */
    public void checkSaveResult(String name, String format, String document) {
        String saveSignedDocumentToFolder = signingConfigurationProperties.getSaveSignedDocumentToFolder();
        if (SigningUtils.isNotEmpty(saveSignedDocumentToFolder)) {
            Path filePath = Paths.get(saveSignedDocumentToFolder).resolve(signedDocumentFilename(name, format));
            byte[] data = Base64.getDecoder().decode(document);
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(data);
                LOG.info("Wrote signed document to: {}", filePath);
            } catch (Exception e) {
                LOG.info("Error writing signed document to: {}", filePath, e);
		LOG.info("Path is: {}", System.getProperty("user.dir"));
            }
        }
    }


    /**
     * Validates the signed document
     * @param signedDocumentFilename the signed document name
     * @param document the Base64-encoded document
     * @return the validation report
     */
    public ValidationReport validateSignedDocument(String signedDocumentFilename, String document) throws IOException {

        SignatureValidationContext ctx = signatureValidationContextBuilder.copy()
            .setDocumentName(signedDocumentFilename)
            .setDocumentData(Base64.getDecoder().decode(document))
            .build();
        return signatureValidationService.validate(ctx);
    }


    /**
     * Parses the Base64-encoded error received as a string from the Signing Client
     * @param errorStr the Base64-encoded error to parse
     * @return the parsed error
     */
    public SigningClientError parseError(String errorStr) {
        String error = new String(Base64.getDecoder().decode(errorStr), StandardCharsets.UTF_8);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(error, SigningClientError.class);
        } catch (JsonProcessingException e) {
            LOG.error("error parsing '{}'", errorStr, e);
            SigningClientError result = new SigningClientError();
            result.setMessage(error);
            return result;
        }
    }


    /**
     * Adds "-signed" to the file name along with the proper file extension of the signed document.
     * Example: "doc1.html" -> "doc1-signed.pdf" when signed as a PAdES-B-LTA
     * @param name the original Signer's Document file name
     * @param format the signed document format - either "xml" (XAdES-B-LTA) or "pdf" (PAdES-B-LTA)
     * @return the signed document file name
     */
    public String signedDocumentFilename(String name, String format) {
        int index = name.lastIndexOf(".");
        if (index != -1) {
            return name.substring(0, index) + "-signed." + format;
        }
        return name + "-signed." + format;
    }
}
