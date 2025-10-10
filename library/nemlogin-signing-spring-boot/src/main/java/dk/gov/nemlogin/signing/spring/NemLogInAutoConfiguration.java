package dk.gov.nemlogin.signing.spring;

import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import dk.gov.nemlogin.signing.util.SignatureKeysProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Spring Configuration for the NemLog-In Signing library
 */
@Configuration
@EnableConfigurationProperties(NemLogInSigningProperties.class)
public class NemLogInAutoConfiguration {

    private final NemLogInSigningProperties nemLogInSigningProperties;


    /**
     * Constructor
     **/
    public NemLogInAutoConfiguration(NemLogInSigningProperties nemLogInSigningProperties) {
        this.nemLogInSigningProperties = nemLogInSigningProperties;
    }


    @Bean
    @ConditionalOnMissingBean
    public SignatureKeys signatureKeys() throws IOException {
        return nemLogInSigningProperties.loadSignatureKeys();
    }


    @Bean
    public SignatureKeysProducer signatureKeysProducer() {
        return new SignatureKeysProducer();
    }


    @Bean
    @Qualifier("signingClientUrl")
    public String signingClientUrl() {
        return nemLogInSigningProperties.getSigningClientUrl();
    }


    @Bean
    @Qualifier("validationServiceUrl")
    public String validationServiceUrl() {
        return nemLogInSigningProperties.getValidationServiceUrl();
    }


    @Bean
    @Qualifier("entityID")
    public String entityID() {
        return nemLogInSigningProperties.getEntityId();
    }


    @Bean
    public SigningPayloadService signingPayloadService() {
        return new SigningPayloadService();
    }
}
