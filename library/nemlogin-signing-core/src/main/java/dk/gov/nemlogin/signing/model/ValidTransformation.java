package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.format.ViewFormat;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.format.SignatureFormat;

import java.util.Objects;
import java.util.stream.Stream;


/**
 * Defines all valid combinations of the SD (signer's document) format, the signature format
 * and the view format.
 */
@SuppressWarnings("unused")
public enum ValidTransformation {

    /** Plain text is Base64-encoded and inserted into XML **/
    A (DocumentFormat.TEXT,   SignatureFormat.XAdES,   ViewFormat.TEXT),

    /** Plain text converted to PDF with a given font family and size **/
    B (DocumentFormat.TEXT,   SignatureFormat.PAdES,   ViewFormat.PDF),

    /** HTML is validated, Base64-encoded and inserted into XML **/
    C (DocumentFormat.HTML,    SignatureFormat.XAdES,   ViewFormat.HTML),

    /** HTML is validated and transformed to PDF **/
    D (DocumentFormat.HTML,    SignatureFormat.PAdES,   ViewFormat.PDF),

    /** XML is converted to HTML using a specified XSLT and validated as SignHTML **/
    E (DocumentFormat.XML,     SignatureFormat.XAdES,   ViewFormat.HTML),

    /** XML is converted to HTML using a specified XSLT and validated as SignHTML, then converted and attached to PDF **/
    F (DocumentFormat.XML,     SignatureFormat.PAdES,   ViewFormat.PDF),

    /** PDF is validated as SignPDF **/
    G (DocumentFormat.PDF,     SignatureFormat.PAdES,   ViewFormat.PDF),

    /** PDF is validated as SignPDF, then Base64-encoded and inserted into XML **/
    H (DocumentFormat.PDF,     SignatureFormat.XAdES,   ViewFormat.PDF);


    private final DocumentFormat sdFormat;
    private final SignatureFormat signatureFormat;
    private final ViewFormat viewFormat;


    /** Constructor **/
    ValidTransformation(DocumentFormat sdFormat, SignatureFormat signatureFormat, ViewFormat viewFormat) {
        this.sdFormat = Objects.requireNonNull(sdFormat);
        this.signatureFormat = Objects.requireNonNull(signatureFormat);
        this.viewFormat = Objects.requireNonNull(viewFormat);
    }

    public DocumentFormat getSdFormat() {
        return sdFormat;
    }

    public SignatureFormat getSignatureFormat() {
        return signatureFormat;
    }

    public ViewFormat getViewFormat() {
        return viewFormat;
    }

    /**
     * Returns the valid transformation associated with the given combination of SD and signature formats.
     *
     * @param sdFormat the SD format
     * @param signatureFormat the signature format
     * @return the valid transformation associated with the given combination of SD and signature formats
     */
    public static ValidTransformation transformationFor(DocumentFormat sdFormat, SignatureFormat signatureFormat) {
        return Stream.of(values())
            .filter(f -> f.sdFormat == sdFormat && f.signatureFormat == signatureFormat)
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException("No valid format for sdFormat=" + sdFormat + ", signatureFormat=" + signatureFormat));
    }
}
