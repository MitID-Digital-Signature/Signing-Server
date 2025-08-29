package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.format.SignatureFormat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * Encapsulates the DTBS (Data To Be Signed) document to be signed by the signing component.
 * The data is loaded fully into memory.
 * <p>
 * The {@code name} is intended to be e.g. the associated file name and is only used for logging and error reporting
 */
@SuppressWarnings("unused")
public abstract class DataToBeSigned implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SignatureFormat format;
    private final byte[] data;
    private final String name;

    /**
     * Constructor
     * @param format data input format
     * @param data the actual data
     */
    DataToBeSigned(SignatureFormat format, byte[] data, String name) {
        this.format = format;
        this.data = data;
        this.name = name;
    }

    public SignatureFormat getFormat() {
        return format;
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the data as UTF-8 text
     * @return the data as UTF-8 text
     */
    public String dataAsText() {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Returns the data as an {@link InputStream}
     * @return the data as an {@link InputStream}
     */
    public InputStream dataInputStream() {
        return new ByteArrayInputStream(data);
    }

    /**
     * Encapsulates XAdES DTBS
     */
    public static class XadesDataToBeSigned extends DataToBeSigned {
        public XadesDataToBeSigned(byte[] data, String name) {
            super(SignatureFormat.XAdES, data, name);
        }
    }


    /**
     * Encapsulates PAdES DTBS
     */
    public static class PadesDataToBeSigned extends DataToBeSigned {
        public PadesDataToBeSigned(byte[] data, String name) {
            super(SignatureFormat.PAdES, data, name);
        }
    }
}
