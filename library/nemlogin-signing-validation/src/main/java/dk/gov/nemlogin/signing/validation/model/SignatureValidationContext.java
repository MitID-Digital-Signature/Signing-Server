package dk.gov.nemlogin.signing.validation.model;

import dk.gov.nemlogin.signing.util.SigningUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

/**
 * Encapsulates the context needed for calling the {@code SignatureValidationService.validate()} method.
 */
@SuppressWarnings("unused")
public class SignatureValidationContext {

    // URL to the validation service
    private String validationServiceUrl;

    // Whether to disable SSL checks upon accessing the validation URL. Only use for development purposes...
    private boolean ignoreSsl;

    // Sets the HTTP client timeout (in milliseconds)
    private Integer timeout;

    // Name of the document to validate
    private String documentName;

    // The actual document data
    private byte[] documentData;

    // Correlation ID passed on in the "CorrelationIdManager.CorrelationId" header
    // Will be logged in the Validation Service back-end.
    private String correlationId;


    /**
     * No-access copy-constructor
     **/
    private SignatureValidationContext(SignatureValidationContext ctx) {
        if (ctx != null) {
            this.validationServiceUrl = ctx.validationServiceUrl;
            this.ignoreSsl = ctx.ignoreSsl;
            this.timeout = ctx.timeout;
            this.documentName = ctx.documentName;
            this.documentData = ctx.documentData;
            this.correlationId = ctx.correlationId;
        }
    }

    public String getValidationServiceUrl() {
        return validationServiceUrl;
    }

    public boolean isIgnoreSsl() {
        return ignoreSsl;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public String getDocumentName() {
        return documentName;
    }

    public byte[] getDocumentData() {
        return documentData != null ? documentData.clone() : new byte[0];
    }

    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Returns a new {@link SignatureValidationContext.Builder}
     **/
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Builder for the {@link SignatureValidationContext}
     */
    public static class Builder {

        SignatureValidationContext template = new SignatureValidationContext(null);

        /**
         * Creates a copy of this Builder
         *
         * @return a copy of this Builder
         */
        public Builder copy() {
            var builder = new Builder();
            builder.template = new SignatureValidationContext(template);
            return builder;
        }

        /**
         * Sets the URL to the validation service
         *
         * @param validationServiceUrl the URL to the validation service
         * @return this
         */
        public Builder setValidationServiceUrl(String validationServiceUrl) {
            template.validationServiceUrl = validationServiceUrl;
            return this;
        }

        /**
         * Sets whether to ignore SSL PKIX Certification path validation when connecting to the Validation Service
         *
         * @param ignoreSsl Sets whether to ignore SSL validation
         * @return this
         */
        public Builder setIgnoreSsl(boolean ignoreSsl) {
            template.ignoreSsl = ignoreSsl;
            return this;
        }

        /**
         * Sets the validation request timeout (in milliseconds)
         *
         * @param timeout the timeout
         * @return this
         */
        public Builder setTimeout(Integer timeout) {
            template.timeout = timeout;
            return this;
        }

        /**
         * Sets the name of the document to validate
         *
         * @param documentName the name of the document
         * @return this
         */
        public Builder setDocumentName(String documentName) {
            template.documentName = documentName;
            return this;
        }

        /**
         * Sets the data of the document to validate
         *
         * @param documentData the document data
         * @return this
         */
        public Builder setDocumentData(byte[] documentData) {
            template.documentData = documentData;
            return this;
        }

        /**
         * Sets a client-selected correlation ID that will be logged at the Validation Service back-end.
         *
         * @param correlationId the correlation ID
         * @return this
         */
        public Builder setCorrelationId(String correlationId) {
            template.correlationId = correlationId;
            return this;
        }

        /**
         * Sets the path to the document to validate.
         * If the documentName has not been set, it will be deduced from the path.
         *
         * @param path the path
         * @return this
         */
        public Builder setDocumentPath(Path path) throws IOException {
            template.documentData = SigningUtils.loadBytes(path);
            if (SigningUtils.isEmpty(template.documentName)) {
                final var fileName = path.getFileName();
                if (fileName != null) {
                    template.documentName = fileName.toString();
                }
            }
            return this;
        }

        /**
         * Sets the URL to the document to validate.
         * If the documentName has not been set, it will be deduced from the URL.
         *
         * @param url the URL
         * @return this
         */
        public Builder setDocumentUrl(URL url) throws IOException {
            template.documentData = SigningUtils.loadBytes(url);
            if (SigningUtils.isEmpty(template.documentName)) {
                template.documentName = fileName(url.getPath());
            }
            return this;
        }

        /**
         * Sets the class-path to the document to validate.
         * If the documentName has not been set, it will be deduced from the class-path.
         *
         * @param classPath the class-path
         * @return this
         */
        public Builder setDocumentClassPath(String classPath) throws IOException {
            template.documentData = SigningUtils.loadBytes(classPath);
            if (SigningUtils.isEmpty(template.documentName)) {
                template.documentName = fileName(classPath);
            }
            return this;
        }

        /**
         * Sets the InputStream to the document to validate.
         * This InputStream will not be closed.
         *
         * @param inputStream the InputStream
         * @return this
         */
        public Builder setDocumentInputStream(InputStream inputStream) throws IOException {
            template.documentData = SigningUtils.loadBytes(inputStream);
            return this;
        }


        /**
         * Returns the file name part of the path, i.e. after the last "/".
         *
         * @param path the path
         * @return the file name part of the path or null if not found
         */
        private String fileName(String path) {
            if (SigningUtils.isNotEmpty(path)) {
                String fileName = path.contains("/")
                    ? path.substring(path.lastIndexOf("/") + 1)
                    : path;
                return SigningUtils.isNotEmpty(fileName) ? fileName : null;
            }
            return null;
        }

        /**
         * Validates and builds a new {@link SignatureValidationContext}
         *
         * @return a new {@link SignatureValidationContext}
         */
        public SignatureValidationContext build() {
            if (SigningUtils.isEmpty(template.validationServiceUrl)) {
                throw new IllegalArgumentException("Missing validationServiceUrl value");
            }
            if (SigningUtils.isEmpty(template.documentName)) {
                throw new IllegalArgumentException("Missing documentName value");
            }
            if (template.documentData == null) {
                throw new IllegalArgumentException("Missing documentData value");
            }
            return new SignatureValidationContext(template);
        }
    }

}
