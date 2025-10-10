package dk.gov.nemlogin.signing.util;

import dk.gov.nemlogin.signing.model.SignatureKeys;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.gov.nemlogin.signing.util.SigningUtils.isEmpty;
import static dk.gov.nemlogin.signing.util.SigningUtils.isNotEmpty;

/**
 * Utility class used for loading {@link SignatureKeys} from a keystore
 */
@SuppressWarnings("unused")
public class SignatureKeysLoader {

    protected String keystoreClassPath;
    protected Path keystorePath;
    protected String keystoreType = "PKCS12";
    protected String keyPairAlias;
    protected String keystorePassword;
    protected String privateKeyPassword;


    public SignatureKeysLoader setKeystoreClassPath(String keystoreClassPath) {
        this.keystoreClassPath = keystoreClassPath;
        return this;
    }

    public SignatureKeysLoader setKeystorePath(Path keystorePath) {
        this.keystorePath = keystorePath;
        return this;
    }

    public SignatureKeysLoader setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
        return this;
    }

    public SignatureKeysLoader setKeyPairAlias(String keyPairAlias) {
        this.keyPairAlias = keyPairAlias;
        return this;
    }

    public SignatureKeysLoader setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public SignatureKeysLoader setPrivateKeyPassword(String privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword;
        return this;
    }


    /**
     * Load private key and certificate chain from the keystore
     * @return the private key and certificate chain from the keystore
     */
    public SignatureKeys loadSignatureKeys() throws IOException {
        PrivateKey privateKey;
        List<X509Certificate> chain;

        try {
            // Load the keystore from the specified classpath or file location
            var keystore = KeyStore.getInstance(keystoreType);

            // Validations
            if ((isNotEmpty(keystoreClassPath) && keystorePath != null) || (isEmpty(keystoreClassPath) && keystorePath == null)) {
                throw new IllegalArgumentException("Exactly one of keyStoreClassPath and keyStorePath must be defined");
            }
            if (Stream.of(keyPairAlias, privateKeyPassword).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("The keyStoreType, keyPairAlias and privateKeyPassword must be defined");
            }

            try (InputStream in = (keystorePath == null) ? getClass().getResourceAsStream(keystoreClassPath)
                                                         : new FileInputStream(keystorePath.toFile())) {
                keystore.load(in, isEmpty(keystorePassword) ? null : keystorePassword.toCharArray());
            }

            // Fetch the private key and certificate chain from the keystore
            if(!keystore.isKeyEntry(keyPairAlias)) {
                throw new IllegalArgumentException("The keystore (class:"+keystoreClassPath+"|path:"+keystorePath+") does not contain the alias: " + keyPairAlias);
            }

            privateKey = (PrivateKey) keystore.getKey(keyPairAlias, privateKeyPassword.toCharArray());
            chain = Arrays.stream(keystore.getCertificateChain(keyPairAlias))
                .map(X509Certificate.class::cast)
                .collect(Collectors.toList());
        } catch (UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException(e);
        }

        return new SignatureKeys(privateKey, chain);
    }
}
