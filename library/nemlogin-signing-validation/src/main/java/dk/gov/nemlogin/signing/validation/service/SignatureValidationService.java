package dk.gov.nemlogin.signing.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.util.SigningUtils;
import dk.gov.nemlogin.signing.validation.model.SignatureValidationContext;
import dk.gov.nemlogin.signing.validation.model.ValidationReport;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
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
            httpClientBuilder
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        // Handle timeouts
        if (ctx.getTimeout() != null) {
            RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(ctx.getTimeout())
                .setConnectionRequestTimeout(ctx.getTimeout())
                .setSocketTimeout(ctx.getTimeout())
                .build();
            httpClientBuilder.setDefaultRequestConfig(config);
        }

        // Perform the validation request
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            var httpEntity = MultipartEntityBuilder
                .create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody(FILE_PARAM_NAME, ctx.getDocumentData(), ContentType.DEFAULT_BINARY, ctx.getDocumentName())
                .build();
            var requestBuilder = RequestBuilder.post(ctx.getValidationServiceUrl())
                .setEntity(httpEntity)
                .addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            if (SigningUtils.isNotEmpty(ctx.getCorrelationId())) {
                requestBuilder.addHeader(CORRELATION_ID_HEADER, ctx.getCorrelationId());
            }
            HttpUriRequest request = requestBuilder.build();
            ResponseHandler<ValidationReport> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
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
