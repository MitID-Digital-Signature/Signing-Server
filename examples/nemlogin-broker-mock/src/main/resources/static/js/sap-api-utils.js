/**
 * The SAP API library is used to create a signature value based on the digest to be signed.
 * The API is using Cryptomathic signersdk.min.js and the session-creation-key.js JavaScript libraries.
 *
 * Disclaimer:
 * This code is not production code. It's purpose is to illustrate for the Broker how
 * to call the NemLog-In Signing API.<br>
 * The code in this file is almost verbatim the code documented in "NL3 Signature Broker implementation guidelines"
 */

function convertStringToNumbers(values) {
    let bytes = [];
    for (let c = 0; c < values.length; c += 2) {
        bytes.push(parseInt(values.substr(c, 2), 16));
    }
    return bytes;
}

function convertNumberToString(values) {
    let resp = "";
    for (const n of values) {
        const value = String.fromCharCode(n);
        resp += value;
    }
    return resp;
}

function encodeSAMLAssertion(assertion) {
    function toUTF8Array(str) {
        let utf8 = [];
        for (let i = 0; i < str.length; i++) {
            let charcode = str.charCodeAt(i);
            if (charcode < 0x80) utf8.push(charcode);
            else if (charcode < 0x800) {
                utf8.push(0xc0 | (charcode >> 6),
                    0x80 | (charcode & 0x3f));
            } else if (charcode < 0xd800 || charcode >= 0xe000) {
                utf8.push(0xe0 | (charcode >> 12),
                    0x80 | ((charcode >> 6) & 0x3f),
                    0x80 | (charcode & 0x3f));
            }
            // surrogate pair
            else {
                i++;
                // UTF-16 encodes 0x10000-0x10FFFF by
                // subtracting 0x10000 and splitting the
                // 20 bits of 0x0-0xFFFFF into two halves
                charcode = 0x10000 + (((charcode & 0x3ff) << 10)
                    | (str.charCodeAt(i) & 0x3ff));
                utf8.push(0xf0 | (charcode >> 18),
                    0x80 | ((charcode >> 12) & 0x3f),
                    0x80 | ((charcode >> 6) & 0x3f),
                    0x80 | (charcode & 0x3f));
            }
        }
        return utf8;
    }

    function base64decode(data) {
        return window.atob(data)
    }

    return toUTF8Array(base64decode(assertion));
}


function createSignatureValue(issueCertResponse, sessionId, correlationId, resolveSign) {

    let forwarderURL = window.location.origin + "/signer-forwarder?sessionId="
        + encodeURIComponent(sessionId) + "&correlationId=" + encodeURIComponent(correlationId);

    let sdk = new Cryptomathic.SignerUserSDK.SDK(forwarderURL, 10000);
    try {
        sdk.initialize();
        console.log("Successfully initialized SDK.");

        let rejectLogOff = function (errorType, message) {
            console.debug("logoff failed msg:'" + message + "'")
        };

        let cleanup = function () {
            sdk.logoff(resolveLogOff, rejectLogOff);
            sdk.free();
        };

        let resolveLogOff = function () {
            console.debug("logoff successful");
            cleanup();
        };

        let rejectSign = function (errorType, message) {
            console.error("signing rejected msg:'" + message + "'");
            cleanup();
        };

        let resolve = function (tokenList, policyList) {
            console.log("Successfully created session.");
            let aPolicy = policyList[0];

            // keyEntry from a PolicyEntry received in
            // createSession resolve callback.
            let keyEntry = aPolicy.KEY_LIST[0];
            let hashToSign = convertStringToNumbers(issueCertResponse.digestToBeSigned);

            console.log("executing sign");
            sdk.sign(keyEntry, hashToSign, resolveSign, rejectSign)
        };

        // sad is taken from /issue-certificate response sad
        // field [5]
        let sad = issueCertResponse.sad;
        let samlAssertion = encodeSAMLAssertion(sad);

        sdk.createSession(resolve, rejectSign, samlAssertion);
    } catch (error) {
        console.error("error " + error);
        sdk.free();
    }

}

/** Call the Cryptomathic Signer Forwarder API - wrap result as Promise **/
function createSignatureVal(issueCertResponse, sessionId, correlationId) {
    return new Promise(resolve => {
        createSignatureValue(issueCertResponse, sessionId, correlationId, signatureBytes => {
            // QSCD returns an array of Numbers / the rest of the flow requires a Base64 encoded string
            // so we will convert the number[] -> string -> Base64
            const signingHashAsString = convertNumberToString(signatureBytes);
            const signingHashB64 = Base64.btoa(signingHashAsString);
            resolve(signingHashB64)
        });
    })
}
