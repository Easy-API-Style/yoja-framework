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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.Handler;

public class TestHttpSessionLifecycle {

    private static final int PORT = 9975;
    private static final String HOST = "localhost";

    private record Ctx(String value) {}

    @Test
    public void test_session_isEmpty_timeout_destroy() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        final Duration timeout = Duration.ofMinutes(30);
        final HttpSessionStore store = new HttpSessionStore("session.yoja", timeout);

        final AtomicReference<Boolean> emptyBefore = new AtomicReference<>();
        final AtomicReference<Duration> timeoutValue = new AtomicReference<>();
        final AtomicReference<Boolean> emptyAfterPut = new AtomicReference<>();
        final AtomicReference<Boolean> destroyedBefore = new AtomicReference<>();
        final AtomicReference<Boolean> destroyedAfter = new AtomicReference<>();

        final Handler<HttpRouting> h = v -> {
            final HttpSession session = v.session();
            emptyBefore.set(session.isEmpty());
            timeoutValue.set(session.timeout());
            session.put(new Ctx("x"));
            emptyAfterPut.set(session.isEmpty());
            destroyedBefore.set(session.isDestroyed());
            session.destroy();
            destroyedAfter.set(session.isDestroyed());
            v.response().send("ok");
        };
        final HttpRouter router = HttpRouter.builder()
                                            .session(store)
                                            .webService(new WebService(HttpMethod.GET, "/session", h))
                                            .build();
        final HttpServer server = newHttpServer(false, PORT, router);
        try {
            assertEquals(200, awaitValue(client.send(HttpGet.of("/session"))).statusCode());

            assertEquals(true, emptyBefore.get());
            assertEquals(timeout, timeoutValue.get());
            assertEquals(false, emptyAfterPut.get());
            assertEquals(false, destroyedBefore.get());
            assertEquals(true, destroyedAfter.get());
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_store_size() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        final HttpSessionStore store = new HttpSessionStore("session.yoja", Duration.ofDays(1));

        final Handler<HttpRouting> h = v -> {
            v.session().put(new Ctx("keep"));
            v.response().send("ok");
        };
        final HttpRouter router = HttpRouter.builder()
                                            .session(store)
                                            .webService(new WebService(HttpMethod.GET, "/keep", h))
                                            .build();
        final HttpServer server = newHttpServer(false, PORT, router);
        try {
            assertEquals(0, awaitValue(store.size()));
            assertEquals(200, awaitValue(client.send(HttpGet.of("/keep"))).statusCode());
            assertEquals(1, awaitValue(store.size()));
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

}
