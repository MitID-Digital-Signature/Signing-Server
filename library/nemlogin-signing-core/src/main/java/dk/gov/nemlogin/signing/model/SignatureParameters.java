package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.exceptions.InvalidSignatureParametersException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.util.SigningUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.gov.nemlogin.signing.util.SigningUtils.allNonNull;
import static dk.gov.nemlogin.signing.util.SigningUtils.allNull;

/**
 * Defines the signature parameters to be passed on to the signing client along with the signing text.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class SignatureParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int INTERNAL_VERSION = 1;
    public static final String DTBS_DIGEST_ALGORITHM = "SHA-256";
    public static final int REFERENCE_TEXT_MAX_LENGTH = 50;

    @SuppressWarnings("FieldCanBeLocal")
    Integer version = INTERNAL_VERSION;
    FlowType flowType;
    String entityID;
    DocumentFormat documentFormat;
    SignatureFormat signatureFormat;
    String dtbsDigest;
    String dtbsDigestAlgorithm = DTBS_DIGEST_ALGORITHM;
    String dtbsSignedInfo;
    String referenceText;
    Integer minAge;
    String signerSubjectNameID;
    Language preferredLanguage;
    SsnPersistenceLevel ssnPersistenceLevel;
    Boolean anonymizeSigner;
    String acceptedCertificatePolicies;


    /** No-access constructor **/
    SignatureParameters() {
    }


    /**
     * No-access constructor
     **/
    SignatureParameters(SignatureParameters params) {
        Objects.requireNonNull(params);
        this.version = params.version;
        this.flowType = params.flowType;
        this.entityID = params.entityID;
        this.documentFormat = params.documentFormat;
        this.signatureFormat = params.signatureFormat;
        this.dtbsDigest = params.dtbsDigest;
        this.dtbsDigestAlgorithm = params.dtbsDigestAlgorithm;
        this.dtbsSignedInfo = params.dtbsSignedInfo;
        this.referenceText = params.referenceText;
        this.minAge = params.minAge;
        this.signerSubjectNameID = params.signerSubjectNameID;
        this.preferredLanguage = params.preferredLanguage;
        this.ssnPersistenceLevel = params.ssnPersistenceLevel;
        this.anonymizeSigner = params.anonymizeSigner;
        this.acceptedCertificatePolicies = params.acceptedCertificatePolicies;
    }


    /**
     * Validates the parameters and returns an immutable map representation of the signature parameters.
     * Suitable when e.g. JWS-signing the parameters.
     * @return an immutable map representation of the signature parameters
     * @throws InvalidSignatureParametersException when the parameters are invalid
     */
    public Map<String, Object> asMap() throws InvalidSignatureParametersException {

        // Validate the parameters
        validate();

        Map<String, Object> map = new HashMap<>();
        for (Field field : getClass().getDeclaredFields()) {

            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            final Entry<String, Object> stringObjectEntry = toMapEntry(field);
            if (stringObjectEntry != null) {
                map.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
            }
        }
        return map;
    }


    /** Utility function that converts a signature parameter field into a map entry **/
    private Entry<String, Object> toMapEntry(Field field) throws InvalidSignatureParametersException {
        try {
            Object value = field.get(this);
            return value != null ? new AbstractMap.SimpleEntry<>(field.getName(), value) : null;
        } catch (IllegalAccessException e) {
            throw new InvalidSignatureParametersException(this, "Error serializing signature parameter", e);
        }
    }


    /**
     * Factory method to use when building signature parameters
     * @return the builder to use for creating signature parameters
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Creates a copy of this signature parameters with the given {@link #dtbsDigest}
     * @param dtbsDigest the DTBS digest to set
     * @return a copy of this signature parameters with the given {@link #dtbsDigest}
     */
    public SignatureParameters withDtbsDigest(String dtbsDigest) {
        SignatureParameters params = new SignatureParameters(this);
        params.dtbsDigest = dtbsDigest;
        return params;
    }


    /**
     * Creates a copy of this signature parameters with the given {@link #dtbsSignedInfo}
     * @param dtbsSignedInfo the DTBS Signed Info structure to set
     * @return a copy of this signature parameters with the given {@link #dtbsSignedInfo}
     */
    public SignatureParameters withDtbsSignedInfo(String dtbsSignedInfo) {
        SignatureParameters params = new SignatureParameters(this);
        params.dtbsSignedInfo = dtbsSignedInfo;
        return params;
    }


    /**
     * Validates that the signature parameters contains a valid set of fields.
     * NB: The method does not validate the contents of the fields.
     * @throws InvalidSignatureParametersException when the parameters are invalid
     */
    public void validate() throws InvalidSignatureParametersException {

        // Check common SP and Broker flow mandatory parameters
        if (!allNonNull(version, flowType, entityID, documentFormat, signatureFormat, dtbsDigestAlgorithm, dtbsSignedInfo)) {
            throw new InvalidSignatureParametersException(this, "Missing mandatory parameters");
        }
        // SP specific mandatory parameters
        if (flowType == FlowType.ServiceProvider) {
            if(!allNonNull(referenceText, dtbsDigest)) {
                throw new InvalidSignatureParametersException(this, "Missing mandatory parameters for SP flow");
            }

            if(referenceText.length() > REFERENCE_TEXT_MAX_LENGTH) {
                throw new InvalidSignatureParametersException(this, "Reference text exceeds the maximum allowed length of " + REFERENCE_TEXT_MAX_LENGTH);
            }
        }
        // Check Broker-flow specific parameters
        if (flowType == FlowType.Broker &&
            !allNull(dtbsDigest, referenceText, minAge, preferredLanguage, ssnPersistenceLevel, signerSubjectNameID,
                     anonymizeSigner, acceptedCertificatePolicies)) {
            throw new InvalidSignatureParametersException(this, "Invalid parameters for Broker flow");
        }
        // Check that acceptedCertificatePolicies has a valid format
        parseAcceptedCertificatePolicies();
    }


    /**
     * Parses the comma-separated list of accepted certificate policies
     * @return the list of accepted certificate policies
     * @throws InvalidSignatureParametersException if unable to parse accepted policies
     */
    public AcceptedCertificatePolicy[] parseAcceptedCertificatePolicies() throws InvalidSignatureParametersException {
        if (SigningUtils.isEmpty(acceptedCertificatePolicies)) {
            return new AcceptedCertificatePolicy[0];
        }
        try {
            return Stream.of(acceptedCertificatePolicies.split("\\s*,\\s*"))
                .map(AcceptedCertificatePolicy::valueOf)
                .toArray(AcceptedCertificatePolicy[]::new);
        } catch (Exception e) {
            throw new InvalidSignatureParametersException(this, "Invalid signature parameter value", e);
        }
    }


    /** {@inheritDoc} **/
    @Override
    public String toString() {
        return "SignatureParameters{" +
            "version=" + version +
            ", flowType=" + flowType +
            ", entityID=" + entityID +
            ", documentFormat=" + documentFormat +
            ", signatureFormat=" + signatureFormat +
            ", dtbsDigest='" + dtbsDigest + '\'' +
            ", dtbsDigestAlgorithm='" + dtbsDigestAlgorithm + '\'' +
            ", dtbsSignedInfo='" + dtbsSignedInfo + '\'' +
            ", referenceText='" + referenceText + '\'' +
            ", minAge=" + minAge +
            ", signerSubjectNameID='" + signerSubjectNameID + '\'' +
            ", preferredLanguage=" + preferredLanguage +
            ", ssnPersistenceLevel=" + ssnPersistenceLevel +
            ", anonymizeSigner=" + anonymizeSigner +
            ", acceptedCertificatePolicies='" + acceptedCertificatePolicies + '\'' +
            '}';
    }


    public Integer getVersion() {
        return version;
    }

    public FlowType getFlowType() {
        return flowType;
    }

    public String getEntityID() {
        return entityID;
    }

    public DocumentFormat getDocumentFormat() {
        return documentFormat;
    }

    public SignatureFormat getSignatureFormat() {
        return signatureFormat;
    }

    public String getDtbsDigest() {
        return dtbsDigest;
    }

    public String getDtbsDigestAlgorithm() {
        return dtbsDigestAlgorithm;
    }

    public String getDtbsSignedInfo() {
        return dtbsSignedInfo;
    }

    public String getReferenceText() {
        return referenceText;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public String getSignerSubjectNameID() {
        return signerSubjectNameID;
    }

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }

    public SsnPersistenceLevel getSsnPersistenceLevel() {
        return ssnPersistenceLevel;
    }

    public Boolean getAnonymizeSigner() {
        return anonymizeSigner;
    }

    public String getAcceptedCertificatePolicies() {
        return acceptedCertificatePolicies;
    }


    /**
     * Builder class for the signature parameters.
     * <p>
     * Please note that upon building a new {@link SignatureParameters}, all mandatory parameters are checked
     * except dtbsDigest and dtbsSignedInfo (both mandatory in SP flow), as these are computed subsequently.
     */
    public static class Builder {

        private final SignatureParameters template = new SignatureParameters();


        /** No-access constructor **/
        private Builder() {
        }

        /**
         * Instantiate a new {@link SignatureParameters} based on the current state of the builder
         * @return a new {@link SignatureParameters} based on the current state of the builder
         */
        public SignatureParameters build() {
            return new SignatureParameters(template);
        }

        public Builder setVersion(Integer version) {
            template.version = version;
            return this;
        }

        public Builder setFlowType(FlowType flowType) {
            template.flowType = flowType;
            return this;
        }

        public Builder setEntityID(String entityID) {
            template.entityID = entityID;
            return this;
        }

        public Builder setDocumentFormat(DocumentFormat documentFormat) {
            template.documentFormat = documentFormat;
            return this;
        }

        public Builder setSignatureFormat(SignatureFormat signatureFormat) {
            template.signatureFormat = signatureFormat;
            return this;
        }

        public Builder setValidTransformation(ValidTransformation transformation) {
            template.documentFormat = transformation.getSdFormat();
            template.signatureFormat = transformation.getSignatureFormat();
            return this;
        }

        public Builder setDtbsDigest(String dtbsDigest) {
            template.dtbsDigest = dtbsDigest;
            return this;
        }

        public Builder setDtbsDigestAlgorithm(String dtbsDigestAlgorithm) {
            template.dtbsDigestAlgorithm = dtbsDigestAlgorithm;
            return this;
        }

        public Builder setDtbsSignedInfo(String dtbsSignedInfo) {
            template.dtbsSignedInfo = dtbsSignedInfo;
            return this;
        }

        public Builder setReferenceText(String referenceText) {
            template.referenceText = referenceText;
            return this;
        }

        public Builder setMinAge(Integer minAge) {
            template.minAge = minAge;
            return this;
        }

        public Builder setSignerSubjectNameID(String signerSubjectNameID) {
            template.signerSubjectNameID = signerSubjectNameID;
            return this;
        }

        public Builder setPreferredLanguage(Language preferredLanguage) {
            template.preferredLanguage = preferredLanguage;
            return this;
        }

        public Builder setSsnPersistenceLevel(SsnPersistenceLevel ssnPersistenceLevel) {
            template.ssnPersistenceLevel = ssnPersistenceLevel;
            return this;
        }

        public Builder setAnonymizeSigner(Boolean anonymizeSigner) {
            template.anonymizeSigner = anonymizeSigner;
            return this;
        }

        public Builder setAcceptedCertificatePolicies(String acceptedCertificatePolicies) {
            template.acceptedCertificatePolicies = acceptedCertificatePolicies;
            return this;
        }

        public Builder setAcceptedCertificatePolicies(AcceptedCertificatePolicy... acceptedCertificatePolicies) {
            template.acceptedCertificatePolicies = acceptedCertificatePolicies != null && acceptedCertificatePolicies.length > 0
                ? Stream.of(acceptedCertificatePolicies)
                   .map(Enum::toString)
                   .collect(Collectors.joining(","))
                : null;
            return this;
        }
    }
}
