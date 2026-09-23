package dk.gov.nemlogin.signing.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.util.SigningUtils;
import dk.gov.nemlogin.signing.validation.model.SignatureValidationContext;
import dk.gov.nemlogin.signing.validation.model.ValidationReport;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Simple implementation of a service for validating a signed document
 * by calling the public NemLog-In Signature Validation API.
 */
public class SignatureValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(SignatureValidationService.class);
    private static final String FILE_PARAM_NAME = "file";
    private static final String CORRELATION_ID_HEADER = "CorrelationIdManager.CorrelationId";

    /**
     * Calls the NemLog-In Signature Validation API and return a {@link ValidationReport} as a result.
     *
     * @param ctx the {@link SignatureValidationContext}
     * @return the  {@link ValidationReport}
     */
    public ValidationReport validate(SignatureValidationContext ctx) throws IOException {

        long t0 = System.currentTimeMillis();

        var httpClientBuilder = HttpClients.custom();

        // Handle SSL ignorance
        if (ctx.isIgnoreSsl()) {
            SSLContext sslContext;
            try {
                sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();
            } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
                throw new IOException(e);
            }

            TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext)
                .build();

            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy((TlsSocketStrategy) tlsStrategy)
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(50)
                .build();

            httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableRedirectHandling()
                .disableConnectionState();
        }

        // Handle timeouts
        if (ctx.getTimeout() != null) {
            RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(ctx.getTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(ctx.getTimeout()))
                .build();
            httpClientBuilder.setDefaultRequestConfig(config);
        }

        // Perform the validation request
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            var httpEntity = MultipartEntityBuilder
                .create()
                .setMode(HttpMultipartMode.LEGACY)
                .addBinaryBody(FILE_PARAM_NAME, ctx.getDocumentData(), ContentType.DEFAULT_BINARY, ctx.getDocumentName())
                .build();

            var requestBuilder = ClassicRequestBuilder.post(ctx.getValidationServiceUrl())
                .setEntity(httpEntity)
                .addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            if (SigningUtils.isNotEmpty(ctx.getCorrelationId())) {
                requestBuilder.addHeader(CORRELATION_ID_HEADER, ctx.getCorrelationId());
            }
            ClassicHttpRequest request = requestBuilder.build();
            HttpClientResponseHandler<ValidationReport> responseHandler = response -> {
                int status = response.getCode();
                if (status == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    return new ObjectMapper().readValue(entity.getContent(), ValidationReport.class);
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            ValidationReport result = httpClient.execute(request, responseHandler);

            LOG.info("Completed signature validation to {}{} in {} ms",
                ctx.getValidationServiceUrl(),
                SigningUtils.isEmpty(ctx.getCorrelationId()) ? "" : String.format(" (correlationId %s)", ctx.getCorrelationId()),
                System.currentTimeMillis() - t0);

            return result;

        } catch (Exception e) {
            final var msg = String.format("Error executing signature validation to %s:%s after %s ms: %s",
                ctx.getValidationServiceUrl(),
                SigningUtils.isEmpty(ctx.getCorrelationId()) ? "" : String.format(" (correlationId %s)", ctx.getCorrelationId()),
                System.currentTimeMillis() - t0,
                e.getMessage());
            throw new IOException(new NemLogInException(ErrorCode.SDK011, msg, e));
        }
    }
}
