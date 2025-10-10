package dk.gov.nemlogin.signing.broker.model;

import dk.gov.nemlogin.signing.broker.utils.SamlUtils;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Implementation of OIOSAML Assertion Attributes
 */
@SuppressWarnings("unused")
public class OIOSAMLAttribute {

    public static final String NAME_PREFIX = "https://data.gov.dk/";
    static final String ATTRIBUTE_VALUE_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";

    // Common OIOSAML attributes
    public static final OIOSAMLAttribute ATTRIBUTE_SPEC_VERSION = attr("model/core/specVersion", "SpecVer");
    public static final OIOSAMLAttribute ATTRIBUTE_LOA = attr("concept/core/nsis/loa", "LoA");
    public static final OIOSAMLAttribute ATTRIBUTE_FULL_NAME = attr("model/core/eid/fullName", "FullName");
    public static final OIOSAMLAttribute ATTRIBUTE_FIRST_NAME = attr("model/core/eid/firstName", "FirsName");
    public static final OIOSAMLAttribute ATTRIBUTE_LAST_NAME = attr("model/core/eid/lastName", "LastName");
    public static final OIOSAMLAttribute ATTRIBUTE_AGE = attr("model/core/eid/age", "Age");
    public static final OIOSAMLAttribute ATTRIBUTE_CVR = attr("model/core/eid/professional/cvr", "CVR");
    public static final OIOSAMLAttribute ATTRIBUTE_ORG_NAME = attr("model/core/eid/professional/orgName", "OrganizationName");
    public static final OIOSAMLAttribute ATTRIBUTE_EMAIL = attr("model/core/eid/email", "Email");
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_POLICY_QUALIFIER = attr("model/core/signing/certificatePolicyQualifier", "CertificatePolicyQualifier");
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_SSN_PERSISTENCE_LEVEL = attr("model/core/signing/certificateSSNPersistenceLevel", "CertificateSSNPersistenceLevel");
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_HOLDER_ID = attr("model/core/signing/certificateHolderIdentifier", "CertificateHolderIdentifier");
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_CN = attr("model/core/signing/certificateCommonName", "CertificateCommonName");
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_ANONYMIZED_SIGNER = attr("model/core/signing/anonymizedSigner", "AnonymizedSigner");

    private final String name;
    private final String friendlyName;


    /** Constructor **/
    private OIOSAMLAttribute(String name, String friendlyName) {
        this.name = NAME_PREFIX + Objects.requireNonNull(name);
        this.friendlyName = friendlyName;
    }

    public String getName() {
        return name;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    /** Factory method **/
    public static OIOSAMLAttribute attr(String name, String friendlyName) {
        return new OIOSAMLAttribute(name, friendlyName);
    }


    /**
     * Construct an {@link Attribute} with a single string value from this attribute specification
     * @param value the value
     * @return the {@link Attribute}
     */
    public Attribute asSimpleAttribute(String value) {
        Attribute attribute = SamlUtils.build(Attribute.class);
        attribute.setName(name);
        if (StringUtils.hasText(friendlyName)) {
            attribute.setFriendlyName(friendlyName);
        }
        attribute.setNameFormat(ATTRIBUTE_VALUE_FORMAT);
        XSAnyBuilder xsAnyBuilder = new XSAnyBuilder();
        XSAny textValue = xsAnyBuilder.buildObject(SAMLConstants.SAML20_NS, AttributeValue.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20_PREFIX);
        textValue.setTextContent(value);
        attribute.getAttributeValues().add(textValue);
        return attribute;
    }


    /**
     * Adds this attribute as an {@link Attribute} to the given {@link AttributeStatement}
     * @param attributeStatement the list of {@link Attribute} to add this attribute to
     */
    public void addTo(AttributeStatement attributeStatement, String value) {
        attributeStatement.getAttributes().add(asSimpleAttribute(value));
    }

}
