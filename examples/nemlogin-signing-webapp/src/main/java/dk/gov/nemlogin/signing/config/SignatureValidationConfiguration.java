package dk.gov.nemlogin.signing.config;

import dk.gov.nemlogin.signing.validation.model.SignatureValidationContext;
import dk.gov.nemlogin.signing.validation.model.SignatureValidationContext.Builder;
import dk.gov.nemlogin.signing.validation.service.SignatureValidationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the beans needed for calling the signature validation service
 */
@Configuration
public class SignatureValidationConfiguration {

    private final String validationServiceUrl;

    /** Constructor **/
    public SignatureValidationConfiguration(@Qualifier("validationServiceUrl") String validationServiceUrl) {
        this.validationServiceUrl = validationServiceUrl;
    }


    /** Defines a {@link SignatureValidationService} used for calling the NemLog-In Signature Validation API **/
    @Bean
    public SignatureValidationService signatureValidationService() {
        return new SignatureValidationService();
    }


    /** Defines a {@link Builder} used for calling the validation service **/
    @Bean
    public Builder signatureValidationContextBuilder() {
        return SignatureValidationContext.builder()
            .setValidationServiceUrl(validationServiceUrl)
            // Fine for demo app
            .setIgnoreSsl(true);
    }
}
