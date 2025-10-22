package dk.gov.nemlogin.signing.broker.controller;

import dk.gov.nemlogin.signing.broker.model.JsonPayload;
import dk.gov.nemlogin.signing.broker.service.BrokerIdpSamlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * API for generating a mock Broker IdP SAML Assertion
 */
@RestController
public class BrokerIdpSamlController {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerIdpSamlController.class);
    static final String HEADER_SIGNING_SESSION_ID = "X-DIGST-Signing-SessionId";

    private final BrokerIdpSamlService brokerIdpSamlService;

    /**
     * Constructor
     **/
    public BrokerIdpSamlController(BrokerIdpSamlService brokerIdpSamlService) {
        this.brokerIdpSamlService = brokerIdpSamlService;
    }

    /**
     * Creates a mock Broker IdP SAML Assertion
     **/
    @GetMapping(path = "/saml/saml-assertion")
    public JsonPayload samlAssertion(@RequestHeader(HEADER_SIGNING_SESSION_ID) String sessionId) throws IOException {
        LOG.info("/saml/saml-assertion for session {}", sessionId);
        return JsonPayload.of("samlAssertion", brokerIdpSamlService.buildAssertion(sessionId));
    }
}
