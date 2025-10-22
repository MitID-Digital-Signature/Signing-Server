/**
 * Miscellaneous JavaScript functionality for the Broker Signing Mock app.
 *
 * Disclaimer:
 * This code is not production code. It's purpose is to illustrate for the Broker how
 * to call the NemLog-In Signing API.<br>
 * Please refer to "NL3 Signature SP implementation guidelines" and "NL3 Signature Broker implementation guidelines"
 * for details about formats and APIs.
 */

/** Configure Ajax with proper headers **/
function ajaxSetup(sessionId = undefined) {
    const basicHeaders = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        "CorrelationIdManager.CorrelationId": correlationId,
    };
    const sessionHeader = sessionId ? { "X-DIGST-Signing-SessionId": sessionId } : {};
    $.ajaxSetup({
        headers: {...basicHeaders, ...sessionHeader}
    });
    return sessionId;
}

/** Simulates sign-in by creating a mock Broker IdP SAML Assertion **/
function samlAssertion() {
    return $.get("/saml/saml-assertion");
}

/** Logs a message to the console **/
function logToConsole(msg) {
    msg = msg.split(/\r?\n/).join("<br />");
    $("#console").append(`<p>${msg}</p>`);
    $("#console")[0].scrollIntoView(false);
}

/** Truncates a string **/
function trunc(str, len = 300) {
    return str && str.length > len ?
        str.substring(0, len - 3) + "..." :
        str
}

/** Compute a SHA-512 digest of the given value **/
function computeSha512Digest(value) {
    const digest = sha512.arrayBuffer(value);
    return String.fromCharCode(...new Uint8Array(digest));
}

/** Extracts and parses the SignatureParameters payload from JWS-sealed payload **/
function parseSignatureParameters(jws) {
    const jwsPayload = jws.split('.')[1];
    return JSON.parse(window.atob(jwsPayload));
}

/** Initialize the viewer displaying the Signers Document **/
function initViewer(documentFormat, signatureFormat, dtbs) {
    if (signatureFormat === "PAdES") {
        initPdfViewer(dtbs);
    } else if (signatureFormat === "XAdES") {
        const xades = Base64.decode(dtbs);
        switch (documentFormat) {
            case "PDF":
                initPdfViewer(extractXmlElement(xades, "Document"));
                break;
            case "HTML":
                initHtmlViewer(extractXmlElement(xades, "Document", true, true));
                break;
            case "TEXT":
                initHtmlViewer(extractPlainText(xades));
                break;
            case "XML":
                initHtmlViewer(extractXml(xades));
                break;
        }
    }
}

/** Initialize the PDF viewer **/
function initHtmlViewer(html) {
    const htmlViewer = $('<div/>', {'class': 'html-viewer', id: 'html-viewer'});
    htmlViewer.html(html);
    // To avoid CSS of the HTML affecting the surrounding content, embed in iframe
    const htmlFrame = $('<iframe/>', { 'class': 'html-viewer-frame', 'id': 'html-viewer-frame'});
    htmlFrame.appendTo('#document-viewer')
        // Bizarrely, Firefox seems to only update on 'load' whereas Chrome ignores this...
        .on('load', () => htmlFrame.contents().find('body').append(htmlViewer));
    htmlFrame.contents().find("body").append(htmlViewer);

    // Local anchor tags in HTML dynamically added to an iframe do not work well. Clicks will load entire page in iframe.
    htmlViewer.find('a[href^="#"]').click(function(event){
        event.preventDefault();
        const a = htmlViewer.find(`a[name='${this.hash.substr(1)}']`);
        if (a.length > 0) {
            htmlFrame.contents().scrollTop(a.offset().top);
        }
    })
}

