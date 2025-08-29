package dk.gov.nemlogin.signing.xades.util;

import certifikat.gov.dk.nemlogin.v0_0.SignTextType;
import certifikat.gov.dk.nemlogin.v0_0.SignedDocumentType;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.parser.XMLParserException;
import org.w3._2000._09.xmldsig_.ObjectFactory;
import org.w3._2000._09.xmldsig_.SignatureType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility methods for processing XAdES documents
 */
public class XmlUtils {

    private static final ObjectFactory OBJECT_FACTORY_XMLDSIG = new ObjectFactory();
    private static final certifikat.gov.dk.nemlogin.v0_0.ObjectFactory OBJECT_FACTORY_NEMLOGIN = new certifikat.gov.dk.nemlogin.v0_0.ObjectFactory();

    private XmlUtils() {
    }

    /** Instantiates the JAXB Context to used for XAdES processing **/
    private static JAXBContext jaxbContext() throws JAXBException {
        // Create JAXB Context
        return JAXBContext.newInstance(SignedDocumentType.class, SignatureType.class);
    }

    /** Instantiates a new JAXB Marshaller **/
    private static Marshaller jaxbMarshaller() throws JAXBException {
        // Create Marshaller
        var jaxbMarshaller = jaxbContext().createMarshaller();
        // Required formatting. Must always be FALSE otherwise c14n# canonicalization fails because of new lines and empty spaces
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        return jaxbMarshaller;
    }

    /** Instantiates a new JAXB Unmarshaller **/
    private static Unmarshaller jaxbUnmarshaller() throws JAXBException {
        // Create Unmarshaller
        return jaxbContext().createUnmarshaller();
    }


    public static byte[] marshal(final SignedDocumentType signedDocument) throws JAXBException {
        final var sw = new StringWriter();
        jaxbMarshaller().marshal(OBJECT_FACTORY_NEMLOGIN.createSignedDocument(signedDocument), sw);
        return sw.toString().getBytes();
    }

    public static byte[] marshal(final SignTextType signText) throws JAXBException {
        final var sw = new StringWriter();
        jaxbMarshaller().marshal(OBJECT_FACTORY_NEMLOGIN.createSignText(signText), sw);
        return sw.toString().getBytes();
    }

    public static String marshal(final SignatureType signature) throws JAXBException {
        final var sw = new StringWriter();
        jaxbMarshaller().marshal(OBJECT_FACTORY_XMLDSIG.createSignature(signature), sw);
        return sw.toString();
    }

    public static SignedDocumentType unmarshal(final byte[] signText) throws JAXBException {
        return jaxbUnmarshaller().unmarshal( new StreamSource(new ByteArrayInputStream(signText)), SignedDocumentType.class).getValue();
    }

    /**
     * Produces a SHA-256 digest of the data
     *
     * @param data the data to hash
     * @return a SHA-256 digest of the data
     */
    public static byte[] sha256(byte[] data) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unable to calculate SHA-256", e);
        }
    }

    /**
     * Canonicalizes the DTBS document to the c14n# specification
     *
     * @param dataToBeSigned must be a self-contained xml document with start and end tag.
     * @return XML canonicalized to c14n# specification
     */
    public static byte[] canonicalize(final byte[] dataToBeSigned) throws IOException {
        org.apache.xml.security.Init.init();
        try {
            final var canonicalizer = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            var byteArrayOutputStream = new ByteArrayOutputStream();
            canonicalizer.canonicalize(dataToBeSigned, byteArrayOutputStream, true);
            return byteArrayOutputStream.toByteArray();
        } catch (CanonicalizationException | InvalidCanonicalizerException | XMLParserException e) {
            throw new IOException(e);
        }
    }
}
