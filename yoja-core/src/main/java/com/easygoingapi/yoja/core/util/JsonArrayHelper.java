/*
 * Copyright 2026 easy api <easy.api.contact@gmail.com>
 * https://easygoingapi.com
 * https://github.com/Easy-API-Style/yoja-framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.easygoingapi.yoja.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Encodes a {@link JsonArray} to its JSON string form, serializing each element
 * by its runtime type (nested arrays/objects recursively, scalars as quoted
 * values, anything else via {@link JsonWriter}).
 */
public class JsonArrayHelper {

    /** Not instantiable. */
    private JsonArrayHelper() {}

    /**
     * Encodes the given array to a JSON string.
     * <p>
     * Nested {@link JsonObject}/{@link JsonArray} are encoded recursively;
     * every other element (strings — properly escaped, numbers, booleans,
     * {@code null}, arbitrary POJOs) is serialized with Jackson so it produces
     * a valid JSON literal.
     *
     * @param jsonArray array to encode (a {@code null} array yields {@code "[]"})
     * @return the JSON string representation of {@code jsonArray}
     */
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
                else {
                    values.add(JsonWriter.defaultWriter().write(value));
                }
            }
        }
        return "[" + String.join(",", values) + "]";
    }

}
