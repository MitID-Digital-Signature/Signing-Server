package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.model.ValidTransformation;

import java.io.Serializable;
import java.util.Properties;

/**
 * Encapsulates the data involved in transforming
 * an SD (Signer's Document) to a DTBS (Data To Be Signed) document.
 */
@SuppressWarnings("unused")
public class TransformationContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Signature keys used to JWS-sign the SigningPayload **/
    private final SignatureKeys signatureKeys;

    /** Signature parameters - the dtbsDigest and dtbsSignedInfo field will be updated during the transformation **/
    private SignatureParameters signatureParameters;

    /** Input SD **/
    private final SignersDocument signersDocument;

    /** Output DTBS **/
    private DataToBeSigned dataToBeSigned;

    /** Transformation properties - may be used by customize behaviour of transformation services **/
    private final Properties transformationProperties = new Properties();


    /**
     * Designated Constructor
     * @param signersDocument the SD to transform
     * @param signatureKeys the signature keys
     * @param signatureParameters the signature parameters
     * @param transformationProperties optionally specify transformation properties
     */
    public TransformationContext(SignersDocument signersDocument, SignatureKeys signatureKeys, SignatureParameters signatureParameters, Properties transformationProperties) throws ValidationException {
        if(signersDocument == null) {
            throw new ValidationException(ErrorCode.SDK010, null, "Signer's document is null");
        } else if(signersDocument.getFile() == null) {
            throw new ValidationException(ErrorCode.SDK010, null, "File containing Signer's document is null");
        }

        this.signersDocument = signersDocument;
        this.signatureKeys = signatureKeys;
        this.signatureParameters = signatureParameters;
        if (transformationProperties != null && !transformationProperties.isEmpty()) {
            this.transformationProperties.putAll(transformationProperties);
        }
    }


    /**
     * Constructor
     * @param signersDocument the SD to transform
     * @param signatureKeys the signature keys
     * @param signatureParameters the signature parameters
     */
    public TransformationContext(SignersDocument signersDocument, SignatureKeys signatureKeys, SignatureParameters signatureParameters) throws ValidationException {
        this(signersDocument, signatureKeys, signatureParameters, null);
    }


    /**
     * Updates the signature parameters with the given dtbsDigest
     * @param dtbsDigest the DTBS digest to set
     */
    public void updateDtbsDigest(String dtbsDigest) {
        this.signatureParameters = this.signatureParameters.withDtbsDigest(dtbsDigest);
    }


    /**
     * Updates the signature parameters with the given dtbsSignedInfo
     * @param dtbsSignedInfo the DTBS Signed Info structure to set
     */
    public void updateDtbsSignedInfo(String dtbsSignedInfo) {
        this.signatureParameters = this.signatureParameters.withDtbsSignedInfo(dtbsSignedInfo);
    }


    /**
     * Returns the transformation defined by the signature parameters
     * @return the transformation defined by the signature parameters
     */
    public ValidTransformation getTransformation() {
        return ValidTransformation.transformationFor(
            signatureParameters.getDocumentFormat(),
            signatureParameters.getSignatureFormat());
    }


    public SignatureKeys getSignatureKeys() {
        return signatureKeys;
    }

    public SignatureParameters getSignatureParameters() {
        return signatureParameters;
    }

    public SignersDocument getSignersDocument() {
        return signersDocument;
    }

    public DataToBeSigned getDataToBeSigned() {
        return dataToBeSigned;
    }

    public void setDataToBeSigned(DataToBeSigned dataToBeSigned) {
        this.dataToBeSigned = dataToBeSigned;
    }

    public Properties getTransformationProperties() {
        return transformationProperties;
    }
}
