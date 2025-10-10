package dk.gov.nemlogin.signing.broker.config;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Broker-Mock application MUST run under HTTPS because the Cryptomathic Signer API requires this.
 * However, just in case the Broker-Mock gets deployed in such a manner that it does not handles it's own
 * TLS offloading, then HTTP support can be added by adding the configuration:
 * <pre>
 *     server.http.port = 8080
 * </pre>
 */
@Configuration
@ConditionalOnProperty(value="server.http.port")
public class HttpServerConfig {

    private static Logger log = LoggerFactory.getLogger(HttpServerConfig.class);

    @Bean
    public ServletWebServerFactory servletContainer(@Value("${server.http.port}") int httpPort) {
        log.info("Initializing HTTP server at port {}", httpPort);

        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(httpPort);

        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addAdditionalTomcatConnectors(connector);
        return tomcat;
    }
}
