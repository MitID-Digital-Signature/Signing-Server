package dk.gov.nemlogin.signing.broker.controller;

import dk.gov.nemlogin.signing.broker.model.JsonPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * Proxies requests to:
 * <ul>
 *     <li>NemLog-In Signing API</li>
 *     <li>Cryptomathic Signer SDK - API</li>
 *     <li>Cryptomathic Signer SDK - JavaScript</li>
 * </ul>
 * The NemLog-In Signing API is presently called directly from the Broker Mock Signing Client (sign.html).<br>
 * If the Broker wishes to proxy the Signing API as well, update signing-api.js to call this Controller.
 */
@RestController
public class SigningApiProxyController extends HttpServlet {

    // HttpServlet is Serializable
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SigningApiProxyController.class);

    static final String HEADER_CORRELATION_ID = "CorrelationIdManager.CorrelationId";
    static final Set<String> RESPONSE_HEADERS_TO_REMOVE = Stream
        .of(HttpHeaders.TRANSFER_ENCODING, HttpHeaders.VARY, HEADER_CORRELATION_ID, "Strict-Transport-Security")
        .collect(Collectors.toSet());

    private final String signingApiUrl;
    private final transient RestTemplate proxyRestTemplate;
    private final String host;


    /**
     * Constructor
     **/
    public SigningApiProxyController(
        @Qualifier("signingApiUrl") String signingApiUrl,
        @Qualifier("proxyRestTemplate") RestTemplate proxyRestTemplate) throws MalformedURLException {
        this.proxyRestTemplate = proxyRestTemplate;
        this.signingApiUrl = signingApiUrl;
        this.host = new URL(signingApiUrl).getHost();
    }

    /**
     * Proxy Signing API -> begin-signature-flow
     **/
    @PostMapping(path = "/signing/begin-signature-flow")
    public ResponseEntity<JsonPayload> proxyBeginSignatureFlow(HttpServletRequest request) throws IOException {
        return proxyRequest(request, JsonPayload.class);
    }

    /**
     * Proxy Signing API -> issue-certificate
     **/
    @PostMapping(path = "/signing/issue-certificate")
    public ResponseEntity<JsonPayload> proxyIssueCertificate(HttpServletRequest request) throws IOException {
        return proxyRequest(request, JsonPayload.class);
    }

    /**
     * Proxy Signing API -> create-xades-ltv
     **/
    @PostMapping(path = "/signing/create-xades-ltv")
    public ResponseEntity<JsonPayload> proxyCreateXadesLtv(HttpServletRequest request) throws IOException {
        return proxyRequest(request, JsonPayload.class);
    }

    /**
     * Proxy Signing API -> create-xades-lta
     **/
    @PostMapping(path = "/signing/create-xades-lta")
    public ResponseEntity<JsonPayload> proxyCreateXadesLta(HttpServletRequest request) throws IOException {
        return proxyRequest(request, JsonPayload.class);
    }

    /**
     * Proxy Signing API -> create-pades-ltv
     **/
    @PostMapping(path = "/signing/create-pades-ltv")
    public ResponseEntity<JsonPayload> proxyCreatePadesLtv(HttpServletRequest request) throws IOException {
        return proxyRequest(request, JsonPayload.class);
    }

    /**
     * Proxy Signing API -> create-pades-lta
     **/
    @PostMapping(path = "/signing/create-pades-lta")
    public ResponseEntity<JsonPayload> proxyCreatePadesLta(HttpServletRequest request) throws IOException {
        return proxyRequest(request, JsonPayload.class);
    }

    /**
     * Proxy Cryptomathic Signer API (JavaScript) -> session-creation-key.js
     **/
    @GetMapping(path = "/signing/session-creation-key.js")
    public ResponseEntity<String> proxySessionCreationKey(HttpServletRequest request) throws IOException {
        return proxyRequest(request, String.class);
    }

    /**
     * Proxy Cryptomathic Signer SDK -> signer-forwarder
     **/
    @PostMapping(path = "/signer-forwarder")
    public ResponseEntity<String> proxySignerForwarder(HttpServletRequest request) throws IOException {
        String url = "/signer-forwarder?" +
            "sessionId=" + URLEncoder.encode(request.getParameter("sessionId"), UTF_8) +
            "&correlationId=" + URLEncoder.encode(request.getParameter("correlationId"), UTF_8);
        return proxyRequest(url, HttpMethod.POST, toProxyRequest(request), String.class);
    }

    /**
     * Proxies a request to the Signing API
     **/
    private <T> ResponseEntity<T> proxyRequest(HttpServletRequest request, Class<T> responseClass) throws IOException {
        return proxyRequest(request.getServletPath(), HttpMethod.resolve(request.getMethod()), toProxyRequest(request), responseClass);
    }

    /**
     * Proxies a request to the Signing API
     **/
    private <V, T> ResponseEntity<T> proxyRequest(String path, HttpMethod method, HttpEntity<V> payload, Class<T> responseClass) throws IOException {

        long t0 = System.currentTimeMillis();
        String url = String.format("%s%s", signingApiUrl, path);
        LOG.debug("Calling NemLog-In Signing Service -> {}", url);

        try {
            ResponseEntity<T> response = proxyRestTemplate
                .exchange(url, method, payload, responseClass);

            LOG.info("Called NemLog-In Signing Service -> {} in {} ms", url, System.currentTimeMillis() - t0);

            return ResponseEntity.status(response.getStatusCode())
                .headers(filterResponseHeaders(response.getHeaders()))
                .body(response.getBody());

        } catch (Exception e) {
            throw new IOException(String.format("Error calling NemLog-In Signing Service -> %s: %s", url, e.getMessage()), e);
        }
    }

    /**
     * Creates a proxy request based on content and headers of the incoming {@link HttpServletRequest}
     *
     * @param request the incoming  {@link HttpServletRequest}
     * @return the proxy request
     */
    private HttpEntity<?> toProxyRequest(HttpServletRequest request) throws IOException {
        try (InputStream in = request.getInputStream()) {
            return new HttpEntity<>(in.readAllBytes(), toHttpHeaders(request));
        }
    }

    /**
     * Avoid double certain proxied response headers to avoid duplicates.
     * <p>
     * Duplicates may be due to the effect of proxying requests from Signing Frontend to Signing Service
     *
     * @param orig the original headers
     * @return the filtered headers
     */
    public static HttpHeaders filterResponseHeaders(HttpHeaders orig) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(orig);
        headers.entrySet().removeIf(h -> RESPONSE_HEADERS_TO_REMOVE.contains(h.getKey()));
        return headers;
    }

    /**
     * Extracts the {@link HttpHeaders} from the request
     *
     * @param request the request
     * @return the headers of the request
     */
    public HttpHeaders toHttpHeaders(HttpServletRequest request) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        StreamSupport.stream(spliteratorUnknownSize(request.getHeaderNames().asIterator(), Spliterator.ORDERED), false)
            .filter(name -> !"host".equalsIgnoreCase(name))
            .forEach(name -> headers.addAll(name, Collections.list(request.getHeaders(name))));
        headers.put("Host", Collections.singletonList(host));
        return new HttpHeaders(headers);
    }
}
