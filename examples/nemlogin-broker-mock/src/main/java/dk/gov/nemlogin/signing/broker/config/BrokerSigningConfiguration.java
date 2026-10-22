package dk.gov.nemlogin.signing.broker.config;

import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Defines the broker signing app configuration
 */
@Configuration
@EnableConfigurationProperties({BrokerSigningProperties.class, SamlProperties.class})
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
    public RestTemplate proxyRestTemplate() throws IOException {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        try {

            SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();

            TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext)
                .build();

            httpClientBuilder.setConnectionManager(
                    PoolingHttpClientConnectionManagerBuilder.create()
                        .setTlsSocketStrategy((TlsSocketStrategy) tlsStrategy)
                        .build()
                );

            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build());
            return new RestTemplate(requestFactory);
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException(e);
        }
    }

    /** URL to the Signing API **/
    @Bean
    public String signingApiUrl() {
        return properties.getSigningApiUrl();
    }
}
