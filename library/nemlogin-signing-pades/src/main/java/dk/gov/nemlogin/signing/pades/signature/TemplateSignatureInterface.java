package dk.gov.nemlogin.signing.pades.signature;

import dk.gov.nemlogin.signing.service.TransformationContext;
import dk.gov.nemlogin.signing.util.SignatureKeysProducer;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

/**
 * Serves two purposes:
 * <ul>
 *     <li>Computes the CMS signature for the PDF using the Service Provider key-pair.
 *         This is a temporary signature overridden by the Signature Client.</li>
 *     <li>Depending on the "nemlogin.signing.pades.reset-signature-content" transformation property,
 *         the service will either return the CMS or "0" for the PDF Signature Dictionary Content field.</li>
 * </ul>
 */
public class TemplateSignatureInterface implements SignatureInterface {

    private static final String KEY_RESET_SIGNATURE_CONTENT = "nemlogin.signing.pades.reset-signature-content";
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final String SIGNING_ALGORITHM = "SHA256withRSA";

    private final TransformationContext ctx;
    private CMSSignedData signedData;


    /** Constructor **/
    public TemplateSignatureInterface(TransformationContext ctx) {
        this.ctx = ctx;
    }


    /**
     * Returns whether to use the CMS or "0" for the PDF Signature Dictionary Content field
     * @return whether to use the CMS or "0" for the PDF Signature Dictionary Content field
     */
    private boolean resetSignatureContent() {
        if (ctx.getTransformationProperties() != null) {
            String reset = ctx.getTransformationProperties().getProperty(KEY_RESET_SIGNATURE_CONTENT, "true");
            return !"false".equalsIgnoreCase(reset);
        }
        // Reset by default
        return true;
    }


    /**
     * Calculates the CMS signature of the content, but returns an empty byte array to ensure that
     * the PDF Signature Dictionary Content field is filled with "0".
     *
     * @param content the PDF document
     * @return the signature
     */
    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            // Create dummy keys for producing the CMS signature.
            var signatureKeys = SignatureKeysProducer.getInstance();

            var bc = new BouncyCastleProvider();
            Store<?> certStore = new JcaCertStore(Collections.singletonList(signatureKeys.getCertificate()));

            var input = new CMSTypedDataInputStream(content);
            var gen = new CMSSignedDataGenerator();
            var sha256Signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                .setProvider(bc)
                .build(signatureKeys.getPrivateKey());

            gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                    .setProvider(bc)
                    .build())
                .build(sha256Signer, new X509CertificateHolder(signatureKeys.getCertificate().getEncoded())));
            gen.addCertificates(certStore);

            // Compute the CMS digest
            this.signedData = gen.generate(input, false);

            // Fill the signature content field with "0" or return CMS, depending on transformation property
            return resetSignatureContent()
                ? EMPTY_BYTE_ARRAY
                : this.signedData.getEncoded();

        } catch (Exception e) {
            throw new IOException("Error generating DTBS PDF Signature", e);
        }
    }


    /** Returns the generated signed data **/
    public CMSSignedData getSignedData() {
        return signedData;
    }

    /**
     * Wrap CMS input stream
     */
    static class CMSTypedDataInputStream implements CMSTypedData {
        InputStream in;

        /** Constructor **/
        public CMSTypedDataInputStream(InputStream in) {
            this.in = in;
        }

        /** {@inheritDoc} **/
        @Override
        public ASN1ObjectIdentifier getContentType() {
            return PKCSObjectIdentifiers.data;
        }

        /** {@inheritDoc} **/
        @Override
        public Object getContent() {
            return in;
        }

        /** {@inheritDoc} **/
        @Override
        public void write(OutputStream out) throws IOException {
            var buffer = new byte[4 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
        }
    }
}
