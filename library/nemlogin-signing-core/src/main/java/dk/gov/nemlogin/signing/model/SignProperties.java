package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.util.SigningUtils;

import java.util.HashMap;

/**
 * For XAdES, the SignProperties are included in the XAdES DTBS.
 * <p>
 * The purpose is to provide similar functionality to the "SIGN_PROPERTIES"
 * parameter of the NemID signing client.
 */
@SuppressWarnings("unused")
public class SignProperties extends HashMap<String, SignProperties.SignPropertyValue<?>> {


    /**
     * Defines the types of property values
     */
    public interface  SignPropertyValue<T> {

        /**
         * Serializes the value for inclusion in XAdES DTBS
         * @return the serialized value for inclusion in XML DTBS
         */
        T serialize();

    }

    /**
     * Defines a String-based property value
     */
    public static class StringValue implements SignPropertyValue<String> {

        private final String value;

        /** Constructor **/
        public StringValue(String value) {
            this.value = value;
        }

        /** {@inheritDoc} **/
        @Override
        public String serialize() {
            return SigningUtils.isEmpty(value) ? "" : value;
        }
    }

    /**
     * Defines a binary property value
     */
    public static class BinaryValue implements SignPropertyValue<byte[]> {

        private final byte[] value;

        /** Constructor **/
        public BinaryValue(byte[] value) {
            this.value = value!=null ? value.clone() : new byte[0];
        }

        /** {@inheritDoc} **/
        @Override
        public byte[] serialize() {
            return value == null ? new byte[0] : value.clone();
        }
    }
}
