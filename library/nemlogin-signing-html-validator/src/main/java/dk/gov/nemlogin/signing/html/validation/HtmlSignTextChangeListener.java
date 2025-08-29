package dk.gov.nemlogin.signing.html.validation;

import org.owasp.html.HtmlChangeListener;

import java.util.Arrays;

/**
 * This class is registering HTML sanitizers changes - things changed are things that does not validate
 */
@SuppressWarnings("NullableProblems")
public class HtmlSignTextChangeListener implements HtmlChangeListener<Object> {

    /**
     * Map of discarded tag names (key) and discarded attribute names (value)
     */
    private final StringBuilder discarded;
    private static final String DELIMITER = "; ";

    public StringBuilder getDiscarded() {
        return discarded;
    }

    public HtmlSignTextChangeListener() {
        discarded = new StringBuilder();
    }

    @Override
    public void discardedAttributes(Object context, String tagName, String... attributeNames) {
        discarded.append(tagName).append(Arrays.asList(attributeNames)).append(DELIMITER);
    }

    @Override
    public void discardedTag(Object context, String tagName) {
        discarded.append(tagName).append(DELIMITER);
    }

}
