package dk.gov.nemlogin.signing.controller;

import dk.gov.nemlogin.signing.service.SignersDocumentService;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * API for managing the signer's documents
 */
@Controller
public class SignersDocumentController {

    private static final Logger LOG = LoggerFactory.getLogger(SignersDocumentController.class);
    private static final List<String> VALID_UPLOAD_FILE_EXTENSIONS = Arrays.asList(
        // Signer's Document Formats
        "pdf", "xml", "xsl", "txt", "html",
        // Fonts - embedded in PAdES with matching file name
        "ttf", "otf",
        // Properties - embedded in XAdES with matching file name as SignProperties
        "properties"
    );

    private final SignersDocumentService signersDocumentService;


    /** Constructor **/
    public SignersDocumentController(SignersDocumentService signersDocumentService) {
        this.signersDocumentService = signersDocumentService;
    }

    private static void deleteFile(Path fileToBeDeleted) {
        try {
            Files.delete(fileToBeDeleted);
        } catch (IOException e) {
            LOG.error("unable to delete file {}", fileToBeDeleted, e);
        }
    }


    /**
     * Download REST endpoint, used to download the given signer's document file.
     *
     * @param fileName file name of the document to download
     * @param request the request
     * @return the response containing the file
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) throws IOException {
        // Load file as Resource
        Resource resource = signersDocumentService.resource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            LOG.warn("Could not determine file type.", e);
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


    /**
     * Upload REST endpoint, used for uploading files to the "signers-documents" folder
     *
     * @param files the files to upload to the "signers-documents" folder
     * @return the index.html page
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadFile(@RequestParam("files") MultipartFile[] files, Model model) throws IOException {

        if (files != null && files.length > 0) {
            Path folder = signersDocumentService.checkCreateSdFolder();
            for (MultipartFile file : files) {
                // IE11 returns full path, Chrome only filename
                String filename = requireNonNull(file.getResource()).getFilename();
                String extension = SigningUtils.fileExtension(filename).toLowerCase();
                if (SigningUtils.isNotEmpty(filename) && VALID_UPLOAD_FILE_EXTENSIONS.contains(extension)) {
                    Files.write(folder.resolve(filename), file.getBytes());
                    LOG.info("Saved file {} to folder {}", filename, folder);
                }
            }
        }
        return indexPage(model);
    }


    /**
     * Delete REST endpoint, used to delete the given signer's document file.
     *
     * @param fileName file name of the document to delete
     * @return the index.html page
     */
    @GetMapping("/delete/{fileName:.+}")
    public String deleteFile(@PathVariable String fileName, Model model) throws IOException {
        // Delete the file
        Resource resource = signersDocumentService.resource(fileName);
        if (signersDocumentService.sdFolderDefined() && resource.exists()) {
            Path path = resource.getFile().toPath();
            Files.delete(path);
            LOG.info("Deleted Signer's Document {}", fileName);

            // If the file is an xml, delete the associated xsl.
            final Path parent = path.getParent();

            if(parent!=null) {
                Path xslPath = parent.resolve(SigningUtils.fileName(fileName, "xsl"));
                if ("xml".equalsIgnoreCase(SigningUtils.fileExtension(fileName)) && Files.exists(xslPath)) {
                    Files.delete(xslPath);
                    LOG.info("Deleted Signer's Document XSL {}", xslPath);
                }

                // If there is an associated sign-properties file, delete it as well
                Path signPropsPath = parent.resolve(SigningUtils.fileName(fileName, "properties"));
                if (Files.exists(signPropsPath)) {
                    Files.delete(signPropsPath);
                    LOG.info("Deleted Sign Properties {}", signPropsPath);
                }
            }
        }
        return indexPage(model);
    }


    /**
     * Reset Documents REST endpoint, used to reset the signer's documents to the default set.
     *
     * @return the index.html page
     */
    @GetMapping("/reset-documents")
    public String resetSignersDocuments(Model model) throws IOException {
        if (signersDocumentService.sdFolderDefined()) {
            try(var stream = Files.walk(signersDocumentService.checkCreateSdFolder())) {
                stream.filter(Files::isRegularFile).collect(Collectors.toList())
                    .stream().forEach(x -> x.toFile().delete()) ;
            }
            signersDocumentService.checkCreateSdFolder().toFile().delete();
        }
        return indexPage(model);
    }

    /**
     * Sets up the page model and returns the index.html page
     *
     * @param model the page model
     * @return the index.html page
     */
    private String indexPage(Model model) throws IOException {
        model.addAttribute("documents", signersDocumentService.documents());
        model.addAttribute("deleteEnabled", signersDocumentService.sdFolderDefined());
        return "redirect:/";
    }
}