/** Naive - but sufficient - way of extracting element **/
function extractXmlElement(xml, name, content = true, decode = false) {
    const elmStartPattern = new RegExp(`<(\\w+:)?${name}[^>]*>`, "gm");
    const elmEndPattern = new RegExp(`</(\\w+:)?${name}>`, "gm");
    const startMatch = elmStartPattern.exec(xml);
    const endMatch = elmEndPattern.exec(xml);
    if (startMatch !== null && endMatch !== null) {
        const contentIndex = startMatch.index + startMatch[0].length;
        const contentLength = endMatch.index - contentIndex;
        const elmIndex = startMatch.index;
        const elmLength = endMatch.index + endMatch[0].length - elmIndex;
        const result = content ? xml.substr(contentIndex, contentLength) : xml.substr(elmIndex, elmLength);
        return decode ? Base64.decode(result) : result;
    }
    return undefined;
}

/** Extracts plain text from the XAdES */
function extractPlainText(xades) {
    let text = extractXmlElement(xades, "Document", true, true);
    // Escape all HTML entities
    // See https://stackoverflow.com/questions/14462612/escape-html-text-in-an-angularjs-directive
    text = text.replace(/[\u00A0-\u9999<>\&\'\"]/gim, i => '&#' + i.charCodeAt(0) + ';');
    let html = text.split(/\r?\n/).join("<br />");

    const monospace = extractXmlElement(xades, "UseMonoSpaceFont");
    // Match the fonts used by SignSDK for generating PAdES
    if (monospace.toLowerCase() === "true") {
        html = `<html><body"><pre style="font: medium Courier">${html}</pre></body></html>`;
    } else {
        html = `<html><body style="font: medium Helvetica">${html}</body></html>`;
    }
    return html;
}

/**
 * Extracts XML + XSL from the XML
 */
function extractXml(xades) {
    const xml = extractXmlElement(xades,"Document", true, true);
    const xsl = extractXmlElement(xades, "Transformation", true, true);
    // Standard XSLT for modern browsers
    const xmlDoc = getDocument(xml);
    const xslDoc = getDocument(xsl);
    const xsltProcessor = new XSLTProcessor();
    xsltProcessor.importStylesheet(xslDoc);
    const resultDocument = xsltProcessor.transformToFragment(xmlDoc, xmlDoc);
    const xmlSerializer = new XMLSerializer();
    return xmlSerializer.serializeToString(resultDocument);
}

/** Get xml Document by parsing input string */
function getDocument(xmlString) {
    const parser = new DOMParser();
    const document = parser.parseFromString(xmlString, "text/xml");
    if (document.getElementsByTagName("parsererror").length > 0  ||
        document.getElementsByTagNameNS("http://www.w3.org/1999/xhtml", 'parsererror').length > 0) {
        throw new Error("Error parsing XML");
    }
    return document;
}

/** Converts the DTBS from Base64 to a binary data array **/
function convertDtbsToBinary(dtbs) {
    const raw = window.atob(dtbs);
    const rawLength = raw.length;
    const array = new Uint8Array(new ArrayBuffer(rawLength));

    for(let i = 0; i < rawLength; i++) {
        array[i] = raw.charCodeAt(i);
    }
    return array;
}

/** Render the page of the PDF **/
function renderPdfPage(pdf, canvas, pageNumToRender = 1) {
    const canvasContext = canvas.getContext('2d');
    pdf.getPage(pageNumToRender).then(page => {
        const viewport = page.getViewport(1);
        canvas.height = viewport.height;
        canvas.width = viewport.width;
        let renderCtx = {canvasContext ,viewport};
        page.render(renderCtx);
    });
}

/** Initializes PDF.js and displays the DTBS in the give canvas element **/
function initPdfJs(dtbs, canvas) {
    let pdfjsLib = window['pdfjs-dist/build/pdf'];
    pdfjsLib.GlobalWorkerOptions.workerSrc = "/webjars/pdfjs-dist/build/pdf.worker.js";

    pdfjsLib.getDocument({data: convertDtbsToBinary(dtbs)}).promise.then(pdfData => {
        renderPdfPage(pdfData, canvas);
    });
}

/** Initialize the PDF viewer **/
function initPdfViewer(pdf) {
    const pdfCanvas = $('<canvas/>',{ 'class':'pdf-viewer', id: 'pdf-viewer' });
    $('#document-viewer').append(pdfCanvas);
    initPdfJs(pdf, pdfCanvas[0]);
}
