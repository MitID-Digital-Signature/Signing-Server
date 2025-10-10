package dk.gov.nemlogin.signing.broker.config;

import dk.gov.nemlogin.signing.util.SignatureKeysLoader;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Define the Signing key properties via Spring properties
 */
@ConfigurationProperties(prefix = "nemlogin.signing.broker")
public class BrokerSigningProperties extends SignatureKeysLoader {

    // Signing API url
    private String signingApiUrl;

    // Service Provider or Broker entity ID
    private String entityId;

    // SAML properties
    private SamlProperties saml;

    public String getSigningApiUrl() {
        return signingApiUrl;
    }

    public void setSigningApiUrl(String signingApiUrl) {
        this.signingApiUrl = signingApiUrl;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public SamlProperties getSaml() {
        return saml;
    }

    public void setSaml(SamlProperties saml) {
        this.saml = saml;
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
