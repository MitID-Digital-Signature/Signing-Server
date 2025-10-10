package dk.gov.nemlogin.signing.spring;

import dk.gov.nemlogin.signing.util.SignatureKeysLoader;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Define the Signing key properties via Spring properties
 */
@ConfigurationProperties(prefix = "nemlogin.signing")
public class NemLogInSigningProperties extends SignatureKeysLoader {

    // Signing Client URL
    private String signingClientUrl;

    // Validation Service URL
    private String validationServiceUrl;

    // Service Provider or Broker entity ID
    private String entityId;

    public String getSigningClientUrl() {
        return signingClientUrl;
    }

    public NemLogInSigningProperties setSigningClientUrl(String signingClientUrl) {
        this.signingClientUrl = signingClientUrl;
        return this;
    }

    public String getValidationServiceUrl() {
        return validationServiceUrl;
    }

    public NemLogInSigningProperties setValidationServiceUrl(String validationServiceUrl) {
        this.validationServiceUrl = validationServiceUrl;
        return this;
    }

    public String getEntityId() {
        return entityId;
    }

    public NemLogInSigningProperties setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getKeystoreClassPath() {
        return keystoreClassPath;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public String getKeyPairAlias() {
        return keyPairAlias;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getPrivateKeyPassword() {
        return privateKeyPassword;
    }
}
