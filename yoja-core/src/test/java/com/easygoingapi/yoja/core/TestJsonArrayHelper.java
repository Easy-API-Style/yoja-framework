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
package com.easygoingapi.yoja.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.util.JsonArrayHelper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TestJsonArrayHelper {

    @Test
    public void test_null_is_empty_array() {
        assertEquals("[]", JsonArrayHelper.encode(null));
        assertEquals("[]", JsonArrayHelper.encode(new JsonArray()));
    }

    @Test
    public void test_strings() {
        assertEquals("[\"a\",\"b\"]", JsonArrayHelper.encode(new JsonArray().add("a").add("b")));
    }

    @Test
    public void test_string_with_special_chars_is_escaped() {
        // a string containing a quote and a backslash must be JSON-escaped
        final JsonArray array = new JsonArray().add("he said \"hi\"\\");
        final String encoded = JsonArrayHelper.encode(array);
        // must round-trip back to the same array
        assertEquals(array, new JsonArray(encoded));
    }

    @Test
    public void test_numbers_are_not_quoted() {
        assertEquals("[42,3.5]", JsonArrayHelper.encode(new JsonArray().add(42).add(3.5)));
    }

    @Test
    public void test_booleans_are_not_quoted() {
        assertEquals("[true,false]", JsonArrayHelper.encode(new JsonArray().add(true).add(false)));
    }

    @Test
    public void test_nested_object_and_array() {
        final JsonArray array = new JsonArray()
                .add(new JsonObject().put("k", "v"))
                .add(new JsonArray().add(1));
        assertEquals(array, new JsonArray(JsonArrayHelper.encode(array)));
    }

}
