package dk.gov.nemlogin.signing;

import dk.gov.nemlogin.signing.util.SigningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * Launches the example NemLog-In signing application
 */
@SpringBootApplication
public class NemLogInSigningApplication {

    private static final Logger LOG = LoggerFactory.getLogger(NemLogInSigningApplication.class);

    static {
        SigningUtils.addBouncyCastleProvider();
    }

    @PostConstruct
    public void startupApplication() {
        LOG.info("Started NemLog-In Example Web App (version {})",
            SigningUtils.versionInfo().get("git.build.version"));
    }

    public static void main(String[] args) {
        SpringApplication.run(NemLogInSigningApplication.class, args);
    }
}
