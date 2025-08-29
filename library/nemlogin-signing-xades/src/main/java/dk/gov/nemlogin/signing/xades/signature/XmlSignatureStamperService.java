package dk.gov.nemlogin.signing.xades.signature;

import certifikat.gov.dk.nemlogin.v0_0.SignedDocumentType;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.DataToBeSigned;
import dk.gov.nemlogin.signing.service.SignatureStamperService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.xades.util.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2000._09.xmldsig_.ObjectFactory;
import org.w3._2000._09.xmldsig_.ReferenceType;
import org.w3._2000._09.xmldsig_.SignatureType;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK005;

/**
 * Updates the DTBS XML SignedDocument with a initial Signature element containing SignedInfo with a reference to and digest of the DTBS
 */
public class XmlSignatureStamperService implements SignatureStamperService {

    private static final String ALGORITHM_CANONICALIZATION = "http://www.w3.org/2001/10/xml-exc-c14n#";
    private static final String ALGORITHM_SIGNATURE = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";
    private static final String ALGORITHM_DIGEST = "http://www.w3.org/2001/04/xmlenc#sha256";
    // NS prefix (without version) and type from certifikat.gov.dk.nemlogin.v0_0.ObjectFactory
    private static final String REFERENCE_TYPE_SIGN_TEXT = "http://dk.gov.certifikat/nemlogin#SignText";

    private static final Logger LOG = LoggerFactory.getLogger(XmlSignatureStamperService.class);

    private static final ObjectFactory objectFactory = new ObjectFactory();

    @Override
    public boolean supports(final SignatureFormat signatureFormat) {
        return SignatureFormat.XAdES == signatureFormat;
    }

    @Override
    public void preSign(final TransformationContext ctx) throws TransformationException {

        try {
            long t0 = System.currentTimeMillis();
            LOG.debug("Start pre-signing XML");
            SignedDocumentType signedDocument = XmlUtils.unmarshal(ctx.getDataToBeSigned().getData());

            // Add the initial XML Signature
            signedDocument.withSignature(createInitialSignature(signedDocument));

            // Update the DTBS XML document
            ctx.setDataToBeSigned(new DataToBeSigned.XadesDataToBeSigned(XmlUtils.marshal(signedDocument), ctx.getDataToBeSigned().getName()));

            // Update the signature parameters with the SignedInfo element
            final var signatureEncoded = Base64.getEncoder().encodeToString(XmlUtils.marshal(signedDocument.getSignature()).getBytes());
            ctx.updateDtbsSignedInfo(signatureEncoded);

            LOG.info("Pre-signed XML in {} ms", System.currentTimeMillis() - t0);
            if (LOG.isDebugEnabled()) {
                LOG.info("XML signature: {}", XmlUtils.marshal(signedDocument.getSignature()));
            }
        } catch (Exception e) {
            throw new TransformationException(SDK005, ctx, "Error pre-signing XML", e);
        }
    }

    private SignatureType createInitialSignature(final SignedDocumentType signedDocument) throws JAXBException, IOException {
        // Calculate digest of SignText
        final byte[] digest = XmlUtils.sha256(XmlUtils.canonicalize(XmlUtils.marshal(signedDocument.getSignText())));
        final String signatureId = "id-" + UUID.randomUUID().toString();
        return objectFactory.createSignatureType()
            .withId(signatureId)
            .withSignedInfo(objectFactory.createSignedInfoType()
                .withCanonicalizationMethod(objectFactory.createCanonicalizationMethodType().withAlgorithm(ALGORITHM_CANONICALIZATION))
                .withSignatureMethod(objectFactory.createSignatureMethodType().withAlgorithm(ALGORITHM_SIGNATURE))
                .withReference(createSignTextReference(signatureId, signedDocument.getSignText().getId(), digest))
            );
    }

    /**
     * Creates an reference to an element in the SignedDocument
     *
     * The reference contains one transform with the required canonicalization algorithm
     * @param signatureId of the signature element
     * @param signTextId id of the SignText element
     * @param digestValue of the referenced element
     * @return the reference
     */
    private ReferenceType createSignTextReference(final String signatureId, final String signTextId, final byte[] digestValue) {
        ReferenceType reference = objectFactory.createReferenceType()
            .withId("r-" + signatureId + "-1")
            .withURI("#" + signTextId)
            .withType(REFERENCE_TYPE_SIGN_TEXT)
            .withDigestMethod(objectFactory.createDigestMethodType().withAlgorithm(ALGORITHM_DIGEST))
            .withDigestValue(digestValue);

        // Add Canonicalization algorithm
        final var transformsType = objectFactory.createTransformsType();
        transformsType.withTransform(objectFactory.createTransformType().withAlgorithm(ALGORITHM_CANONICALIZATION));
        return reference.withTransforms(transformsType);
    }
}
