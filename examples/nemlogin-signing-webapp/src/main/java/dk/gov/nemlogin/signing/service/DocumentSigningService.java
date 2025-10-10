package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.config.SigningConfigurationProperties;
import dk.gov.nemlogin.signing.dto.SigningPayloadDTO;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.model.FlowType;
import dk.gov.nemlogin.signing.model.Language;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/**
 * Generates a {@link SigningPayloadDTO} for a given Signer's Document and signature format
 */
@Service
public class DocumentSigningService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentSigningService.class);

    private final SigningPayloadService signingPayloadService;
    private final SignersDocumentService signersDocumentService;
    private final TransformationPropertiesService transformationProperties;
    private final SignatureKeys signatureKeys;
    private final SigningConfigurationProperties signingConfigurationProperties;
    private final String signingClientUrl;
    private final String entityID;
    private final MessageSource messageSource;


    /**
     * Constructor
     **/
    public DocumentSigningService(
        ServiceFacade serviceFacade,
        @Qualifier("signingClientUrl") String signingClientUrl,
        @Qualifier("entityID") String entityID,
        MessageSource messageSource) {
        this.signingPayloadService = serviceFacade.signingPayloadService;
        this.signersDocumentService = serviceFacade.signersDocumentService;
        this.transformationProperties = serviceFacade.transformationProperties;
        this.signatureKeys = serviceFacade.signatureKeys;
        this.signingConfigurationProperties = serviceFacade.signingConfigurationProperties;
        this.signingClientUrl = signingClientUrl;
        this.entityID = entityID;
        this.messageSource = messageSource;
    }


    /**
     * Returns the URL to load signing client from
     **/
    public String getSigningClientUrl() {
        return signingClientUrl;
    }


    /**
     * Generates a {@link SigningPayloadDTO} payload for the file to sign
     *
     * @param signatureFormat the signature format
     * @param fileName        the file to sign
     * @return the signing payload
     */
    public SigningPayloadDTO generateSigningPayload(SignatureFormat signatureFormat, String fileName) throws IOException {

        SignersDocument sd = signersDocumentService.document(fileName);

        // Resolve language
        Locale locale = LocaleContextHolder.getLocale();

        // Resolve default reference text
        String referenceText = messageSource.getMessage(
            "reference_text",
            new Object[]{fileName},
            locale);

        // Construct the signature parameters
        SignatureParameters signatureParameters = SignatureParameters.builder()
            .setFlowType(FlowType.ServiceProvider)
            .setEntityID(entityID)
            .setPreferredLanguage(Language.valueOf(locale))
            .setReferenceText(referenceText)
            .setDocumentFormat(sd.getFormat())
            .setSignatureFormat(signatureFormat)
            .build();

        // Look for transformation properties
        Properties transformationProps = transformationProperties.getTransformationProperties(sd, signatureFormat);

        // Instantiate a transformation context
        TransformationContext ctx;
        SigningPayloadDTO result;
        try {
            ctx = new TransformationContext(sd, signatureKeys, signatureParameters, transformationProps);

            result = signingPayloadService.produceSigningPayloadDTO(ctx);
        } catch (NemLogInException e) {
            throw new IOException(e);
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Computed Signing Payload {}", result);
        }

        // Used for debugging
        String saveDtbsToFolder = signingConfigurationProperties.getSaveDtbsToFolder();
        if (SigningUtils.isNotEmpty(saveDtbsToFolder)) {
            DataToBeSigned dtbs = ctx.getDataToBeSigned();
            try (FileOutputStream fos = new FileOutputStream(saveDtbsToFolder + "/" + dtbs.getName())) {
                fos.write(ctx.getDataToBeSigned().getData());
                LOG.info("Wrote DTBS document to: {}/{}", saveDtbsToFolder, dtbs.getName());
            } catch (Exception e) {
                LOG.info("Error writing DTBS document to: {}/{}", saveDtbsToFolder, dtbs.getName(), e);
            }
        }

        return result;
    }
}
