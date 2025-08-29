package dk.gov.nemlogin.signing.broker.service;

import dk.gov.nemlogin.signing.broker.config.BrokerSigningConfiguration;
import dk.gov.nemlogin.signing.dto.SigningPayloadDTO;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.FlowType;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import dk.gov.nemlogin.signing.model.SignatureParameters;
import dk.gov.nemlogin.signing.model.SignersDocument;
import dk.gov.nemlogin.signing.service.SigningPayloadService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Generates a {@link SigningPayloadDTO} for a given Signer's Document and signature format
 */
@Service
public class DocumentSigningService {

    private final SigningPayloadService signingPayloadService;
    private final SignersDocumentService signersDocumentService;
    private final BrokerSigningConfiguration conf;
    private final SignatureKeys signatureKeys;


    /**
     * Constructor
     **/
    public DocumentSigningService(
        SigningPayloadService signingPayloadService,
        SignersDocumentService signersDocumentService,
        SignatureKeys signatureKeys,
        BrokerSigningConfiguration conf) {
        this.signingPayloadService = signingPayloadService;
        this.signersDocumentService = signersDocumentService;
        this.signatureKeys = signatureKeys;
        this.conf = conf;
    }


    /**
     * Generates a {@link SigningPayloadDTO} payload for the file to sign
     *
     * @param signatureFormat the signature format
     * @param fileName        the file to sign
     * @return the signing payload
     */
    public SigningPayloadDTO generateSigningPayload(SignatureFormat signatureFormat, String fileName) throws IOException {
        SignersDocument sd = signersDocumentService.document(fileName);

        // Construct the signature parameters
        SignatureParameters signatureParameters = SignatureParameters.builder()
            .setFlowType(FlowType.Broker)
            .setEntityID(conf.getProperties().getEntityId())
            .setDocumentFormat(sd.getFormat())
            .setSignatureFormat(signatureFormat)
            .build();

        try {
            // Instantiate a transformation context
            TransformationContext ctx = new TransformationContext(sd, signatureKeys, signatureParameters);

            // Create the signing payload
            return signingPayloadService.produceSigningPayloadDTO(ctx);
        } catch (NemLogInException e) {
            throw new IOException(e);
        }
    }
}
