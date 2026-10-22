package dk.gov.nemlogin.signing.broker.model;

import dk.gov.nemlogin.signing.broker.utils.SamlUtils;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static dk.gov.nemlogin.signing.broker.model.OIOSAMLAttribute.NamePrefix.DATA_GOV_DK;
import static dk.gov.nemlogin.signing.broker.model.OIOSAMLAttribute.NamePrefix.NEM_LOGIN_DK;

/**
 * Implementation of OIOSAML Assertion Attributes
 */
@SuppressWarnings("unused")
public class OIOSAMLAttribute {

    public enum NamePrefix {
        DATA_GOV_DK("https://data.gov.dk/"),
        NEM_LOGIN_DK("https://nemlog-in.dk/");

        final String prefixString;

        NamePrefix(String prefixString) {
            this.prefixString = prefixString;
        }
    }

    static final List<OIOSAMLAttribute> ALL = new ArrayList<>(2);

    static final String ATTRIBUTE_VALUE_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";

    // Common OIOSAML attributes
    public static final OIOSAMLAttribute ATTRIBUTE_SPEC_VERSION = attr(DATA_GOV_DK,"model/core/specVersion", "SpecVer", true);
    public static final OIOSAMLAttribute ATTRIBUTE_LOA = attr(DATA_GOV_DK,"concept/core/nsis/loa", "LoA", true);
    public static final OIOSAMLAttribute ATTRIBUTE_FULL_NAME = attr(DATA_GOV_DK,"model/core/eid/fullName", "FullName", false);
    public static final OIOSAMLAttribute ATTRIBUTE_FIRST_NAME = attr(DATA_GOV_DK,"model/core/eid/firstName", "FirsName", false, true);
    public static final OIOSAMLAttribute ATTRIBUTE_LAST_NAME = attr(DATA_GOV_DK,"model/core/eid/lastName", "LastName", false, true);
    public static final OIOSAMLAttribute ATTRIBUTE_AGE = attr(DATA_GOV_DK,"model/core/eid/age", "Age", false, true);
    public static final OIOSAMLAttribute ATTRIBUTE_CVR = attr(DATA_GOV_DK,"model/core/eid/professional/cvr", "CVR", false);
    public static final OIOSAMLAttribute ATTRIBUTE_ORG_NAME = attr(DATA_GOV_DK,"model/core/eid/professional/orgName", "OrganizationName", false, true);
    public static final OIOSAMLAttribute ATTRIBUTE_EMAIL = attr(DATA_GOV_DK,"model/core/eid/email", "Email", false);

    // NL3 Assertion attributes
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_POLICY_QUALIFIER = attr(DATA_GOV_DK,"model/core/signing/certificatePolicyQualifier", "CertificatePolicyQualifier", true);
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_SSN_PERSISTENCE_LEVEL = attr(DATA_GOV_DK,"model/core/signing/certificateSSNPersistenceLevel", "CertificateSSNPersistenceLevel", true);
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_HOLDER_ID = attr(DATA_GOV_DK,"model/core/signing/certificateHolderIdentifier", "CertificateHolderIdentifier", true);
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_CN = attr(DATA_GOV_DK,"model/core/signing/certificateCommonName", "CertificateCommonName", false);
    public static final OIOSAMLAttribute ATTRIBUTE_CERT_ANONYMIZED_SIGNER = attr(DATA_GOV_DK,"model/core/signing/anonymizedSigner", "AnonymizedSigner", false);
    public static final OIOSAMLAttribute ATTRIBUTE_REFERENCE_TEXT = attr(DATA_GOV_DK,"model/core/signing/referenceText", "ReferenceText", true);
    public static final OIOSAMLAttribute ATTRIBUTE_LOIP = attr(NEM_LOGIN_DK, "internal/loipextended", "LoIP", false);

    private final String name;
    private final String friendlyName;
    private final boolean required;
    private final boolean metadata;

    /** Constructor **/
    private OIOSAMLAttribute(NamePrefix namePrefix, String name, String friendlyName, boolean required, boolean metadata) {
        this.name = namePrefix.prefixString + Objects.requireNonNull(name);
        this.friendlyName = friendlyName;
        this.required = required;
        this.metadata = metadata;
    }


    /** Factory method **/
    public static OIOSAMLAttribute attr(NamePrefix prefix, String name, String friendlyName, boolean required, boolean metadata) {
        var attribute = new OIOSAMLAttribute(prefix, name, friendlyName, required, metadata);
        ALL.add(attribute);
        return attribute;
    }

    /** Factory method **/
    public static OIOSAMLAttribute attr(NamePrefix prefix, String name, String friendlyName, boolean required) {
        return attr(prefix, name, friendlyName, required, false);
    }

    public String getName() {
        return name;
    }

    public String getFriendlyName() {
        return friendlyName;
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
