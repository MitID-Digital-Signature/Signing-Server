package dk.gov.nemlogin.signing.html.validation;


import org.owasp.html.CssGrammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This code was taken from the NemID validation project
 * Validates CSS definition against a whitelist
 */
@SuppressWarnings("NullableProblems")
public final class CSSContentValidator {

    private Set<String> allowedStyles = new HashSet<>(Arrays.asList("color", "background", "background-color", "float", "overflow",
        "line-height", "position", "top", "bottom", "left", "right",
        "margin", "margin-right", "margin-top", "margin-left", "margin-bottom",
        "width", "height", "float", "clear", "display", "white-space"));

    /**
     * Contains property families, e.g. font-* and padding-*.
     */
    private Set<String> allowedFamilies = new HashSet<>(Arrays.asList("border", "font", "text", "list", "padding"));

    private Set<String> disallowedFunctions = new HashSet<>(Arrays.asList("url", "attr", "expression", "element"));

    /**
     * Validator entry point. Delegates to <code>parse(String)</code>
     *
     * @see #parse(String)
     */
    public String validate(String content) {
        String value = null;
        if (content != null) {
            value = parse(content);
        }
        return value;
    }

    class CSSTokenizer {
        private final StringTokenizer outer;

        public CSSTokenizer(String content) {
            this.outer = new StringTokenizer(content, "{", false);
        }

        public String parseTokens() {
            String errorText = null;

            if (!outer.hasMoreTokens()) {
                return null;
            }

            //Content is the body of a style element
            while (errorText == null && outer.hasMoreTokens()) {
                String token = outer.nextToken();

                if (token.indexOf('}') != -1) {
                    var end = token.substring(token.indexOf('}'));
                    token = token.substring(0, token.indexOf('}'));
                    if (end.toLowerCase().contains("/") || end.toLowerCase().contains("&#47;")) {
                        errorText = parseStyle(end);
                    }
                }
                if (errorText == null) {
                    errorText = parseStyle(token);
                }
            }

            return errorText;
        }

        private String parseStyle(String str) {
            String errorText = null;
            var outerStyle = new StringTokenizer(str, ";", false);

            if (outerStyle.hasMoreTokens()) {
                //Style has multiple definitions
                while (errorText == null && outerStyle.hasMoreTokens()) {
                    String token = outerStyle.nextToken();
                    errorText = validateStyleDefinition(token);
                }
            } else {
                errorText = validateStyleDefinition(str);
            }

            return errorText;
        }

        private String validateStyleDefinition(String str) {

            if (str == null || "".equals(str.trim())) {
                return null;
            }
            str = str.trim();

            if (str.indexOf(':') <= 0) {
                //Input is not a valid style definition
                return "'" + str + "' is not a valid style definition";
            }

            var name = str.substring(0, str.indexOf(':'));

            name = name.trim().toLowerCase();
            var value = str.substring(name.length());
            value = value.trim().toLowerCase();

            boolean allowed = allowedStyles.contains(name);

            if (!allowed) {
                allowed = allowedFamilies.contains(getFamily(name));
            }

            if (!allowed) {
                return "'" + name + "' is not an allowed style property";
            }

            // Validate value
            if (value.toLowerCase().contains("url")) {
                return "Urls are not allowed in styles - '" + name + "' is defined using an url";
            }

            final List<String> result = new ArrayList<>();

            CssGrammar.parsePropertyGroup(str, new CSSPropertyHandler(result));

            return !result.isEmpty() ? Arrays.toString(result.toArray()) : null;
        }

        private String getFamily(String name) {
            int dashindex = name.indexOf('-');
            if (dashindex == -1) {
                return name;
            }

            return name.substring(0, dashindex);
        }

        private class CSSPropertyHandler implements CssGrammar.PropertyHandler {
            private final List<String> result;

            public CSSPropertyHandler(List<String> result) {
                this.result = result;
            }

            @Override
            public void startProperty(String name) {
                name = name.toLowerCase();

                boolean allowed = allowedStyles.contains(name);

                if (!allowed) {
                    allowed = allowedFamilies.contains(getFamily(name));
                }

                if (!allowed) {
                    result.add("'" + name + "' is not an allowed style property");
                }
            }

            @Override
            public void url(String token) {
                result.add("Urls are not allowed in styles - '" + token + "' is defined using an url");
            }

            @Override
            public void startFunction(String token) {
                token = token.toLowerCase();

                int pos = token.indexOf('(');
                if (-1 < pos) {
                    token = token.substring(0, pos);
                }
                if (disallowedFunctions.contains(token)) {
                    result.add("Function type are not allowed in styles functions - '" + token + "'");
                }
            }

            @Override
            public void errorRecovery(String text) {
                result.add("Erronous formating is not allowed in styles - '" + text + "'");
            }

            @Override
            public void at(String token) {
                result.add("'@XXX' CSS instructions are not allowed in styles - '" + token + "'");
            }

            @Override
            public void comment(String text) {
                result.add("CSS comments are not allowed in styles - '" + text + "'");
            }

            @Override
            public void quantity(String token) {
                // no need to implement
            }

            @Override
            public void identifier(String name) {
                // no need to implement
            }

            @Override
            public void hash(String token) {
                // no need to implement
            }

            @Override
            public void quotedString(String token) {
                // no need to implement
            }

            @Override
            public void punctuation(String token) {
                // no need to implement
            }

            @Override
            public void endFunction(String token) {
                // no need to implement
            }

            @Override
            public void endProperty() {
                // no need to implement
            }

        }
    }

    /**
     * Starting point for CSS validation. This method accept single style definitions or stylesheets (multiple grouped styles)<br />
     * Input is divided into single style definitions and passed to <code>parseStyle(String)</code> method.
     *
     * @param content - CSS definition
     * @return Validation error OR <code>null</code> if no validation errors were found
     * @see CSSTokenizer#parseStyle(String)
     * @see CSSTokenizer#validateStyleDefinition(String)
     */
    private String parse(String content) {

        if (content.contains("@")) {
            return "'@XXX' CSS instructions are not allowed";
        } else if (content.contains("{")) {
            //Get rid of name of style
            content = content.substring(content.indexOf('{'));
        }

        return new CSSTokenizer(content).parseTokens();
    }

}
