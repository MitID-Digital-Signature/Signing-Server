package dk.gov.nemlogin.signing.pdf.validation;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdfparser.PDFObjectStreamParser;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A PDF Parser used for validating PDFs.
 * <p>
 * This PDF parser is more strict than the usual PDF parser.
 * It turns off leniency whist parsing.
 * <p>
 * Furthermore, the parser ensures that object streams are loaded and hence validated,
 * and that non-referenced objects are also DocumentSigningController added for the purpose of validation.
 */
public class ValidationPdfParser extends PDFParser {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationPdfParser.class);
    /**
     * Constructor
     *
     * @param inputStream the PDF input stream
     */
    // suppression sonar: likes try with resources to closed stream - closed elsewhere
    @SuppressWarnings("squid:S2095")
    public ValidationPdfParser(InputStream inputStream) throws IOException {
        super(new ScratchFile(MemoryUsageSetting.setupMainMemoryOnly()).createBuffer(inputStream));
        // Disable leniency
        setLenient(false);
    }

    /**
     * Shortcut method that returns the parsed {@link COSDocument}.
     *
     * @return the parsed {@link COSDocument}
     */
    public COSDocument getCOSDocument() throws IOException {
        return getPDDocument().getDocument();
    }


    /**
     * {@inheritDoc}
     * <p>
     * Furthermore, override the usual {@link super.#initialParse()} method to also perform the following tasks:
     * <ul>
     *     <li>Parse non-referenced objects present in the XRef.</li>
     *     <li>Ensures that the object streams are loaded.</li>
     * </ul>
     */
    @Override
    protected void initialParse() throws IOException {
        // First, perform a normal initialParse()
        super.initialParse();

        // Get the list of referenced object numbers
        Set<Long> objNumbers = document.getObjects().stream()
            .map(COSObject::getObjectNumber)
            .collect(Collectors.toSet());

        // Compute the list of non-reference object numbers by comparing with the XRef
        List<COSObjectKey> nonReferencedObjKeys = document.getXrefTable().keySet().stream()
            .filter(objKey -> !objNumbers.contains(objKey.getNumber()))
            .collect(Collectors.toList());

        // Parse the non-referenced objects - will be added to the document object pool
        for (COSObjectKey objKey : nonReferencedObjKeys) {
            try {
                parseObjectDynamically(objKey.getNumber(), objKey.getGeneration(), false);
            } catch (IOException e) {
                LOG.trace("ignored error", e);
            }
        }

        // Ensures that the object streams are loaded.
        dereferenceObjectStreams();
    }


    /**
     * This version of the {@link COSDocument#dereferenceObjectStreams()} will parse and add the object stream objects
     * to the document even if they are not referenced in the (stream-based) XRef.
     * The purpose is to validate all object stream objects.
     */
    // sonar - ignoring "Reduce the number of conditional operators (4) used in the expression (maximum allowed 3)."
    @SuppressWarnings("java:S1067")
    void dereferenceObjectStreams() throws IOException {
        for ( COSObject objStream : document.getObjectsByType( COSName.OBJ_STM ) ) {
            COSStream stream = (COSStream)objStream.getObject();
            var parser = new PDFObjectStreamParser(stream, document);
            parser.parse();
            for (COSObject next : parser.getObjects()) {
                var key = new COSObjectKey(next);
                if (document.getObjectFromPool(key) == null
                    || document.getObjectFromPool(key).getObject() == null
                    || document.getObjectFromPool(key).getObject() instanceof COSNull
                    // xrefTable stores negated objNr of objStream for objects in objStreams
                    || (document.getXrefTable().containsKey(key)
                    && document.getXrefTable().get(key) == -objStream.getObjectNumber())) {
                    COSObject obj = document.getObjectFromPool(key);
                    obj.setObject(next.getObject());
                }
            }
        }
    }
}
