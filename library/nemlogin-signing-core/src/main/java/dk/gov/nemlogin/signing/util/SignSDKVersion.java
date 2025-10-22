package dk.gov.nemlogin.signing.util;

public class SignSDKVersion {

    private static final String PDF_PRODUCER = "NemLog-In SP Java SDK";

    private SignSDKVersion() {
    }

    /**
     * Returns the PDF producer to include in the PDF
     * @return the PDF producer to include in the PDF
     */
    public static String getProducer() {
        String producer = PDF_PRODUCER;
        String version = SigningUtils.versionInfo().getProperty("git.build.version");

        // we might not be running on a distributed version - so we will try to parse the version from signsdk.pom
        if (SigningUtils.isNotEmpty(version)) {
            version = "developer";
        }

        if (SigningUtils.isNotEmpty(version)) {
            producer += String.format(" (version %s)", version);
        }
        return producer;
    }

}
