package dk.gov.nemlogin.signing.util;

import dk.gov.nemlogin.signing.model.SignatureKeys;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

/**
 * Utility class used for generating the private key and certificate used for
 * pre-signing the DTBS (Data TO Be Signed) document.<br>
 * The private key + certificate will be returned in the form of a {@link SignatureKeys} object.
 * <p>
 * The CMS signature will subsequently be replaced by the Signing Client in the user browser.
 */
@SuppressWarnings("unused")
public class SignatureKeysProducer {

    private static final String SIGNING_ALGORITHM = "SHA256withRSA";
    private static final String DN = "cn=NemLog-In";
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    private static SignatureKeys instance;

    /**
     * Factory method for returning a single cached instance of a generated {@link SignatureKeys}
     *
     * @return a single cached instance of a generated {@link SignatureKeys}
     */
    public static synchronized SignatureKeys getInstance() throws IOException {
        if (instance == null) {
            instance = new SignatureKeysProducer().createSignatureKeys();
        }
        return instance;
    }


    /**
     * Generates a new private key and certificate
     *
     * @return the new key material wrapped as a {@link SignatureKeys} object
     */
    private SignatureKeys createSignatureKeys() throws IOException {

        SigningUtils.addBouncyCastleProvider();

        // Step 1 - generate key pair
        KeyPair keyPair = null;
        X509Certificate cert = null;
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA", BC);
            keyPairGenerator.initialize(1024, new SecureRandom());
            keyPair = keyPairGenerator.generateKeyPair();

            // Step 2 - produce a certificate
            var dnName = new X500Name(DN);
            var subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNING_ALGORITHM);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded()));

            X509CertificateHolder certificateHolder = new X509v3CertificateBuilder(
                dnName,
                BigInteger.valueOf(System.currentTimeMillis()),
                Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
                dnName,
                subPubKeyInfo)
                .addExtension(
                    new ASN1ObjectIdentifier("2.5.29.19"),
                    false,
                    new BasicConstraints(false))
                .addExtension(
                    new ASN1ObjectIdentifier("2.5.29.15"),
                    true,
                    new X509KeyUsage(
                        X509KeyUsage.digitalSignature |
                            X509KeyUsage.nonRepudiation |
                            X509KeyUsage.keyEncipherment |
                            X509KeyUsage.dataEncipherment))
                .build(sigGen);

            cert = new JcaX509CertificateConverter()
                .setProvider(BC)
                .getCertificate(certificateHolder);
        } catch (CertificateException | OperatorCreationException | NoSuchProviderException | NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        return new SignatureKeys(keyPair.getPrivate(), Collections.singletonList(cert));
    }
}

