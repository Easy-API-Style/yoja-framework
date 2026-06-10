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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.YojaAppException;
import com.easygoingapi.yoja.core.http.HttpParameter;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;

public class TestHttpParameter {

    @Test
    public void test_01() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntry("test_01", "value_test_01")
                     .putEntry("test_02", "value_test_02");
        assertEquals(true, httpParameter.hasName("test_01"));
        assertEquals(false, httpParameter.hasName("TEST_01"));
        assertEquals(true, httpParameter.hasName("test_02"));
        assertEquals(false, httpParameter.hasName("test_01_aaa"));
        assertEquals(List.of("value_test_01"), httpParameter.values("test_01"));
        assertEquals(List.of(), httpParameter.values("TEST_01"));
        assertEquals(List.of("value_test_02"), httpParameter.values("test_02"));
        assertEquals(List.of(), httpParameter.values("test_01_aaa"));
        assertEquals(false, httpParameter.isEmpty());
        assertEquals(Set.of("test_01", "test_02"), httpParameter.names());
        httpParameter.removeEntries("test_02");
        assertEquals(Set.of("test_01"), httpParameter.names());
        httpParameter.removeEntries("TEST_01");
        assertEquals(false, httpParameter.isEmpty());
        httpParameter.removeEntries("test_01");
        assertEquals(true, httpParameter.isEmpty());
        assertEquals(Set.of(), httpParameter.names());
    }
    
    @Test
    public void test_02() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntry("test_02", "value_1");
        assertEquals(1, httpParameter.size());
        assertEquals(List.of("value_1"), httpParameter.values("test_02"));
        httpParameter.addEntry("test_02", "value_2");
        assertEquals(2, httpParameter.size());
        assertEquals(List.of("value_1", "value_2"), httpParameter.values("test_02"));
        httpParameter.putEntries("test_02", List.of("value_1", "value_2", "value_3"));
        assertEquals(List.of("value_1", "value_2", "value_3"), httpParameter.values("test_02"));
        assertEquals(3, httpParameter.size());
        assertEquals(false, httpParameter.isEmpty());
    }
    
    @Test
    public void test_03() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1", List.of("value_1", "value_2", "value_3"));
        httpParameter.putEntry("name_2", "value_4");
        assertEquals("name_1=value_1&name_1=value_2&name_1=value_3&name_2=value_4", httpParameter.parameterQuery(Format.decoded));
        assertEquals("name_1=value_1&name_1=value_2&name_1=value_3&name_2=value_4", httpParameter.parameterQuery(Format.encoded));
        
        httpParameter.clear();
        assertEquals(true, httpParameter.isEmpty());
        
        httpParameter.putEntry("name_2", "value_4");
        httpParameter.putEntries("name_1", List.of("value_1", "value_2", "value_3"));
        assertEquals("name_2=value_4&name_1=value_1&name_1=value_2&name_1=value_3", httpParameter.parameterQuery(Format.decoded));
        assertEquals("name_2=value_4&name_1=value_1&name_1=value_2&name_1=value_3", httpParameter.parameterQuery(Format.encoded));
    }
    
    @Test
    public void test_04() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1", List.of("value_1_é", "value_2_=", "value_3_?"));
        httpParameter.putEntry("name_2", "value_4");
        assertEquals("name_1=value_1_%C3%A9&name_1=value_2_%3D&name_1=value_3_%3F&name_2=value_4", httpParameter.parameterQuery(Format.encoded));
        assertEquals("name_1=value_1_é&name_1=value_2_=&name_1=value_3_?&name_2=value_4", httpParameter.parameterQuery(Format.decoded));
    }
    
    @Test
    public void test_05() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1_à", List.of("value_1_é", "value_2_=", "value_3_?"));
        httpParameter.putEntry("name_2_ç", "value_4");
        assertEquals("name_1_%C3%A0=value_1_%C3%A9&name_1_%C3%A0=value_2_%3D&name_1_%C3%A0=value_3_%3F&name_2_%C3%A7=value_4", httpParameter.parameterQuery(Format.encoded));
        assertEquals("name_1_à=value_1_é&name_1_à=value_2_=&name_1_à=value_3_?&name_2_ç=value_4", httpParameter.parameterQuery(Format.decoded));
    }
    
    @Test
    public void test_06() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1", List.of("value_1", "value_2", "value_3"));
        httpParameter.putEntry("name_2", null);
        assertEquals("name_1=value_1&name_1=value_2&name_1=value_3&name_2", httpParameter.parameterQuery(Format.decoded));
    }
    
    @Test
    public void test_07() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1", List.of("value_1", "value_2", "value_3"));
        httpParameter.addEntry("name_1", null);
        assertEquals("name_1=value_1&name_1=value_2&name_1=value_3&name_1", httpParameter.parameterQuery(Format.decoded));
    }
    
    @Test
    public void test_08() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1", List.of("value_1", "value_2", "value_3"));
        httpParameter.addEntry("name_1", null);
        httpParameter.addEntry("name_1", null);
        assertEquals("name_1=value_1&name_1=value_2&name_1=value_3&name_1&name_1", httpParameter.parameterQuery(Format.decoded));
    }
    
    @Test
    public void test_09() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1", List.of("value_1", "value_2", "value_3"));
        httpParameter.addEntries("name_1", List.of("value_4", "value_5", "value_6"));
        assertEquals("name_1=value_1&name_1=value_2&name_1=value_3&name_1=value_4&name_1=value_5&name_1=value_6", httpParameter.parameterQuery(Format.decoded));
    }
    
    @Test
    public void test_10() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_1", List.of("value_1", "value_2", "value_3"));
        httpParameter.putEntry("name_1", null);
        assertEquals("name_1", httpParameter.parameterQuery(Format.decoded));
    }

    @Test
    public void test_11() {
        Exception exception = null;
        try {
            final HttpParameter httpParameter = new HttpParameter();
            assertEquals(true, httpParameter.isEmpty());
            httpParameter.putEntries("", null);
        }
        catch (final Exception e) {
            exception = e;
        }
        assertEquals(YojaAppException.class, exception.getClass());
        assertEquals("http name paramter non blank", exception.getMessage());
    }

    @Test
    public void test_12() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntries("name_2", List.of("value_1", "value_2", "value_3"));
        httpParameter.putEntry("name_2");
        assertEquals("name_2", httpParameter.parameterQuery(Format.decoded));
    }

    @Test
    public void test_13() {
        final HttpParameter httpParameter = new HttpParameter();
        assertEquals(true, httpParameter.isEmpty());
        httpParameter.putEntry("name_2");
        assertEquals("name_2", httpParameter.parameterQuery(Format.decoded));
    }

    @Test
    public void test_14() {
        final HttpParameter httpParameter = HttpParameter.parse("name_1&name_2=value_2");
        assertEquals(false, httpParameter.isEmpty());
        assertEquals(true, httpParameter.hasName("name_1"));
        assertEquals(true, httpParameter.hasName("name_2"));
        assertEquals(List.of(), httpParameter.values("name_1"));
        assertEquals(List.of("value_2"), httpParameter.values("name_2"));
    }

    @Test
    public void test_15() {
        final HttpParameter httpParameter = HttpParameter.parse("name_1&name_2=value_2#fragment");
        assertEquals(false, httpParameter.isEmpty());
        assertEquals(true, httpParameter.hasName("name_1"));
        assertEquals(true, httpParameter.hasName("name_2"));
        assertEquals(List.of(), httpParameter.values("name_1"));
        assertEquals(List.of("value_2"), httpParameter.values("name_2"));
    }

    @Test
    public void test_16() {
        final HttpParameter httpParameter = HttpParameter.parse("#fragment");
        assertEquals(true, httpParameter.isEmpty());
    }
    
    @Test
    public void test_17() {
        final HttpParameter httpParameter = HttpParameter.parse("name_1_%C3%A0=value_1_%C3%A9&name_1_%C3%A0=value_2_%3D&name_1_%C3%A0=value_3_%3F&name_2_%C3%A7=value_4");
        assertEquals(true, httpParameter.hasName("name_1_à"));
        assertEquals(true, httpParameter.hasName("name_2_ç"));
        assertEquals(List.of("value_1_é", "value_2_=", "value_3_?"), httpParameter.values("name_1_à"));
        assertEquals(List.of("value_4"), httpParameter.values("name_2_ç"));
        assertEquals(false, httpParameter.isEmpty());
    }

    @Test
    public void test_18() {
        final HttpParameter httpParameter = HttpParameter.parse("&name_2=&name_1&name_2=value_2&name_2=value_3");
        assertEquals(false, httpParameter.isEmpty());
        assertEquals(true, httpParameter.hasName("name_1"));
        assertEquals(true, httpParameter.hasName("name_2"));
        assertEquals(List.of(), httpParameter.values("name_1"));
        assertEquals(List.of("value_2", "value_3"), httpParameter.values("name_2"));
        assertEquals(4, httpParameter.size());
        assertEquals("value_2", httpParameter.firstValue("name_2"));
    }

    @Test
    public void test_19() {
        final HttpParameter httpParameter = HttpParameter.parse("&name_2&name_1&name_2=value_2&name_2=value_2");
        assertEquals(false, httpParameter.isEmpty());
        assertEquals(true, httpParameter.hasName("name_1"));
        assertEquals(true, httpParameter.hasName("name_2"));
        assertEquals(List.of(), httpParameter.values("name_1"));
        assertEquals(List.of("value_2", "value_2"), httpParameter.values("name_2"));
        assertEquals(4, httpParameter.size());
        assertEquals("value_2", httpParameter.firstValue("name_2"));
    }
    
}
