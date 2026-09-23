package dk.gov.nemlogin.signing.service;

import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.exceptions.SignedDocumentIntegrityException;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.model.IntegrityValues;

public interface DocumentIntegrityService extends NemLogInService<SignatureFormat> {

    /**
     * Produces integrity values for comparing the original document before signing, with the signed document to ensure the returned
     * signed document has not been tampered with.
     *
     * @param ctx the {@link TransformationContext} after the signature payload has been created.
     * @return the integrity values needed for verifying the integrity of the signed document when the signing flow completes.
     */
    IntegrityValues createIntegrityValues(TransformationContext ctx) throws NemLogInException;

    /**
     *
     * Verifies the integrity of the signed document by comparing it with the integrity values produced by IntegrityValueCreator.createIntegrityValues()
     *
     * Throws {@link SignedDocumentIntegrityException} if the integrity verification fails.
     *
     * @param signedDocument the signed document
     * @param integrityValues the {@link IntegrityValues}
     * @throws SignedDocumentIntegrityException
     */
    void verifySignedDocument(byte[] signedDocument, IntegrityValues integrityValues) throws SignedDocumentIntegrityException;
}
