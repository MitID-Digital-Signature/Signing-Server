package dk.gov.nemlogin.signing.controller;

import dk.gov.nemlogin.signing.format.SignatureFormat;
import dk.gov.nemlogin.signing.service.DocumentSigningService;
import dk.gov.nemlogin.signing.service.SignersDocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.UUID;

/**
 * Handles controller part of the MVC application
 */
@Controller
@SuppressWarnings({"SpringMVCViewInspection"})
public class DocumentSigningController {

    private final SignersDocumentService signersDocumentService;
    private final DocumentSigningService documentSigningService;

    /**
     * Constructor
     **/
    public DocumentSigningController(
        SignersDocumentService signersDocumentService,
        DocumentSigningService documentSigningService) {
        this.signersDocumentService = signersDocumentService;
        this.documentSigningService = documentSigningService;
    }


    /**
     * index.html page
     **/
    @GetMapping({"/", "/index.html"})
    public String index(Model model) throws IOException {
        model.addAttribute("documents", signersDocumentService.documents());
        model.addAttribute("correlationId", UUID.randomUUID().toString());
        model.addAttribute("deleteEnabled", signersDocumentService.sdFolderDefined());
        return "index";
    }


    /**
     * sign.html page
     **/
    @GetMapping("/sign/{format:.+}/{fileName:.+}")
    public String sign(
        @PathVariable("format") String format,
        @PathVariable("fileName") String fileName,
        @RequestParam("correlationId") String correlationId,
        Model model) throws IOException {

        // Resolve DTBS signature format
        SignatureFormat signatureFormat = "xml".equalsIgnoreCase(format)
            ? SignatureFormat.XAdES
            : SignatureFormat.PAdES;

        model.addAttribute("document", signersDocumentService.document(fileName));
        model.addAttribute("format", format);
        model.addAttribute("correlationId", correlationId);
        model.addAttribute("signingPayload", documentSigningService.generateSigningPayload(signatureFormat, fileName));
        model.addAttribute("signingClientUrl", documentSigningService.getSigningClientUrl() + "?correlationId=" + correlationId);
        return "sign";
    }
}
