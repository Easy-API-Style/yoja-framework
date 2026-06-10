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

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpHeader;

public class TestHttpHeader {

    @Test
    public void test_01() {
        final HttpHeader httpHeader = new HttpHeader();
        assertEquals(true, httpHeader.isEmpty());
        httpHeader.put("test_01", "value_test_01")
                  .put("test_02", "value_test_02");
        assertEquals(true, httpHeader.has("test_01"));
        assertEquals(true, httpHeader.has("TEST_01"));
        assertEquals(true, httpHeader.has("test_02"));
        assertEquals(false, httpHeader.has("test_01_aaa"));
        assertEquals("value_test_01", httpHeader.value("test_01"));
        assertEquals("value_test_01", httpHeader.value("TEST_01"));
        assertEquals("value_test_02", httpHeader.value("test_02"));
        assertEquals(null, httpHeader.value("test_01_aaa"));
        assertEquals(false, httpHeader.isEmpty());
        assertEquals(Set.of("test_01", "test_02"), httpHeader.names());
        httpHeader.remove("test_02");
        assertEquals(Set.of("test_01"), httpHeader.names());
        httpHeader.remove("TEST_01");
        assertEquals(true, httpHeader.isEmpty());
        assertEquals(Set.of(), httpHeader.names());
    }
    
}
