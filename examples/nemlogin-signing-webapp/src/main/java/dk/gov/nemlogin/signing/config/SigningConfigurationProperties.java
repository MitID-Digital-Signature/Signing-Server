package dk.gov.nemlogin.signing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Defines a few properties that may be used to control the behaviour of example web app
 */
@Configuration
@ConfigurationProperties(prefix = "nemlogin.signing.webapp")
public class SigningConfigurationProperties {

    /**
     * The folder to place Signer's Documents in.<br>
     * If no files are present in this folder, the example web application will display
     * the files of the "classpath:/documents/" path instead.
     */
    private String sdFolder = "file:./signers-documents/";

    /**
     * If defined, the generated DTBS (Data To Be Signed) will be persisted to this folder.
     */
    private String saveDtbsToFolder;

    /**
     * If defined, the generated signed document will be persisted to this folder.
     */
    private String saveSignedDocumentToFolder = "signed-documents";


    public String getSdFolder() {
        return sdFolder;
    }

    public void setSdFolder(String sdFolder) {
        this.sdFolder = sdFolder;
    }

    public String getSaveDtbsToFolder() {
        return saveDtbsToFolder;
    }

    public void setSaveDtbsToFolder(String saveDtbsToFolder) {
        this.saveDtbsToFolder = saveDtbsToFolder;
    }

    public String getSaveSignedDocumentToFolder() {
        return saveSignedDocumentToFolder;
    }

    public void setSaveSignedDocumentToFolder(String saveSignedDocumentToFolder) {
        this.saveSignedDocumentToFolder = saveSignedDocumentToFolder;
    }
}
