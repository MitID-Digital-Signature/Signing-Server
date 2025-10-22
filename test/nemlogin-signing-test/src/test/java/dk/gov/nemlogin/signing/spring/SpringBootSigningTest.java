package dk.gov.nemlogin.signing.spring;

import dk.gov.nemlogin.signing.service.SigningPayloadService;
import dk.gov.nemlogin.signing.spring.NemLogInAutoConfiguration;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test that Spring Boot integration works, courtesy of the nemlogin-signing-spring-boot wrapper project.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NemLogInAutoConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringBootSigningTest {

    @Autowired(required = false)
    SignatureKeys signatureKeys;

    @Autowired(required = false)
    @Qualifier("signingClientUrl")
    String signingClientUrl;

    @Autowired(required = false)
    SigningPayloadService signingPayloadService;


    /**
     * Test that the Spring autowiring works properly
     */
    @Test
    void testSpringConfiguration() {
        Assertions.assertNotNull(signatureKeys);
        Assertions.assertNotNull(signatureKeys.getPrivateKey());
        Assertions.assertFalse(signatureKeys.getCertificateChain().isEmpty());
        Assertions.assertEquals("https://undef.com", signingClientUrl);
        Assertions.assertNotNull(signingPayloadService);
    }
}
