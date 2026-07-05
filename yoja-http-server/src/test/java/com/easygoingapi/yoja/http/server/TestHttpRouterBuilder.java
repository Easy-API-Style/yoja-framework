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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.Handler;

public class TestHttpRouterBuilder {

    private static final int PORT = 9973;
    private static final String HOST = "localhost";

    @Test
    public void test_webService_varargs_and_webServices_list() {
        final HttpClient client = newHttpClient(false, PORT, HOST);

        final Handler<HttpRouting> a = v -> v.response().send("a");
        final Handler<HttpRouting> first = v -> v.nextHandler();
        final Handler<HttpRouting> second = v -> v.response().send("c");
        final WebService wsB = new WebService(HttpMethod.GET, "/b", v -> v.response().send("b"));

        final HttpRouter router = HttpRouter.builder()
                                            .webService(HttpMethod.GET, "/a", a)      // varargs, single handler
                                            .webService(HttpMethod.GET, "/c", first, second) // varargs, chained
                                            .webServices(List.of(wsB))                // List overload
                                            .build();
        final HttpServer server = newHttpServer(false, PORT, router);
        try {
            assertEquals("a", awaitValue(client.send(HttpGet.of("/a"))).bodyAsText());
            assertEquals("c", awaitValue(client.send(HttpGet.of("/c"))).bodyAsText());
            assertEquals("b", awaitValue(client.send(HttpGet.of("/b"))).bodyAsText());
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

}
