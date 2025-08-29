package dk.gov.nemlogin.signing.model;

import java.util.Locale;

/**
 * The language to use in the Signing client
 * <p>
 * Values defined verbatim according to the Signing flow specification.
 */
@SuppressWarnings("unused")
public enum Language {

    /** Danish **/
    da,

    /** English **/
    en,

    /** Kalaallisut - Greenlandic **/
    kl;

    /**
     * Returns the Language value for the given locale
     * @param locale the locale
     * @return the Language value for the given locale
     */
    public static Language valueOf(Locale locale) {

        if(locale != null){
            switch (locale.getLanguage()){
                case "da":
                    return da;
                case "en" :
                    return en;
                case "kl" :
                    return kl;
            }
        }

        return da;
    }
}
