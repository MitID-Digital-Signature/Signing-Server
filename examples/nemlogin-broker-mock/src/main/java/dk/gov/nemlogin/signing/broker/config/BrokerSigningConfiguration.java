package dk.gov.nemlogin.signing.broker.config;

import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Defines the broker signing app configuration
 */
@Configuration
@EnableConfigurationProperties(BrokerSigningProperties.class)
public class BrokerSigningConfiguration {

    private final BrokerSigningProperties properties;

    /** Constructor **/
    public BrokerSigningConfiguration(BrokerSigningProperties properties) {
        this.properties = properties;
    }

    public BrokerSigningProperties getProperties() {
        return properties;
    }

    /** Instantiates a {@link SigningPayloadService} as a Spring component **/
    @Bean
    public SigningPayloadService signingPayloadService() {
        return new SigningPayloadService();
    }

    /** Signature keys used for JWS-sealing signature parameters **/
    @Bean
    public SignatureKeys signatureKeys() throws IOException {
        return properties.loadSignatureKeys();
    }

    /** RestTemplate used for proxying requests to the NemLog-In Signing API **/
    @Bean
    @Qualifier("proxyRestTemplate")
    public RestTemplate proxyRestTemplate() throws IOException {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        try {
            SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();
            httpClientBuilder
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build());
            return new RestTemplate(requestFactory);
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException(e);
        }
    }

    /** URL to the Signing API **/
    @Bean
    @Qualifier("signingApiUrl")
    public String signingApiUrl() {
        return properties.getSigningApiUrl();
    }
}
