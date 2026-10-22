package dk.gov.nemlogin.signing.model;

import java.io.Serial;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the Service Provider private key and certificate chain used
 * for JWS signing the {@link SignatureParameters}
 */
@SuppressWarnings("unused")
public class SignatureKeys implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient PrivateKey privateKey;
    private final List<X509Certificate> certificateChain;

    /**
     * Constructor
     * @param privateKey the private key used for JWS signing
     * @param certificateChain the certificate chain associated with the key
     */
    public SignatureKeys(PrivateKey privateKey, List<X509Certificate> certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = Collections.unmodifiableList(certificateChain);
    }


    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public List<X509Certificate> getCertificateChain() {
        return certificateChain;
    }


    /**
     * Returns the certificate chain excluding the root certificate
     * @return the certificate chain excluding the root certificate
     */
    public List<X509Certificate> getNonRootCertificateChain() {
        return certificateChain.stream()
            .filter(c -> !c.getIssuerX500Principal().getName().equals(c.getSubjectX500Principal().getName()))
            .toList();
    }


    /**
     * Returns the first certificate in the list
     * @return the first certificate in the list
     */
    public X509Certificate getCertificate() {
        // First certificate of the chain is the actual certificate used for signing
        return certificateChain.getFirst();
    }


    /**
     * Returns the public key of the first certificate in the list
     * @return the public key of the first certificate in the list
     */
    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey)getCertificate().getPublicKey();
    }
}
