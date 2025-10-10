package dk.gov.nemlogin.signing.broker.service;

import dk.gov.nemlogin.signing.broker.config.BrokerSigningConfiguration;
import dk.gov.nemlogin.signing.broker.config.SamlProperties;
import dk.gov.nemlogin.signing.broker.model.OIOSAMLAttribute;
import dk.gov.nemlogin.signing.broker.utils.SamlUtils;
import dk.gov.nemlogin.signing.model.SignatureKeys;
import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.impl.AssertionMarshaller;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Service used for generating a mock Broker IdP SAML Assertion
 */
@Service
@SuppressWarnings("SameParameterValue")
public class BrokerIdpSamlService {

    static final RandomIdentifierGenerationStrategy ID_GENERATOR = new RandomIdentifierGenerationStrategy();
    static final String PASSWORD_PROTECTED_TRANSPORT = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    static final String SPEC_VERSION_OIOSAML30 = "OIO-SAML-3.0";
    static final String METHOD_BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";

    private final SignatureKeys signatureKeys;
    private final BrokerSigningConfiguration conf;
    private final SamlProperties samlProps;


    /**
     * Constructor
     **/
    public BrokerIdpSamlService(
        SignatureKeys signatureKeys,
        BrokerSigningConfiguration conf) {
        this.signatureKeys = signatureKeys;
        this.conf = conf;
        this.samlProps = conf.getProperties().getSaml();
    }


    @PostConstruct
    void init() {
        SamlUtils.initOpenSAML();
    }


    /**
     * Builds a new Broker assertion
     *
     * @param sessionId the sign-flow session ID
     * @return the SAML Assertion
     */
    public String buildAssertion(String sessionId) throws IOException {

        Date issueInstant = new Date();

        // Create assertion
        Assertion assertion = SamlUtils.build(Assertion.class);
        assertion.setIssueInstant(issueInstant.toInstant());
        assertion.setID(ID_GENERATOR.generateIdentifier());

        // AuthnStatement
        AuthnStatement authnStatement = SamlUtils.build(AuthnStatement.class);
        authnStatement.setAuthnInstant(issueInstant.toInstant());
        authnStatement.setSessionIndex(ID_GENERATOR.generateIdentifier());
        AuthnContext authnContext = SamlUtils.build(AuthnContext.class);
        AuthnContextClassRef authnContextClassRef = SamlUtils.build(AuthnContextClassRef.class);
        authnContextClassRef.setURI(PASSWORD_PROTECTED_TRANSPORT);
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);
        assertion.getAuthnStatements().add(authnStatement);

        // AttributeStatement
        String certHolderId = UUID.randomUUID().toString();
        AttributeStatement attributeStatement = SamlUtils.build(AttributeStatement.class);
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_SPEC_VERSION, attributeStatement, SPEC_VERSION_OIOSAML30);
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_LOA, attributeStatement, samlProps.getAttrLoa());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_CERT_POLICY_QUALIFIER, attributeStatement, samlProps.getAttrCertPolicy());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_CERT_SSN_PERSISTENCE_LEVEL, attributeStatement, samlProps.getAttrSsnPersistenceLevel());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_CERT_HOLDER_ID, attributeStatement, "person:" + certHolderId);
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_FULL_NAME, attributeStatement, samlProps.getAttrFirstName() + " " + samlProps.getAttrLastName());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_FIRST_NAME, attributeStatement, samlProps.getAttrFirstName());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_LAST_NAME, attributeStatement, samlProps.getAttrLastName());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_EMAIL, attributeStatement, samlProps.getAttrEmail());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_AGE, attributeStatement, samlProps.getAttrAge());
        addAssertionAttribute(OIOSAMLAttribute.ATTRIBUTE_CERT_ANONYMIZED_SIGNER, attributeStatement, samlProps.getAttrAnonymized());
        assertion.getAttributeStatements().add(attributeStatement);

        // Issuer
        Issuer issuer = SamlUtils.build(Issuer.class);
        issuer.setFormat(NameIDType.ENTITY);
        issuer.setValue(conf.getProperties().getEntityId());
        assertion.setIssuer(issuer);

        // Subject
        Subject subject = SamlUtils.build(Subject.class);
        NameID nameID = SamlUtils.build(NameID.class);
        nameID.setFormat(NameIDType.PERSISTENT);
        nameID.setValue(certHolderId);
        nameID.setSPNameQualifier(samlProps.getSpEntityId());
        subject.setNameID(nameID);
        SubjectConfirmation subjectConfirmation = SamlUtils.build(SubjectConfirmation.class);
        subjectConfirmation.setMethod(METHOD_BEARER);
        SubjectConfirmationData subjectConfirmationData = SamlUtils.build(SubjectConfirmationData.class);
        subjectConfirmationData.setRecipient(samlProps.getAssertionConsumerUrl());
        subjectConfirmationData.setNotOnOrAfter(LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.UTC));
        subjectConfirmationData.setInResponseTo(sessionId);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        assertion.setSubject(subject);


        // Audience restriction
        AudienceRestriction audienceRestriction = SamlUtils.build(AudienceRestriction.class);
        Audience audience = SamlUtils.build(Audience.class);
        audience.setURI(samlProps.getSpEntityId());
        audienceRestriction.getAudiences().add(audience);
        Conditions conditions = SamlUtils.build(Conditions.class);
        conditions.setNotBefore(issueInstant.toInstant());
        conditions.setNotOnOrAfter(LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.UTC));
        conditions.getAudienceRestrictions().add(audienceRestriction);
        assertion.setConditions(conditions);

        // Sign and return the Assertion
        signAssertion(assertion, signatureKeys);

        String serializedAssertion = SamlUtils.serializeSamlObject(assertion, false);
        return Base64.getEncoder().encodeToString(serializedAssertion.getBytes(UTF_8));
    }


    /**
     * Signs a SAML Assertion
     *
     * @param assertion the Assertion to sign
     */
    private void signAssertion(Assertion assertion, SignatureKeys signatureKeys) throws IOException {
        Signature signature = SamlUtils.build(Signature.class);
        BasicX509Credential signingCredential = new BasicX509Credential(signatureKeys.getCertificate(), signatureKeys.getPrivateKey());
        SignatureRSASHA256 signatureRSASHA256 = new SignatureRSASHA256();
        signature.setSigningCredential(signingCredential);
        signature.setCanonicalizationAlgorithm(CanonicalizationMethod.EXCLUSIVE);
        signature.setSignatureAlgorithm(signatureRSASHA256.getURI());
        signature.setKeyInfo(SamlUtils.getPublicKeyInfo(signingCredential));
        assertion.setSignature(signature);
        AssertionMarshaller marshaller = new AssertionMarshaller();
        try {
            marshaller.marshall(assertion);
            Signer.signObject(signature);
        } catch (SignatureException | MarshallingException e) {
            throw new IOException(e);
        }
    }


    /**
     * Adds the attribute if the value is non-null
     **/
    private void addAssertionAttribute(OIOSAMLAttribute attr, AttributeStatement attributeStatement, String value) {
        if (value != null) {
            attr.addTo(attributeStatement, value);
        }
    }
}
