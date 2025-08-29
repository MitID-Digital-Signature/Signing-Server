package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.config.SigningConfigurationProperties;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import org.springframework.stereotype.Service;

/**
 * ServiceFacade contains a list of services utilized by Services.
 */
@Service
public class ServiceFacade {

    SigningPayloadService signingPayloadService;
    SignersDocumentService signersDocumentService;
    TransformationPropertiesService transformationProperties;
    SignatureKeys signatureKeys;
    SigningConfigurationProperties signingConfigurationProperties;

    /** Constructor **/
    public ServiceFacade(SigningPayloadService signingPayloadService, SignersDocumentService signersDocumentService, TransformationPropertiesService transformationProperties, SignatureKeys signatureKeys, SigningConfigurationProperties signingConfigurationProperties) {
        this.signingPayloadService = signingPayloadService;
        this.signersDocumentService = signersDocumentService;
        this.transformationProperties = transformationProperties;
        this.signatureKeys = signatureKeys;
        this.signingConfigurationProperties = signingConfigurationProperties;
    }
}
