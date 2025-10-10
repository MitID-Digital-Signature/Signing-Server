package dk.gov.nemlogin.signing.model;

/**
 * Persistence level for UUID in the "subjectSerialNumber" of the short term certificate.
 * <p>
 * Values defined verbatim according to the Signing flow specification.
 */
@SuppressWarnings("unused")
public enum SsnPersistenceLevel {
    Session,
    Global
}
