package dk.gov.nemlogin.signing.format;

/**
 * Defines the valid SD (signer's document) formats.
 */
public enum DocumentFormat {

    /** Plain UTF-8 text **/
    TEXT,

    /** The SignHTML subset supported by the NemLog-In signing component **/
    HTML,

    /** XML **/
    XML,

    /** The SignPDF subset supported by the NemLog-In signing component **/
    PDF
}
