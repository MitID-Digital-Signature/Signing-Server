package dk.gov.nemlogin.signing.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * Various utility method used throughout the project.
 * <p>
 * If using older Java SDKs, such as JDK 8, this is the class to update
 */
@SuppressWarnings("unused")
public class SigningUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SigningUtils.class);
    private static final String VERSION_FILE = "classpath:/signsdk-version.properties";

    private SigningUtils() {
    }

    /**
     * Adds the Bouncy Castle provider if it's not already added
     */
    public static void addBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Loads all bytes from the given path
     * @param path the path to load data from
     * @return the loaded data
     * @throws IOException on errors
     */
    public static byte[] loadBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }


    /**
     * Loads all bytes from the given input stream.
     * The input stream in not closed.
     * @param in the input stream to load data from
     * @return the loaded data
     * @throws IOException on errors
     */
    public static byte[] loadBytes(InputStream in) throws IOException {
        return in.readAllBytes();
    }


    /**
     * Loads all bytes from the given classpath.
     * @param classPath the class path to load data from
     * @return the loaded data
     * @throws IOException on errors
     */
    public static byte[] loadBytes(String classPath) throws IOException {
        try (InputStream in = SigningUtils.class.getResourceAsStream(classPath)) {
            return loadBytes(in);
        }
    }


    /**
     * Loads all bytes from the given URL
     * @param url the URL to load data from
     * @return the loaded data
     * @throws IOException on errors
     */
    public static byte[] loadBytes(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return loadBytes(in);
        }
    }


    /**
     * Loads all bytes from the given path. The path may have the following prefix:
     * <ul>
     *     <li>"classpath:" - loads from the classpath.</li>
     *     <li>"file:" - loads from the file system.</li>
     *     <li>"http(s):" - loads from the URL.</li>
     *     <li>none - loads from classpath</li>
     * </ul>
     * @param path the protocol path to load data from
     * @return the loaded data
     * @throws IOException on errors
     */
    public static byte[] loadBytesByProtocol(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            return loadBytes(path.substring("classpath:".length()));
        } else if (path.startsWith("file:")) {
            return loadBytes(Paths.get(path.substring("file:".length())));
        } else if (path.startsWith("http:") || path.startsWith("https:")) {
            return loadBytes(new URL(path));
        } else {
            return loadBytes(path);
        }
    }


    /**
     * The million'th implementation of the string "isEmpty" function
     * @param str the string to check
     * @return if the string is empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isBlank();
    }


    /**
     * The million'th implementation of the string "isNotEmpty" function
     * @param str the string to check
     * @return if the string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }


    /**
     * Checks that all objects in the list are null
     * @param objects the objects to check
     * @return if all are null
     */
    public static boolean allNull(Object... objects) {
        return objects == null || Arrays.stream(objects).allMatch(Objects::isNull);
    }


    /**
     * Checks that all objects in the list are non-null
     * @param objects the objects to check
     * @return if all are non-null
     */
    public static boolean allNonNull(Object... objects) {
        return objects == null || Arrays.stream(objects).noneMatch(Objects::isNull);
    }


    /**
     * Returns the file extension
     * @param name the file name, e.g. "test.pdf"
     * @return the file extension, e.g. "pdf"
     */
    public static String fileExtension(String name) {
        if (isEmpty(name) || !name.contains(".")) {
            return name;
        }
        return name.substring(name.lastIndexOf('.') + 1);
    }


    /**
     * Returns the file name without the file extension
     * @param name the file name, e.g. "test.pdf"
     * @return the file name without the file extension, e.g. "test"
     */
    public static String stripFileExtension(String name) {
        if (isEmpty(name)) {
            return name;
        }
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        return name;
    }


    /**
     * Returns the name with a different file extension
     * @param name the file name, e.g. "test.pdf"
     * @param extension the extension, e.g. "html"
     * @return the name with a different file extension, e.g. "test.html"
     */
    public static String fileName(String name, String extension) {
        if (isEmpty(extension) || isEmpty(name)) {
            return name;
        }
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        return stripFileExtension(name) + "." + extension;
    }


    /**
     * Return SignSDK version information.
     * Returns an empty map if no version information is found.
     * @return SignSDK version information
     */
    public static Properties versionInfo() {
        var versionProperties = new Properties();
        try {
            versionProperties.load(new ByteArrayInputStream(loadBytesByProtocol(VERSION_FILE)));
        } catch (Exception e) {
            LOG.trace("error ignored", e);
        }
        return versionProperties;
    }
}
