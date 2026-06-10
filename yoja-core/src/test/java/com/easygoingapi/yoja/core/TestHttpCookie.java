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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpCookie;

import io.vertx.core.http.CookieSameSite;

public class TestHttpCookie {

    @Test
    public void test_01() {
        final HttpCookie cookie = HttpCookie.of("session", "abc123");
        assertEquals("session", cookie.getName());
        assertEquals("abc123", cookie.getValue());
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
        assertEquals(0L, cookie.getMaxAge());
        assertNull(cookie.getSameSite());
        assertFalse(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
    }

    @Test
    public void test_02() {
        final HttpCookie cookie = HttpCookie.builder("session", "abc123")
                                            .domain("example.com")
                                            .path("/")
                                            .maxAge(3600L)
                                            .sameSite(CookieSameSite.STRICT)
                                            .httpOnly(true)
                                            .secure(true)
                                            .build();
        assertEquals("session", cookie.getName());
        assertEquals("abc123", cookie.getValue());
        assertEquals("example.com", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertEquals(3600L, cookie.getMaxAge());
        assertEquals(CookieSameSite.STRICT, cookie.getSameSite());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }

    @Test
    public void test_03() {
        final HttpCookie cookie = HttpCookie.of("name", "value");
        assertEquals(cookie, cookie);
    }

    @Test
    public void test_04() {
        final HttpCookie cookie = HttpCookie.of("name", "value");
        assertNotEquals(cookie, null);
        assertNotEquals(cookie, "name");
    }

    @Test
    public void test_05() {
        final HttpCookie c1 = HttpCookie.builder("session", "value_1")
                                        .domain("example.com")
                                        .path("/")
                                        .build();
        final HttpCookie c2 = HttpCookie.builder("session", "value_2")
                                        .domain("example.com")
                                        .path("/")
                                        .build();
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void test_06() {
        final HttpCookie c1 = HttpCookie.builder("a", "v")
                                        .domain("example.com")
                                        .path("/")
                                        .maxAge(100L)
                                        .httpOnly(true)
                                        .secure(true)
                                        .sameSite(CookieSameSite.STRICT)
                                        .build();
        final HttpCookie c2 = HttpCookie.builder("a", "v")
                                        .domain("example.com")
                                        .path("/")
                                        .maxAge(999L)
                                        .httpOnly(false)
                                        .secure(false)
                                        .sameSite(CookieSameSite.LAX)
                                        .build();
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void test_07() {
        final HttpCookie c1 = HttpCookie.of("a", "v");
        final HttpCookie c2 = HttpCookie.of("b", "v");
        assertNotEquals(c1, c2);
    }

    @Test
    public void test_08() {
        final HttpCookie c1 = HttpCookie.builder("a", "v").domain("example.com").build();
        final HttpCookie c2 = HttpCookie.builder("a", "v").domain("other.com").build();
        assertNotEquals(c1, c2);
    }

    @Test
    public void test_09() {
        final HttpCookie c1 = HttpCookie.builder("a", "v").path("/foo").build();
        final HttpCookie c2 = HttpCookie.builder("a", "v").path("/bar").build();
        assertNotEquals(c1, c2);
    }

    @Test
    public void test_10() {
        final HttpCookie c1 = HttpCookie.of("a", "v");
        final HttpCookie c2 = HttpCookie.of("a", "v");
        assertEquals(0, c1.compareTo(c2));
    }

    @Test
    public void test_11() {
        final HttpCookie a = HttpCookie.of("a", "v");
        final HttpCookie b = HttpCookie.of("b", "v");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void test_12() {
        final HttpCookie c1 = HttpCookie.builder("a", "v").domain("alpha.com").build();
        final HttpCookie c2 = HttpCookie.builder("a", "v").domain("beta.com").build();
        assertTrue(c1.compareTo(c2) < 0);
        assertTrue(c2.compareTo(c1) > 0);
    }

    @Test
    public void test_13() {
        final HttpCookie c1 = HttpCookie.builder("a", "v")
                                        .domain("example.com")
                                        .path("/aaa")
                                        .build();
        final HttpCookie c2 = HttpCookie.builder("a", "v")
                                        .domain("example.com")
                                        .path("/bbb")
                                        .build();
        assertTrue(c1.compareTo(c2) < 0);
        assertTrue(c2.compareTo(c1) > 0);
    }

    @Test
    public void test_14() {
        final HttpCookie withNullName = HttpCookie.of(null, "v");
        final HttpCookie withName = HttpCookie.of("a", "v");
        assertTrue(withNullName.compareTo(withName) < 0);
        assertTrue(withName.compareTo(withNullName) > 0);
    }

    @Test
    public void test_15() {
        final HttpCookie withNullDomain = HttpCookie.builder("a", "v").build();
        final HttpCookie withDomain = HttpCookie.builder("a", "v").domain("example.com").build();
        assertTrue(withNullDomain.compareTo(withDomain) < 0);
        assertTrue(withDomain.compareTo(withNullDomain) > 0);
    }

    @Test
    public void test_16() {
        final HttpCookie cookie = HttpCookie.of("session", "abc");
        final String s = cookie.toString();
        assertTrue(s.contains("name=session"));
        assertTrue(s.contains("value=abc"));
        assertTrue(s.contains("maxAge=0"));
        assertTrue(s.contains("httpOnly=false"));
        assertTrue(s.contains("secure=false"));
        assertFalse(s.contains("path="));
    }

    @Test
    public void test_17() {
        final HttpCookie cookie = HttpCookie.builder("session", "abc")
                                            .domain("example.com")
                                            .path("/api")
                                            .build();
        final String s = cookie.toString();
        assertTrue(s.contains("path=/api"));
        assertTrue(s.contains("example.com"));
    }

}
