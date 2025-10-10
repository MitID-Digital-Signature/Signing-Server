package dk.gov.nemlogin.signing.broker;

import dk.gov.nemlogin.signing.util.SigningUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launches the example NemLog-In Broker Mock application
 */
@SpringBootApplication
public class NemLogInBrokerMockApplication {

    static {
        SigningUtils.addBouncyCastleProvider();
    }

    public static void main(String[] args) {
        SpringApplication.run(NemLogInBrokerMockApplication.class, args);
    }
}
