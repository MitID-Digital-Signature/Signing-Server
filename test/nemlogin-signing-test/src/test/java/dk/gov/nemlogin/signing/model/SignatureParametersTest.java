package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.exceptions.InvalidSignatureParametersException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.SignatureParameters.Builder;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static dk.gov.nemlogin.signing.model.AcceptedCertificatePolicy.Person;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test various aspects of {@link SignatureParameters}
 */
class SignatureParametersTest {

    Supplier<Builder> spFlowBuilder = () -> SignatureParameters.builder()
        .setFlowType(FlowType.ServiceProvider)
        .setEntityID("https://saml.serviceprovider.dk/login")
        .setDocumentFormat(DocumentFormat.XML)
        .setSignatureFormat(SignatureFormat.XAdES)
        .setDtbsDigest("xxx")
        .setDtbsSignedInfo("xxx")
        .setReferenceText("Signed by xxx");

    Supplier<Builder> brokerFlowBuilder = () -> SignatureParameters.builder()
        .setFlowType(FlowType.Broker)
        .setEntityID("https://saml.serviceprovider.dk/login")
        .setDocumentFormat(DocumentFormat.XML)
        .setSignatureFormat(SignatureFormat.XAdES)
        .setDtbsSignedInfo("xxx");

    /**
     * Test mandatory parameters for the Service Provider Flow
     */
    @Test
    void testSignatureParametersForSpFlow() {
        assertDoesNotThrow(() -> spFlowBuilder.get().build().validate());

        // Assert missing mandatory fields yields validation exception
        var ex = InvalidSignatureParametersException.class;
        assertThrows(ex, () -> spFlowBuilder.get().setVersion(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setFlowType(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setEntityID(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setDocumentFormat(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setSignatureFormat(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setDtbsDigest(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setDtbsSignedInfo(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setDtbsDigestAlgorithm(null).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setReferenceText(null).build().validate());

        // Assert additional input validation
        assertDoesNotThrow(() -> spFlowBuilder.get().setReferenceText("x".repeat(SignatureParameters.REFERENCE_TEXT_MAX_LENGTH)).build().validate());
        assertThrows(ex, () -> spFlowBuilder.get().setReferenceText("x".repeat(SignatureParameters.REFERENCE_TEXT_MAX_LENGTH + 1)).build().validate());
    }

    /**
     * Test mandatory parameters for the Broker Flow
     */
    @Test
    void testSignatureParametersForBrokerFlow() {
        assertDoesNotThrow(() -> brokerFlowBuilder.get().build().validate());

        // Assert missing mandatory fields yields validation exception
        var ex = InvalidSignatureParametersException.class;
        assertThrows(ex, () -> brokerFlowBuilder.get().setVersion(null).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setFlowType(null).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setEntityID(null).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setDocumentFormat(null).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setSignatureFormat(null).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setDtbsSignedInfo(null).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setDtbsDigestAlgorithm(null).build().validate());

    }

    @Test
    void testSignatureParametersForBrokerFlowFields() {
        var ex = InvalidSignatureParametersException.class;
        // Assert invalid Broker flow fields yields validation exception
        assertThrows(ex, () -> brokerFlowBuilder.get().setDtbsDigest("xxx").build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setReferenceText("Signed by xxx").build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setMinAge(18).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setPreferredLanguage(Language.da).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setSsnPersistenceLevel(SsnPersistenceLevel.Global).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setSignerSubjectNameID("uuid").build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setAnonymizeSigner(false).build().validate());
        assertThrows(ex, () -> brokerFlowBuilder.get().setAcceptedCertificatePolicies(Person).build().validate());
    }
}
