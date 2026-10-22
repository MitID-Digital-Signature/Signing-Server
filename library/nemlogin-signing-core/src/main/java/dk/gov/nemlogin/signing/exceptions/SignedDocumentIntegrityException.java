package dk.gov.nemlogin.signing.exceptions;

import dk.gov.nemlogin.signing.model.IntegrityValues;

import static dk.gov.nemlogin.signing.exceptions.ErrorCode.SDK013;

public class SignedDocumentIntegrityException extends NemLogInException {

    private IntegrityValues integrityValues;

    public SignedDocumentIntegrityException(IntegrityValues integrityValues, String message ) {
        super( SDK013, message );
        this.integrityValues = integrityValues;
    }

    public IntegrityValues getIntegrityValues() {
        return integrityValues;
    }
}
