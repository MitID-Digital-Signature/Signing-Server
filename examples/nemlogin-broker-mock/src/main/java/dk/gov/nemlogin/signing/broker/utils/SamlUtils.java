package dk.gov.nemlogin.signing.broker.utils;

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.security.SecurityException;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

/**
 * OpenSAML utility methods
 */
public final class SamlUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SamlUtils.class);

    private SamlUtils() {
    }

    /** Initialize OpenSAML **/
    public static void initOpenSAML() {
        try {
            JavaCryptoValidationInitializer cryptoValidationInitializer = new JavaCryptoValidationInitializer();
            cryptoValidationInitializer.init();
            InitializationService.initialize();
            LOG.info("Initialized OpenSAML");
        } catch (InitializationException e) {
            LOG.error("Cannot initialize OpenSAML", e);
        }
    }

    /**
     * Builds a SAML object for the given class
     *
     * @param clazz the class
     * @return the SAML object
     */
    @SuppressWarnings("unchecked")
    public static <T> T build(final Class<T> clazz) {
        try {
            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
            QName defaultElementName = (QName)clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
            return  (T) Objects.requireNonNull(builderFactory.getBuilder(defaultElementName))
                .buildObject(defaultElementName);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Could not create SAML object for class " + clazz, e);
        }
    }

    /**
     * Serializes the SAML object
     *
     * @param object the SAML object to serialize
     * @param indent whether to indent the seriablized SAML XML or not
     */
    public static String serializeSamlObject(final XMLObject object, boolean indent) throws IOException {
        Element element;

        if (object instanceof SignableSAMLObject && ((SignableSAMLObject)object).isSigned() && object.getDOM() != null) {
            element = object.getDOM();
        } else {
            Marshaller out = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(object);
            assert out != null;
            try {
                out.marshall(object);
            } catch (MarshallingException e) {
                throw new IOException(e);
            }
            element = object.getDOM();
        }

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // XML parsers should not be vulnerable to XXE attacks
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        StreamResult result;
        try {
            Transformer transformer = transformerFactory.newTransformer();
            if (indent) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            }
            result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(element);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new IOException(e);
        }
        return result.getWriter().toString();
    }

    /**
     * Returns the public key info for the given credential
     * @param credential the credential
     * @return the public key info
     */
    public static KeyInfo getPublicKeyInfo(BasicX509Credential credential) throws IOException {
        try {
            X509KeyInfoGeneratorFactory x509KeyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
            x509KeyInfoGeneratorFactory.setEmitEntityCertificate(true);
            return x509KeyInfoGeneratorFactory.newInstance().generate(credential);
        } catch (SecurityException e) {
            throw new IOException(e);
        }
    }
}
