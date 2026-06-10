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

import com.easygoingapi.yoja.core.http.HttpParameter;
import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;

public class TestHttpUrl {

    @Test
    public void test_01() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .protocol(HttpProtocole.https)
                   .port(3333)
                   .path("/path_1/path_2")
                   .parameterQuery("name_1&name_2=value_2")
                   .fragment("fragment")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(3333, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals("/path_1/path_2", httpUrl.path());
        assertEquals("name_1&name_2=value_2", httpUrl.parameterQuery(Format.decoded));
        assertEquals("name_1&name_2=value_2", httpUrl.parameterQuery(Format.encoded));
        assertEquals("/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.pathAndQuery(Format.decoded));
        assertEquals("/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.pathAndQuery(Format.encoded));
        assertEquals("fragment", httpUrl.fragment(Format.decoded));
        assertEquals("fragment", httpUrl.fragment(Format.encoded));
        assertEquals("https://yojaHost:3333/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.url(Format.decoded));
        assertEquals("https://yojaHost:3333/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.url(Format.encoded));
    }
    
    @Test
    public void test_02() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .protocol(HttpProtocole.http)
                   .port(2222)
                   .path("path_1/path_2")
                   .parameterQuery("name_1&name_2=value_2")
                   .fragment("fragment")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(2222, httpUrl.port());
        assertEquals(HttpProtocole.http, httpUrl.protocol());
        assertEquals("/path_1/path_2", httpUrl.path());
        assertEquals("name_1&name_2=value_2", httpUrl.parameterQuery(Format.decoded));
        assertEquals("/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.pathAndQuery(Format.decoded));
        assertEquals("fragment", httpUrl.fragment(Format.decoded));
        assertEquals("http://yojaHost:2222/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_03() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .path("path_1/path_2")
                   .parameterQuery("name_1&name_2=value_2")
                   .fragment("fragment")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(null, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals("/path_1/path_2", httpUrl.path());
        assertEquals("name_1&name_2=value_2", httpUrl.parameterQuery(Format.decoded));
        assertEquals("/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.pathAndQuery(Format.decoded));
        assertEquals("fragment", httpUrl.fragment(Format.decoded));
        assertEquals("https://yojaHost/path_1/path_2?name_1&name_2=value_2#fragment", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_04() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .path("path_1/path_2")
                   .parameterQuery("name_1&name_2=value_2")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(null, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals("/path_1/path_2", httpUrl.path());
        assertEquals("name_1&name_2=value_2", httpUrl.parameterQuery(Format.decoded));
        assertEquals("/path_1/path_2?name_1&name_2=value_2", httpUrl.pathAndQuery(Format.decoded));
        assertEquals(null, httpUrl.fragment(Format.decoded));
        assertEquals("https://yojaHost/path_1/path_2?name_1&name_2=value_2", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_05() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .path("path_1/path_2")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(null, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals("/path_1/path_2", httpUrl.path());
        assertEquals(null, httpUrl.parameterQuery(Format.decoded));
        assertEquals("/path_1/path_2", httpUrl.pathAndQuery(Format.decoded));
        assertEquals(null, httpUrl.fragment(Format.decoded));
        assertEquals("https://yojaHost/path_1/path_2", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_06() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .path("path_1/path_2")
                   .fragment("fragment")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(null, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals("/path_1/path_2", httpUrl.path());
        assertEquals(null, httpUrl.parameterQuery(Format.decoded));
        assertEquals("/path_1/path_2#fragment", httpUrl.pathAndQuery(Format.decoded));
        assertEquals("fragment", httpUrl.fragment(Format.decoded));
        assertEquals("https://yojaHost/path_1/path_2#fragment", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_07() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .fragment("fragment")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(null, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals(null, httpUrl.parameterQuery(Format.decoded));
        assertEquals("/#fragment", httpUrl.pathAndQuery(Format.decoded));
        assertEquals("fragment", httpUrl.fragment(Format.decoded));
        assertEquals("https://yojaHost/#fragment", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_08() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(null, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals(null, httpUrl.parameterQuery(Format.decoded));
        assertEquals("/", httpUrl.pathAndQuery(Format.decoded));
        assertEquals(null, httpUrl.fragment(Format.decoded));
        assertEquals("https://yojaHost/", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_09() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .parameterQuery("name_1&name_2=value_2")
                   .build();
        assertEquals("yojaHost", httpUrl.host());
        assertEquals(null, httpUrl.port());
        assertEquals(HttpProtocole.https, httpUrl.protocol());
        assertEquals("/", httpUrl.path());
        assertEquals("name_1&name_2=value_2", httpUrl.parameterQuery(Format.decoded));
        assertEquals("/?name_1&name_2=value_2", httpUrl.pathAndQuery(Format.decoded));
        assertEquals(null, httpUrl.fragment(Format.decoded));
        assertEquals("https://yojaHost/?name_1&name_2=value_2", httpUrl.url(Format.decoded));
    }

    @Test
    public void test_10() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .parameterQuery("name_1_%3D=name_2_%3F&name_2_%3F=value_2_%C3%A7%C3%A0%C3%A9%C3%A8%C3%B9")
                   .build();
        assertEquals("name_1_==name_2_?&name_2_?=value_2_çàéèù", httpUrl.parameterQuery(Format.decoded));
        assertEquals("name_1_%3D=name_2_%3F&name_2_%3F=value_2_%C3%A7%C3%A0%C3%A9%C3%A8%C3%B9", httpUrl.parameterQuery(Format.encoded));
        assertEquals("https://yojaHost/?name_1_==name_2_?&name_2_?=value_2_çàéèù", httpUrl.url(Format.decoded));
        assertEquals("https://yojaHost/?name_1_%3D=name_2_%3F&name_2_%3F=value_2_%C3%A7%C3%A0%C3%A9%C3%A8%C3%B9", httpUrl.url(Format.encoded));
    }
    
    @Test
    public void test_11() {
        final HttpParameter httpParameter = new HttpParameter();
        httpParameter.putEntry("name_1_=", "name_2_?");
        httpParameter.putEntry("name_2_?", "value_2_çàéèù");
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .parameter(httpParameter)
                   .build();
        assertEquals("name_1_==name_2_?&name_2_?=value_2_çàéèù", httpUrl.parameterQuery(Format.decoded));
        assertEquals("name_1_%3D=name_2_%3F&name_2_%3F=value_2_%C3%A7%C3%A0%C3%A9%C3%A8%C3%B9", httpUrl.parameterQuery(Format.encoded));
        assertEquals("https://yojaHost/?name_1_==name_2_?&name_2_?=value_2_çàéèù", httpUrl.url(Format.decoded));
        assertEquals("https://yojaHost/?name_1_%3D=name_2_%3F&name_2_%3F=value_2_%C3%A7%C3%A0%C3%A9%C3%A8%C3%B9", httpUrl.url(Format.encoded));
    }

    @Test
    public void test_12() {
        final HttpUrl httpUrl =
            HttpUrl.builder("yojaHost")
                   .fragment("fragment_?çàéèù=")
                   .build();
        assertEquals("fragment_?çàéèù=", httpUrl.fragment(Format.decoded));
        assertEquals("fragment_%3F%C3%A7%C3%A0%C3%A9%C3%A8%C3%B9%3D", httpUrl.fragment(Format.encoded));
        assertEquals("https://yojaHost/#fragment_?çàéèù=", httpUrl.url(Format.decoded));
        assertEquals("https://yojaHost/#fragment_%3F%C3%A7%C3%A0%C3%A9%C3%A8%C3%B9%3D", httpUrl.url(Format.encoded));
    }
    
}
