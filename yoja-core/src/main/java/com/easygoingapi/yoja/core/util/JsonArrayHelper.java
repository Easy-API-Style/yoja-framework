package com.easygoingapi.yoja.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonArrayHelper {
    
    private static JsonWriter JSON_WRITER = JsonWriter.builder().build();
    
    private static String pad(final String value) {
        return "\"" + value + "\"";
    }
    
    public static String encode(final JsonArray jsonArray) {
        final List<String> values = new ArrayList<>();
        if (jsonArray != null) {
            final Iterator<Object> iterator = jsonArray.iterator();
            while (iterator.hasNext()) {
                final Object value = iterator.next();
                if (value instanceof JsonArray v) {
                    values.add(encode(v)); 
                }
                else if (value instanceof JsonObject v) {
                    values.add(v.encode());
                }
                else if (value instanceof Boolean v) {
                    values.add(pad(v.toString()));
                }
                else if (value instanceof String v) {
                    values.add(pad(v.toString()));
                }
                else if (value instanceof Number v) {
                    values.add(pad(v.toString()));
                }
                else {
                    values.add(JSON_WRITER.write(value));
                }
            }
        }
        return "[" + String.join(",", values) + "]";
    }

}
