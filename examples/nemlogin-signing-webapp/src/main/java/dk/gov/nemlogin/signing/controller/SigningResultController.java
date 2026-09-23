package dk.gov.nemlogin.signing.controller;

import dk.gov.nemlogin.signing.client.DetailedSigningClientError;
import dk.gov.nemlogin.signing.client.SigningClientError;
import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.SignedDocumentIntegrityException;
import dk.gov.nemlogin.signing.model.IntegrityValues;
import dk.gov.nemlogin.signing.service.ServiceLoaderFactory;
import dk.gov.nemlogin.signing.service.SigningResultService;
import dk.gov.nemlogin.signing.validation.model.ValidationReport;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.UUID;

/**
 * Handles the result received from the Signing Client via the sign.html page
 */
@Controller
@SuppressWarnings({"SpringMVCViewInspection"})
public class SigningResultController {

    private static final Logger LOG = LoggerFactory.getLogger(SigningResultController.class);

    private static final String RESULT_MEDIA_TYPE = "mediaType";
    private static final String RESULT_FORMAT = "format";
    private static final String RESULT_SIGNED_DOCUMENT_FILENAME = "signedDocumentFilename";
    private static final String RESULT_NAME = "name";
    private static final String RESULT_TYPE_DOCUMENT_SIGNED = "signedDocument";
    private static final String RESULT_CORRELATION_ID="correlationId";
    private static final String RESULT_NEW_CORRELATION_ID="newCorrelationId";

    public static final String RESULT_TYPE_ERROR = "errorResponse";
    public static final String RESULT_TYPE_CANCEL = "cancelSign";

    private final SigningResultService signingResultService;
    private final ObjectMapper mapper = new ObjectMapper();
    /**
     * Constructor
     **/
    public SigningResultController(SigningResultService signingResultService) {
        this.signingResultService = signingResultService;
    }


    /**
     * Handle result posted back from the sign.html page.
     * The result may either be the signed document or an error
     */
    @PostMapping("/signing-result")
    public String signingResult(
        @RequestParam String type,
        @RequestParam String name,
        @RequestParam String format,
        @RequestParam String result,
        @RequestParam String correlationId,
        Model model,
        HttpSession session) throws NemLogInException {

        LOG.info("Received signing document result: {} for document: {}", type, name);
        String signedDocumentFilename = signingResultService.signedDocumentFilename(name, format);
        model.addAttribute(RESULT_NAME, name);
        model.addAttribute(RESULT_SIGNED_DOCUMENT_FILENAME, signedDocumentFilename);
        model.addAttribute(RESULT_FORMAT, format);
        model.addAttribute(RESULT_CORRELATION_ID, correlationId);

        if (RESULT_TYPE_DOCUMENT_SIGNED.equals(type)) {

            signingResultService.checkSaveResult(name, format, result);

            model.addAttribute(RESULT_MEDIA_TYPE, mediaType(format));
            model.addAttribute(RESULT_TYPE_DOCUMENT_SIGNED, result);
            model.addAttribute(RESULT_NEW_CORRELATION_ID, UUID.randomUUID().toString());

            try {
                IntegrityValues iv = mapper.readValue((String)session.getAttribute("iv"), IntegrityValues.class);
                ServiceLoaderFactory.getDocumentIntegrityService(iv.getSignatureFormat()).verifySignedDocument(Base64.getDecoder().decode(result), iv);
            } catch(Exception e) {
                throw new NemLogInException(ErrorCode.SDK013, e);
            }

            return "sign-complete";

        } else if (RESULT_TYPE_CANCEL.equals(type)) {

            return "sign-cancel";

        } else if (RESULT_TYPE_ERROR.equals(type)) {

            SigningClientError error = signingResultService.parseError(result);
            if (error.getDetails().get(0).getErrorCode().equals("SAML046")) {
                var extraInfo = new DetailedSigningClientError();
                extraInfo.setErrorCode("Info");
                extraInfo.setErrorMessage("SAML046 Happens when time between identifying yourself and pressing sign takes too logn");
                error.getDetails().add(extraInfo);
            }
            model.addAttribute("error", error);
            return "sign-error";

        } else {
            throw new IllegalArgumentException("Invalid signing result type: " + type);
        }
    }


    /**
     * Handle result posted back from the sign-complete.html page.
     * The result should always be a valid signed document.
     * <p>
     * The document will be validated, and the result again displayed at the sign-complete.html page.
     */
    @PostMapping("/validate-signing-result")
    public String validateSigningResult(
        @RequestParam String name,
        @RequestParam String format,
        @RequestParam String result,
        Model model) {

        LOG.info("Validating signed document: {}", name);
        String signedDocumentFilename = signingResultService.signedDocumentFilename(name, format);

        model.addAttribute(RESULT_NAME, name);
        model.addAttribute(RESULT_SIGNED_DOCUMENT_FILENAME, signedDocumentFilename);
        model.addAttribute(RESULT_FORMAT, format);
        model.addAttribute(RESULT_MEDIA_TYPE, mediaType(format));
        model.addAttribute(RESULT_TYPE_DOCUMENT_SIGNED, result);

        try {
            ValidationReport validationReport = signingResultService.validateSignedDocument(signedDocumentFilename, result);
            model.addAttribute("validationReport", validationReport);
        } catch (Exception e) {
            LOG.error("Error validating signed document; {}", e.getMessage(), e);
        }

        return "sign-complete";
    }


    /**
     * Returns the media type to use for the given format
     **/
    private String mediaType(String format) {
        return "xml".equalsIgnoreCase(format)
            ? MediaType.TEXT_XML_VALUE
            : MediaType.APPLICATION_PDF_VALUE;
    }
}
