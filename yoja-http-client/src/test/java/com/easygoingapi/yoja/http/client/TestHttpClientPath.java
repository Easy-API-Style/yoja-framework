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
package com.easygoingapi.yoja.http.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The path a request is sent to, percent-encoded segment by segment (HttpClient#encodePath). */
public class TestHttpClientPath {

    @Test
    public void the_characters_a_path_holds_are_kept() {
        assertEquals("/api/v1/items/42", HttpClient.encodePath("/api/v1/items/42"));
        assertEquals("/a-b_c.d~e/!$&'()*+,;=:@", HttpClient.encodePath("/a-b_c.d~e/!$&'()*+,;=:@"));
    }

    @Test
    public void the_others_are_encoded_as_their_utf8_bytes() {
        assertEquals("/files/%C3%A9t%C3%A9%202026", HttpClient.encodePath("/files/été 2026"));
        assertEquals("/q/a%3Fb%23c", HttpClient.encodePath("/q/a?b#c"), "no query, no fragment out of a segment");
        assertEquals("/p/100%25", HttpClient.encodePath("/p/100%"), "a lone % is encoded");
        assertEquals("/%22quoted%22/%3Ctag%3E", HttpClient.encodePath("/\"quoted\"/<tag>"));
    }

    @Test
    public void a_path_given_encoded_is_not_encoded_twice() {
        assertEquals("/files/%C3%A9t%C3%A9%202026", HttpClient.encodePath("/files/%C3%A9t%C3%A9%202026"));
        assertEquals("/a%2Fb", HttpClient.encodePath("/a%2Fb"), "an encoded / stays inside its segment");
    }

    @Test
    public void no_path_is_an_empty_one() {
        assertEquals("", HttpClient.encodePath(null));
    }

}
