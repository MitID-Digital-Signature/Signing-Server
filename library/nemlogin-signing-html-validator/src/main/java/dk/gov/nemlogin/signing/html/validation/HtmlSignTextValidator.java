package dk.gov.nemlogin.signing.html.validation;

import org.jsoup.Jsoup;
import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlChangeReporter;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This code was originally taken from the NemID validation project
 * Validates HTML elements/attributes definition against a whitelist
 * <p>
 * sonar suppression has been added - to preserve the original code - and remove sonar scanning code smells
 */
@SuppressWarnings({"NullableProblems", "java:S1192", "java:S1066", "java:S1117", "java:S1132", "java:S1186", "java:S1166",
    "java:S139", "java:S1188", "java:S121"})
public class HtmlSignTextValidator {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlSignTextValidator.class);

    private String status;
    private final PolicyFactory policy;

    /**
     * Constructor
     */
    public HtmlSignTextValidator() {
        policy = new HtmlPolicyBuilder()
            .allowStandardUrlProtocols()
            .allowElements("html", "body", "head", "meta", "style", "title", "p", "div", "span", "ul", "ol", "li",
                "h1", "h2", "h3", "h4", "h5", "h6", "table", "tbody", "thead", "tfoot", "tr", "td", "th",
                "i", "b", "u", "center", "a", "br")
            .allowAttributes("xmlns").onElements("html")
            .allowAttributes("charset", "http-equiv", "name", "content").onElements("meta")
            .allowAttributes("text", "bgcolor", "class", "style").onElements("body")
            .allowAttributes("type").onElements("style")
            .allowTextIn("style")
            .allowAttributes("align", "bgcolor", "style", "class").onElements("p", "div", "span")
            .allowAttributes("style", "class").onElements("ul", "li", "h1", "h2", "h3", "h4", "h5", "h6")
            .allowAttributes("start", "type", "style", "class").onElements("ol")
            .allowAttributes("border", "cellspacing", "cellpadding", "width", "align", "style").onElements("table")
            .allowAttributes("bgcolor", "class", "style").onElements("tr")
            .allowAttributes("bgcolor", "rowspan", "colspan", "align", "valign", "width", "class", "style").onElements("th")
            .allowAttributes("bgcolor", "rowspan", "colspan", "align", "valign", "width", "class", "style").onElements("td")
            .allowAttributes("href", "name").matching(documentLinksOnly).onElements("a")
            .toFactory();
    }


    /**
     * Returns "OK" if the validation succeded otherwise "NOT OK" plus a list of illegal tag and attribute names e.g:
     * <code>NOT OK: html[badAttribute1, badAttribute2]; badTag; badTag;</code>
     * <p>
     * All though the html tag is a legal tag the badAttribute1 and badAttribute2 for that tag are not. The badTag is not
     * a legal tag and are stated the number of times it appear in the <code>untrustedHtml</code>
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Validates the <code>untrustedHtml</code> according to the requirements specified in the document
     * https://www.danidnet.dk/svn/clients/javascript/trunk/docs/Delivery/Design%20Documents/Integration/DanID%20integration.doc secton 5.2
     *
     * @param untrustedHTML html to validate
     * @return Returns true if the <code>untrustedHTML</code> exclusively contains the tags and attributes stated in
     * requirements whitelist otherwise false
     */
    public boolean validate(String untrustedHTML) {
        var htmlChangeListener = new HtmlSignTextChangeListener();
        HtmlChangeReporter<Object> reporter = new HtmlChangeReporter<>(multiReceivers, htmlChangeListener, null);

        final HtmlStreamEventReceiver wrappedRenderer = reporter.getWrappedRenderer();
        if (wrappedRenderer != null && policy!=null) {
            final HtmlSanitizer.Policy apply = this.policy.apply(wrappedRenderer);
            if(apply!=null) { // must be here because of findbugs
                reporter.setPolicy(apply);
            }
        }

        try {

            HtmlSanitizer.sanitize(untrustedHTML, reporter.getWrappedPolicy());

            if (htmlChangeListener.getDiscarded().length() == 0) {
                // Successful validation
                this.status = "HTML OK";
                LOG.debug("valid signtext: {}", untrustedHTML);
            } else {
                this.status = "HTML not valid: " + htmlChangeListener.getDiscarded().toString();
                LOG.debug("invalid signtext: {}", untrustedHTML);
            }
        } catch (CSSValidationException | AttributeValidationException e) {
            this.status = "HTML not valid: " + e.getMessage();
            LOG.debug("invalid signtext: {}", untrustedHTML, e);
            return false;
        }

        if (detectComments(untrustedHTML)) {
            this.status = "NOT OK: Contains comments";
            return false;
        }

        return htmlChangeListener.getDiscarded().length() == 0;
    }

    /**
     * Detect any comments
     * Comments are identified by the node name #comment.
     *
     * @param untrustedHTML html to be checked for comments
     */
    private boolean detectComments(String untrustedHTML) {
        var document = Jsoup.parse(untrustedHTML);
        return document.childNodes().stream().anyMatch(node -> node.nodeName().equals("#comment"));
    }

    private static final AttributePolicy documentLinksOnly = (elementName, attributeName, value) -> {
        if ("href".equals(attributeName)) {
            if (!value.startsWith("#")) {
                return null; // Any links that are not document-relative must be discarded.
            }
        }
        return value;
    };


    private final HtmlStreamEventReceiver multiReceivers = new HtmlStreamEventReceiver() {
        @Override
        public void openTag(String elementName, List<String> attrs) {
            singleAttrValidator.openTag(elementName, attrs);
            cssValidator.openTag(elementName, attrs);
        }

        @Override
        public void closeTag(String elementName) {
            cssValidator.closeTag(elementName);
        }

        @Override
        public void text(String text) {
            cssValidator.text(text);
        }

        @Override
        public void openDocument() {
        }

        @Override
        public void closeDocument() {
        }
    };

    /**
     * Check for duplicate attribute use.
     * <p>
     * There must never be two or more attributes on the same start tag whose names are an ASCII case-insensitive match for each other.
     */
    private final HtmlStreamEventReceiver singleAttrValidator = new HtmlStreamEventReceiver() {
        @Override
        public void openTag(String elementName, List<String> attrs) {
            for (var i = 0; i < attrs.size() - 1; i += 2) {
                String name = attrs.get(i);

                for (int j = i + 2; j < attrs.size(); j += 2) {
                    if (name.equalsIgnoreCase(attrs.get(j))) {
                        throw new AttributeValidationException("Duplicate use of attribute: '" + name + "' in element: '" + elementName + "'");
                    }
                }
            }
        }

        @Override
        public void openDocument() {
        }

        @Override
        public void closeDocument() {
        }

        @Override
        public void closeTag(String elementName) {
        }

        @Override
        public void text(String text) {
        }
    };

    /**
     * OWASP doesn't allow fine-grained control of CSS in style elements. This is provided here in the form of a renderer instead
     * which uses the {@link CSSContentValidator} class.
     */
    private final HtmlStreamEventReceiver cssValidator = new HtmlStreamEventReceiver() {
        private boolean insideStyleTag;

        @Override
        public void openTag(String elementName, List<String> attrs) {
            if ("style".equalsIgnoreCase(elementName)) {
                insideStyleTag = true;
            }

            for (var i = 0; i < attrs.size(); i++) {
                var name = attrs.get(i);
                if ("style".equalsIgnoreCase(name)) {
                    var cssValidator = new CSSContentValidator();
                    var validate = cssValidator.validate(attrs.get(i + 1));
                    if (validate != null)
                        throw new CSSValidationException(validate);
                }
            }
        }

        @Override
        public void closeTag(String elementName) {
            if ("style".equalsIgnoreCase(elementName)) {
                insideStyleTag = false;
            }
        }

        @Override
        public void text(String text) {
            if (insideStyleTag) {
                var cssValidator = new CSSContentValidator();
                var validate = cssValidator.validate(text);
                if (validate != null)
                    throw new CSSValidationException(validate);
            }
        }

        @Override
        public void openDocument() {
        }

        @Override
        public void closeDocument() {
        }
    };

    /**
     * Thrown in case illegal CSS is found inside a &lt;style&gt; element.
     */
    private static class CSSValidationException extends RuntimeException {
        public CSSValidationException(String message) {
            super(message);
        }
    }

    /**
     * Thrown in case duplicate attributes is found inside a &lt;style&gt; element.
     */
    private static class AttributeValidationException extends RuntimeException {
        public AttributeValidationException(String message) {
            super(message);
        }
    }
}
