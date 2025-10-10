package dk.gov.nemlogin.signing.broker.controller;

import dk.gov.nemlogin.signing.broker.config.BrokerSigningConfiguration;
import dk.gov.nemlogin.signing.broker.service.DocumentSigningService;
import dk.gov.nemlogin.signing.broker.service.SignersDocumentService;
import dk.gov.nemlogin.signing.format.SignatureFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * Handles controller part of the MVC application
 */
@Controller
@SuppressWarnings({"SpringMVCViewInspection"})
public class DocumentSigningController {

    private final SignersDocumentService signersDocumentService;
    private final DocumentSigningService documentSigningService;
    private final BrokerSigningConfiguration conf;

    /**
     * Constructor
     **/
    public DocumentSigningController(
        SignersDocumentService signersDocumentService,
        DocumentSigningService documentSigningService, BrokerSigningConfiguration conf) {
        this.signersDocumentService = signersDocumentService;
        this.documentSigningService = documentSigningService;
        this.conf = conf;
    }


    /**
     * index.html page
     **/
    @GetMapping({"/", "/index.html"})
    public String index(Model model) throws IOException {
        model.addAttribute("documents", signersDocumentService.documents());
        model.addAttribute("correlationId",UUID.randomUUID().toString());
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
        model.addAttribute("format", signatureFormat.name());
        model.addAttribute("correlationId", correlationId);
        model.addAttribute("signingPayload", documentSigningService.generateSigningPayload(signatureFormat, fileName));
        model.addAttribute("signingApiUrl", conf.getProperties().getSigningApiUrl());
        model.addAttribute("timestamp", String.valueOf(new Date().getTime()));
        return "sign";
    }
}
