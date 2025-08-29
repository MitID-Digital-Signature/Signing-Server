/**
 * Wrapper of the NemLog-In Broker Signing API
 *
 * Disclaimer:
 * This code is not production code. It's purpose is to illustrate for the Broker how
 * to call the NemLog-In Signing API.<br>
 * The API calls in this file are described in detail "NL3 Signature Broker implementation guidelines".
 */

/** Call Signing API -> begin-signature-flow  **/
function beginSignatureFlow(signatureParameters) {
    return $.post(`${signingApiUrl}/signing/begin-signature-flow`, JSON.stringify({"signatureParameters": signatureParameters}));
}

/** Call Signing API -> issue-certificate **/
function issueCertificate(samlPayload) {
    return $.post(`${signingApiUrl}/signing/issue-certificate`, JSON.stringify(samlPayload));
}

/** Call Signing API -> create-xades-ltv **/
function createXadesLtv(req) {
    return $.post(`${signingApiUrl}/signing/create-xades-ltv`, JSON.stringify(req));
}

/** Call Signing API -> create-xades-lta **/
function createXadesLta(req) {
    return $.post(`${signingApiUrl}/signing/create-xades-lta`, JSON.stringify(req));
}

/** Call Signing API -> create-pades-ltv **/
function createPadesLtv(req) {
    return $.post(`${signingApiUrl}/signing/create-pades-ltv`, JSON.stringify(req));
}

/** Call Signing API -> create-pades-lta **/
function createPadesLta(req) {
    return $.post(`${signingApiUrl}/signing/create-pades-lta`, JSON.stringify(req));
}
