package dk.gov.nemlogin.signing.pdf.validation;

import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.ValidationException;
import dk.gov.nemlogin.signing.format.DocumentFormat;
import dk.gov.nemlogin.signing.service.SignersDocumentValidationService;
import dk.gov.nemlogin.signing.service.TransformationContext;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates the PDF document against PDF whitelists and valid fonts
 */
public class PdfValidationService implements SignersDocumentValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(PdfValidationService.class);

    /**
     * {@inheritDoc}
     **/
    @Override
    public boolean supports(DocumentFormat sdFormat) {
        return DocumentFormat.PDF == sdFormat;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ValidationException
     **/
    @Override
    public void validate(TransformationContext ctx) throws ValidationException {

        // Parse the PDF. Throws a ValidationException if the PDF cannot be parsed
        try (COSDocument pdf = parsePdf(ctx)) {
            List<PdfValidationResult> validationResults = new ArrayList<>();
            // Validate against whitelists
            validationResults.addAll(validateAgainstWhitelist(pdf));
            // Validate fonts
            validationResults.addAll(PdfFontValidator.validateFonts(pdf));

            if (!validationResults.isEmpty()) {
                String errorStr =
                    "Signer's Document " + ctx.getSignersDocument().getName() +
                        " did not validate for PDF elements: " + PdfValidationResult.toString(validationResults, true, 4048);
                throw new ValidationException(ErrorCode.SDK010, ctx, errorStr);
            }
        } catch (IOException e) {
            LOG.error("error closing pdf", e);
        }
    }


    /**
     * Parses the Signer's Document of the transformation context and returns the parsed {@link COSDocument}
     *
     * @param ctx the transformation context
     * @return the parsed {@link COSDocument}
     */
    public COSDocument parsePdf(TransformationContext ctx) throws ValidationException {
        var pdf = ctx.getSignersDocument();
        COSDocument cosDocument;
        try {
            var inputStream = new ByteArrayInputStream(pdf.getData());
            var parser = new ValidationPdfParser(inputStream);
            parser.parse();
            cosDocument = parser.getCOSDocument();
        } catch (Exception e) {
            throw new ValidationException(ErrorCode.SDK010, ctx,
                "Error parsing Signer's Document " + pdf.getName() + ": " + e, e);
        }
        return cosDocument;
    }


    /**
     * Validates document against a whitelist using the {@link PdfWhitelistValidator}
     *
     * @param cosDocument the COS tree structure from the PDF document
     * @return the validation result
     */
    public List<PdfValidationResult> validateAgainstWhitelist(final COSDocument cosDocument) {
        List<COSObject> objectList = cosDocument.getObjects();
        return objectList.stream()
            .map(cosObject -> new PdfWhitelistValidator().whitelistValidation(cosObject))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

}
