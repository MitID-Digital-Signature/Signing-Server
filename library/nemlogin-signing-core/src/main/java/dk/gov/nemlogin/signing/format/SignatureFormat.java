package dk.gov.nemlogin.signing.format;

/**
 * Defines the valid Signature formats.
 * <p>
 * Values defined verbatim according to the Signing flow specification.
 */
public enum SignatureFormat {

    /** XAdES (XAdES-B-LTA) format containing XMLDsig **/
    XAdES,

    /** PAdES (PAdES-B-LTA) containing signature dictionary **/
    PAdES
}
