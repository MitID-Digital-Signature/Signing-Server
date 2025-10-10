package dk.gov.nemlogin.signing.broker.model;

import java.util.HashMap;

/** Simple representation of a JSON payload **/
public class JsonPayload extends HashMap<String, Object> {

    public static JsonPayload of(String name, Object value) {
        JsonPayload result = new JsonPayload();
        result.put(name, value);
        return result;
    }
}
