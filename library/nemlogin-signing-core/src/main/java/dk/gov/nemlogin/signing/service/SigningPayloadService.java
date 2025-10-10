package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.dto.SigningPayloadDTO;
import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.TransformationException;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.model.FlowType;
import dk.gov.nemlogin.signing.model.SigningPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK006;

/**
 * Performs a full instantiation of {@link SigningPayload}.
 * <p>
 * Service Providers should call the {@link #produceSigningPayloadDTO(TransformationContext)} method
 * to produce a {@link SigningPayloadDTO} suitable for passing on to the Signing Client as a JSON object.
 * <p>
 * Brokers should call the  {@link #produceSigningPayload(TransformationContext)} method
 * to produce a signing payload for further processing. The {@link SigningPayload#getSignatureParameters}
 * field is suitable for passing on to the {@code begin-sign-flow} Signing API endpoint.
 */
public class SigningPayloadService {

    private static final Logger LOG = LoggerFactory.getLogger(SigningPayloadService.class);

    /**
     * Performs the full instantiation of a {@link SigningPayload} based on the {@code ctx}, which must contain
     * the SD (Signer's Document), signature parameters, and signature keys used for signing the
     * signature parameters.
     * <p>
     * Brokers should call this method to produce a signing payload for further processing.
     * The {@link SigningPayload#getSignatureParameters} field is suitable for passing on to the
     * {@code begin-sign-flow} Signing API endpoint.
     *
     * @param ctx the {@link TransformationContext} from which to instantiate the {@link SigningPayload}
     * @return the instantiated {@link SigningPayload}
     */
    public SigningPayload produceSigningPayload(TransformationContext ctx) throws NemLogInException {

        // Sanity check
        if(ctx == null) {
            throw new ValidationException(ErrorCode.SDK010, null, "Transformation context is null");
        }

        long t0 = System.currentTimeMillis();
        LOG.debug("Producing signed payload from context: {}", ctx);

        // Step 1: Validate SD
        ServiceLoaderFactory
            .getSignersDocumentValidationService(ctx.getSignersDocument().getFormat())
            .validate(ctx);

        // Step 2: Transform SD to DTBS
        ServiceLoaderFactory
            .getFormatTransformationService(ctx.getTransformation())
            .transform(ctx);

        // Step 3: Attach source documents to DTBS (only applies to XML -> PAdES)
        ServiceLoaderFactory
            .getSourceAttachmentService(ctx.getTransformation())
            .attachSourceFiles(ctx);

        // Step 4: Pre-sign the DTBS
        ServiceLoaderFactory
            .getSignatureStamperService(ctx.getDataToBeSigned().getFormat())
            .preSign(ctx);

        // Step 5: Compute Digest for DTBS - only used for SP flow
        updateDtbsDigest(ctx);

        // Step 6: Validate the signature parameters
        ctx.getSignatureParameters().validate();

        // Step 7: Sign the SignatureParameters
        String signedSignatureParameters = ServiceLoaderFactory
            .getSignatureParameterSigningService()
            .jwsSign(ctx.getSignatureParameters(), ctx.getSignatureKeys());

        // Finally, wrap as a singing payload
        var signingPayload = new SigningPayload(signedSignatureParameters, ctx.getDataToBeSigned());

        LOG.info("Produced signed payload in {} ms", System.currentTimeMillis() - t0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Signed payload: {} ", signingPayload);
        }

        return signingPayload;
    }


    /**
     * Variant of {@link #produceSigningPayload(TransformationContext)} that wraps the resulting
     * signing payload as {@link SigningPayloadDTO}.
     * <p>
     * Service Providers should call this method to produce a signing payload suitable for passing on
     * to the Signing Client as a JSON object.
     *
     * @param ctx the {@link TransformationContext} from which to instantiate the {@link SigningPayloadDTO}
     * @return the instantiated {@link SigningPayloadDTO}
     */
    public SigningPayloadDTO produceSigningPayloadDTO(TransformationContext ctx) throws NemLogInException {

        var signingPayload = produceSigningPayload(ctx);

        // Wrap as a signing payload DTO
        return new SigningPayloadDTO(signingPayload);
    }


    /**
     * For SP flow only.
     * Computes a digest for the entire DTBS document using the same algorithm as used for signing signature parameters
     * and updates the dtbsDigest field of the signature parameters.
     *
     * @param ctx the {@link TransformationContext} to compute and update the DTBS digest for
     */
    void updateDtbsDigest(TransformationContext ctx) throws TransformationException {
        if (ctx.getSignatureParameters().getFlowType() == FlowType.ServiceProvider) {
            try {
                var digester = MessageDigest.getInstance(ctx.getSignatureParameters().getDtbsDigestAlgorithm());
                byte[] digest = digester.digest(ctx.getDataToBeSigned().getData());
                ctx.updateDtbsDigest(Base64.getEncoder().encodeToString(digest));
            } catch (NoSuchAlgorithmException e) {
                throw new TransformationException(SDK006, ctx, "Error computing digest for DTBS", e);
            }
        }
    }
}
