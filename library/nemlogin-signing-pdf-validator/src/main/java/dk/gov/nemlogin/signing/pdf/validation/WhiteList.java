package dk.gov.nemlogin.signing.pdf.validation;

import org.apache.pdfbox.cos.COSName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Holding the whitelists
 * <p>
 * sonar suppression:
 * - "Names of regular expressions named groups should be used" (regex)
 * - "Generic exceptions should never be thrown"
 */
@SuppressWarnings({"java:S5860","java:S112"})
public final class WhiteList {

    private static final Logger LOG = LoggerFactory.getLogger(WhiteList.class);
    private static final Pattern PDF_NAME_PATTERN = Pattern.compile("^/(?<name>[a-zA-Z0-9.\\-_\\\\+]+)");
    private static final Pattern PDF_REGEX_NAME_PATTERN = Pattern.compile("^\\{regex}/(?<name>[a-zA-Z0-9.\\-_\\\\+]+)");

    // Whitelists
    private static final Set<String> EXCLUSIONS = load("pdf/whitelistexclusions.txt");
    private static final Set<String> KEYS = load("pdf/whitelistkeys.txt");
    private static final Set<String> NAMES = load("pdf/whitelist.txt");
    protected static final Set<Pattern> NAMESREGEX = load("pdf/whitelist.txt", PDF_REGEX_NAME_PATTERN)
        .stream()
        .map(Pattern::compile)
        .collect(Collectors.toSet());

    private WhiteList() {
    }

    /**
     * Returns whether the given PDF key is excluded from whitelist checking
     *
     * @param key the key to check
     * @return if the given PDF key is excluded from whitelist checking
     */
    public static boolean isKeyExcluded(final COSName key) {
        return EXCLUSIONS.contains(key.getName());
    }


    /**
     * Returns whether the given PDF object name is in the whitelist
     *
     * @param name the name to check
     * @return if the given PDF object name is in the whitelist
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isNameWhitelisted(final COSName name) {
        return NAMES.contains(name.getName()) ||
            NAMESREGEX.stream().anyMatch(p -> p.matcher(name.getName()).matches());
    }


    /**
     * Returns whether the given PDF key is in the whitelist
     *
     * @param key the name to check
     * @return if the given PDF key is in the whitelist
     */
    public static boolean isKeyWhitelisted(final COSName key) {
        return KEYS.contains(key.getName());
    }


    /**
     * Loads the whitelist resource
     *
     * @param resourceName the classpath to the resource
     * @return the whitelist
     */
    private static Set<String> load(String resourceName) {
        return load(resourceName, PDF_NAME_PATTERN);
    }


    /**
     * Loads the whitelist resource
     *
     * @param resourceName the classpath to the resource
     * @param namePattern  the name pattern to match
     * @return the whitelist
     */
    private static Set<String> load(String resourceName, Pattern namePattern) {
        Set<String> strings = new HashSet<>();

        try (var inputStream = WhiteList.class.getClassLoader().getResourceAsStream(resourceName)) {
            var sc = new Scanner(Objects.requireNonNull(inputStream));
            while (sc.hasNextLine()) {
                var matcher = namePattern.matcher(sc.nextLine());
                if (matcher.matches()) {
                    strings.add(matcher.group("name"));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading PDF whitelist " + resourceName, e);
        }
        LOG.debug("Loaded PDF whitelist from {} containing {} entries", resourceName, strings.size());
        return strings;
    }
}
