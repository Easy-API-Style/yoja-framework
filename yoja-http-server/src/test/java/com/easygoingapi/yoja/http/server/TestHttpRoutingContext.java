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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.Handler;

public class TestHttpRoutingContext {

    private static final int PORT = 9974;
    private static final String HOST = "localhost";

    private record Tag(String value) {}

    @Test
    public void test_data_webServiceType_and_sessionStore() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        final HttpSessionStore store = new HttpSessionStore("session.yoja", Duration.ofDays(1));

        final AtomicReference<WebService.Type> type = new AtomicReference<>();
        final AtomicReference<HttpSessionStore> storeRef = new AtomicReference<>();
        final AtomicReference<Tag> byClass = new AtomicReference<>();
        final AtomicReference<String> byMissingKey = new AtomicReference<>();
        final AtomicReference<Boolean> hasKey = new AtomicReference<>();
        final AtomicReference<Tag> removed = new AtomicReference<>();
        final AtomicReference<Tag> afterRemove = new AtomicReference<>();
        final AtomicReference<Boolean> failedBefore = new AtomicReference<>();

        final Handler<HttpRouting> h1 = v -> {
            v.putData(new Tag("v"));
            v.nextHandler();
        };
        final Handler<HttpRouting> h2 = v -> {
            failedBefore.set(v.failed());
            type.set(v.webServiceType());
            storeRef.set(v.sessionStore());
            byClass.set(v.getData(Tag.class));
            byMissingKey.set(v.getData("missing", "fallback"));
            hasKey.set(v.data().containsKey(Tag.class.getName()));
            removed.set(v.removeData(Tag.class.getName()));
            afterRemove.set(v.getData(Tag.class));
            v.response().send("done");
        };
        final HttpRouter router = HttpRouter.builder()
                                            .session(store)
                                            .webService(new WebService(HttpMethod.GET, "/ctx", h1, h2))
                                            .build();
        final HttpServer server = newHttpServer(false, PORT, router);
        try {
            assertEquals(200, awaitValue(client.send(HttpGet.of("/ctx"))).statusCode());

            assertEquals(false, failedBefore.get());
            assertEquals(WebService.Type.WebService, type.get());
            assertEquals(store, storeRef.get());
            assertEquals(new Tag("v"), byClass.get());
            assertEquals("fallback", byMissingKey.get());
            assertTrue(hasKey.get());
            assertEquals(new Tag("v"), removed.get());
            assertNull(afterRemove.get());
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_sessionStore_null_when_sessions_disabled() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        final AtomicReference<HttpSessionStore> storeRef = new AtomicReference<>();
        storeRef.set(store()); // sentinel non-null so we can detect it was overwritten

        final Handler<HttpRouting> h = v -> {
            storeRef.set(v.sessionStore());
            v.response().send("ok");
        };
        final HttpRouter router = HttpRouter.builder()
                                            .webService(new WebService(HttpMethod.GET, "/nosession", h))
                                            .build();
        final HttpServer server = newHttpServer(false, PORT, router);
        try {
            assertEquals(200, awaitValue(client.send(HttpGet.of("/nosession"))).statusCode());
            assertNull(storeRef.get(), "sessionStore() must be null when sessions are disabled");
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_fail_with_status_code() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        final Handler<HttpRouting> h = v -> v.fail(503, new RuntimeException("boom"));
        final HttpRouter router = HttpRouter.builder()
                                            .webService(new WebService(HttpMethod.GET, "/boom", h))
                                            .build();
        final HttpServer server = newHttpServer(false, PORT, router);
        try {
            assertEquals(503, awaitValue(client.send(HttpGet.of("/boom"))).statusCode());
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    private static HttpSessionStore store() {
        return new HttpSessionStore("sentinel.yoja", Duration.ofDays(1));
    }

}
