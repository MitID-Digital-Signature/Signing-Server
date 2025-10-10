package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.format.DocumentFormat;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * Encapsulates the original SD (Signer's Document) to be signed by the signing component.
 * <p>
 * Once {@link SignersDocumentFile#getData()} has been called, the data is loaded fully into memory.
 */
@SuppressWarnings("unused")
public abstract class SignersDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    private final DocumentFormat format;
    private final SignersDocumentFile file;
    private final SignProperties properties;


    /**
     * Designated Constructor
     * @param format data input format
     * @param file the document file
     * @param properties optional sign properties
     */
    SignersDocument(DocumentFormat format, SignersDocumentFile file, SignProperties properties) {
        this.format = format;
        this.file = file;
        this.properties = properties;
    }

    /**
     * Constructor
     * @param format data input format
     * @param file the document file
     */
    SignersDocument(DocumentFormat format, SignersDocumentFile file) {
        this(format, file, null);
    }

    /**
     * Constructor
     * @param format data input format
     * @param data the actual data
     * @param name the document name
     */
    SignersDocument(DocumentFormat format, byte[] data, String name) throws NemLogInException {
        this(format,
            SignersDocumentFile.builder()
                .setData(data)
                .setName(name)
                .build());
    }


    public DocumentFormat getFormat() {
        return format;
    }

    public byte[] getData() throws NemLogInException {
        return file.getData();
    }

    public String getName() {
        return file.getName();
    }

    public SignersDocumentFile getFile() {
        return file;
    }

    public SignProperties getProperties() {
        return properties;
    }

    /**
     * Returns the data as UTF-8 text
     * @return the data as UTF-8 text
     */
    public String dataAsText() throws NemLogInException {
        return new String(file.getData(), StandardCharsets.UTF_8);
    }


    /**
     * Encapsulates UTF-8 plain text SD
     */
    public static class PlainTextSignersDocument extends SignersDocument {
        private final boolean useMonoSpaceFont;

        /** Constructor **/
        public PlainTextSignersDocument(SignersDocumentFile file, boolean useMonoSpaceFont, SignProperties properties) {
            super(DocumentFormat.TEXT, file, properties);
            this.useMonoSpaceFont = useMonoSpaceFont;
        }

        /** Constructor **/
        public PlainTextSignersDocument(SignersDocumentFile file, boolean useMonoSpaceFont) {
            this(file, useMonoSpaceFont, null);
        }

        public boolean isUseMonoSpaceFont() {
            return useMonoSpaceFont;
        }
    }

    /**
     * Encapsulates HTML (SignHTML) SD
     */
    public static class HtmlSignersDocument extends SignersDocument {

        /** Constructor **/
        public HtmlSignersDocument(SignersDocumentFile file, SignProperties properties) {
            super(DocumentFormat.HTML, file, properties);
        }

        /** Constructor **/
        public HtmlSignersDocument(SignersDocumentFile file) {
            this(file, null);
        }

    }

    /**
     * Encapsulates XML SD
     */
    public static class XmlSignersDocument extends SignersDocument {
        private final SignersDocumentFile xsltFile;

        /** Constructor **/
        public XmlSignersDocument(SignersDocumentFile file, SignersDocumentFile xsltFile, SignProperties properties) {
            super(DocumentFormat.XML, file, properties);
            this.xsltFile = xsltFile;
        }

        /** Constructor **/
        public XmlSignersDocument(SignersDocumentFile file, SignersDocumentFile xsltFile) {
            this(file, xsltFile, null);
        }


        public SignersDocumentFile getXsltFile() {
            return xsltFile;
        }

        public byte[] getXsltData() throws NemLogInException {
            return xsltFile.getData();
        }

        /**
         * Returns the XSLT data as UTF-8 text
         * @return the XSLT data as UTF-8 text
         */
        public String xsltAsText() throws NemLogInException {
            return new String(xsltFile.getData(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Encapsulates PDF (SignPDF) SD
     */
    public static class PdfSignersDocument extends SignersDocument {

        /** Constructor **/
        public PdfSignersDocument(SignersDocumentFile file, SignProperties properties) {
            super(DocumentFormat.PDF, file, properties);
        }

        /** Constructor **/
        public PdfSignersDocument(SignersDocumentFile file) {
            this(file, null);
        }
    }
}
