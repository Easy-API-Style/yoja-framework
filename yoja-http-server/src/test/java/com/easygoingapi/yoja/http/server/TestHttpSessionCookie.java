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
package com.easygoingapi.yoja.http.server;

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static com.easygoingapi.yoja.http.server.util.TestUtil.newHttpClient;
import static com.easygoingapi.yoja.http.server.util.TestUtil.newHttpServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.http.CookieSameSite;

public class TestHttpSessionCookie {

    /**
     * Sends a request to a server holding the given session store.
     *
     * @return the {@code Set-Cookie} header of the response, lower-cased
     */
    private static String setCookieOf(final HttpSessionStore httpSessionStore) {
        final HttpClient httpClient = newHttpClient();
        final WebService webService = new WebService(HttpMethod.GET,
                                                     "/webService_1",
                                                     h -> {
                                                         h.session();
                                                         h.response().send();
                                                     });
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .session(httpSessionStore)
                      .webService(webService)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpResponse httpResponse = awaitValue(httpClient.send(HttpGet.of("/webService_1")));
            assertEquals(200, httpResponse.statusCode());
            return httpResponse.header("set-cookie").toLowerCase();
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_defaults() {
        final HttpSessionStore httpSessionStore = new HttpSessionStore("session.yoja", Duration.ofDays(1));
        assertTrue(httpSessionStore.httpOnly());
        assertTrue(httpSessionStore.secure());
        assertEquals(CookieSameSite.STRICT, httpSessionStore.sameSite());
        assertEquals("/", httpSessionStore.path());
        assertNull(httpSessionStore.maxAge());
        final String setCookie = setCookieOf(httpSessionStore);
        assertTrue(setCookie.startsWith("session.yoja="), setCookie);
        assertTrue(setCookie.contains("; httponly"), setCookie);
        assertTrue(setCookie.contains("; secure"), setCookie);
        assertTrue(setCookie.contains("; samesite=strict"), setCookie);
        assertTrue(setCookie.contains("; path=/"), setCookie);
        assertFalse(setCookie.contains("max-age"), setCookie);
        assertFalse(setCookie.contains("domain"), setCookie);
    }

    @Test
    public void test_builder() {
        final HttpSessionStore httpSessionStore = HttpSessionStore.builder("session.yoja")
                                                                  .timeout(Duration.ofHours(1))
                                                                  .httpOnly(false)
                                                                  .secure(false)
                                                                  .sameSite(CookieSameSite.LAX)
                                                                  .path("/app")
                                                                  .maxAge(Duration.ofDays(7))
                                                                  .build();
        assertEquals(Duration.ofHours(1), httpSessionStore.timeout());
        final String setCookie = setCookieOf(httpSessionStore);
        assertFalse(setCookie.contains("httponly"), setCookie);
        assertFalse(setCookie.contains("secure"), setCookie);
        assertTrue(setCookie.contains("; samesite=lax"), setCookie);
        assertTrue(setCookie.contains("; path=/app"), setCookie);
        assertTrue(setCookie.contains("; max-age=604800"), setCookie);
    }

    @Test
    public void test_validation() {
        assertThrows(IllegalArgumentException.class, () -> HttpSessionStore.builder(" ").build());
        assertThrows(IllegalArgumentException.class, () -> HttpSessionStore.builder("s")
                                                                           .timeout(Duration.ZERO)
                                                                           .build());
        assertThrows(IllegalArgumentException.class, () -> HttpSessionStore.builder("s")
                                                                           .path("app")
                                                                           .build());
        assertThrows(IllegalArgumentException.class, () -> HttpSessionStore.builder("s")
                                                                           .sameSite(CookieSameSite.NONE)
                                                                           .secure(false)
                                                                           .build());
        assertThrows(IllegalArgumentException.class, () -> HttpSessionStore.builder("__Secure-s")
                                                                           .secure(false)
                                                                           .build());
        assertThrows(IllegalArgumentException.class, () -> HttpSessionStore.builder("__Host-s")
                                                                           .path("/app")
                                                                           .build());
    }

}
