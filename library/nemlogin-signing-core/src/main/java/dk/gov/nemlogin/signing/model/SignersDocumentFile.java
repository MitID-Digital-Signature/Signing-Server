package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.exceptions.ErrorCode;
import dk.gov.nemlogin.signing.exceptions.NemLogInException;
import dk.gov.nemlogin.signing.util.SigningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Wraps the actual file of a {@link SignersDocument}.
 * Supports lazy loading of the file contents the first time {@link #getData()} is called..
 */
@SuppressWarnings("unused")
public class SignersDocumentFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SignersDocumentFile.class);

    public static final String DEFAULT_NAME = "unnamed";

    // 20 mb
    public static final int MAX_FILE_SIZE = 1024 * 1024 * 20;

    private Long creationTime;
    private Long lastModified;
    private String name;

    // File source - one of:
    private String classPath;

    private String path;
    private URL url;

    // File data - lazy loaded
    private byte[] data;

    /**
     * Constructor
     **/
    private SignersDocumentFile() {
    }

    /**
     * Constructor
     **/
    private SignersDocumentFile(SignersDocumentFile template) {
        this.creationTime = template.creationTime;
        this.lastModified = template.lastModified;
        this.name = template.name;
        this.classPath = template.classPath;
        this.path = template.path;
        this.url = template.url;
        this.data = template.data;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public String getName() {
        return name;
    }

    public String getClassPath() {
        return classPath;
    }

    public Path getPath() {
        return path != null ? Paths.get(path) : null;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * Returns the data of the SD file.
     * Load and cache the data first time around
     *
     * @return the SD data
     */
    public byte[] getData() throws NemLogInException {
        if (data == null) {
            data = loadData();
        }
        if (data.length > MAX_FILE_SIZE) {
            throw new NemLogInException(ErrorCode.SDK010, "Signer's Document file exceeds max size of 20 mb");
        }
        return data;
    }


    /**
     * Loads the data of the SD file.
     * @return the SD data
     */
    private synchronized byte[] loadData() throws NemLogInException {
        if (data == null) {
            try {
                if (SigningUtils.isNotEmpty(classPath)) {
                    data = SigningUtils.loadBytes(classPath);
                } else if (path != null) {
                    data = SigningUtils.loadBytes(Paths.get(path));
                } else {
                    data = SigningUtils.loadBytes(url);
                }
            } catch (Exception e) {
                throw new NemLogInException(ErrorCode.SDK001, e);
            }
        }
        return data;
    }


    /**
     * Returns a new {@link Builder}
     **/
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Builder for the {@link SignersDocumentFile}.
     */
    public static class Builder extends SignersDocumentFile {
        final SignersDocumentFile template = new SignersDocumentFile();

        public Builder setCreationTime(Long creationTime) {
            this.template.creationTime = creationTime;
            return this;
        }

        public Builder setLastModified(Long lastModified) {
            this.template.lastModified = lastModified;
            return this;
        }

        public Builder setName(String name) {
            this.template.name = name;
            return this;
        }

        public Builder setClassPath(String classPath) {
            this.template.classPath = classPath;
            return this;
        }

        public Builder setPath(String path) {
            this.template.path = path;
            return this;
        }

        public Builder setUrl(URL url) {
            this.template.url = url;
            return this;
        }

        public Builder setData(byte[] data) {
            this.template.data = data;
            return this;
        }

        private void buildName(SignersDocumentFile result) {
            if (SigningUtils.isEmpty(result.name)) {
                result.name = DEFAULT_NAME;

                if (result.path != null || result.url != null) {
                    Path path = result.path != null ? Paths.get(result.path) : Paths.get(result.url.getPath());
                    Path fileName = path.getFileName();

                    if (fileName != null) {
                        result.name = fileName.toString();
                    }
                }
            }
        }

        private void buildLastModified(SignersDocumentFile result) {
            if (result.lastModified == null && result.path != null) {
                try {
                    result.lastModified = Files.getLastModifiedTime(Paths.get(result.path)).toMillis();
                } catch (IOException e) {
                    LOG.warn("Cannot read creation time of file: {}", result.path, e);
                }
            }
        }

        private void buildCreationTime(SignersDocumentFile result) {
            if (result.creationTime == null && result.path != null) {
                try {
                    result.creationTime = ((FileTime) Files.getAttribute(Paths.get(result.path), "creationTime")).toMillis();
                } catch (IOException e) {
                    LOG.warn("Cannot read creation time of file: {}", result.path, e);
                }
            }
        }

        /**
         * Validates and builds a new {@link SignersDocumentFile}
         *
         * @return a new {@link SignersDocumentFile}
         */
        public SignersDocumentFile build() throws NemLogInException {
            SignersDocumentFile result = new SignersDocumentFile(template);

            buildCreationTime(result);
            buildLastModified(result);
            buildName(result);

            long sourceNo = Stream.of(result.data, result.classPath, result.path, result.url)
                .filter(Objects::nonNull)
                .count();
            if (sourceNo == 0) {
                throw new NemLogInException(ErrorCode.SDK001, "At least one Signer's Document file source must be defined");
            }
            return result;
        }
    }
}
