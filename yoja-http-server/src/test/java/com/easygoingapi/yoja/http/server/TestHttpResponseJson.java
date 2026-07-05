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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.fasterxml.jackson.annotation.JsonView;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TestHttpResponseJson {

    private static final int PORT = 9971;
    private static final String HOST = "localhost";

    private record Person(String name, int age) {}

    /** Jackson view markers for the {@link #sendJson(Object, Class)} test. */
    private static final class Views {
        static final class Public {}
        static final class Admin {}
    }

    private static final class Account {
        @JsonView(Views.Public.class) public String name = "alice";
        @JsonView(Views.Admin.class)  public String secret = "s3cr3t";
    }

    /** Starts a server whose single GET service runs {@code handler}. */
    private static HttpServer start(final Handler<HttpRouting> handler) {
        final HttpRouter router = HttpRouter.builder()
                                            .webService(new WebService(HttpMethod.GET, "/json", handler))
                                            .build();
        return newHttpServer(false, PORT, router);
    }

    @Test
    public void test_sendJson_object() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        final HttpServer server = start(v -> v.response().sendJson(new Person("Alice", 30)));
        try {
            final HttpResponse response = awaitValue(client.send(HttpGet.of("/json")));
            assertEquals(200, response.statusCode());
            assertTrue(response.header("content-type").contains("json"),
                       "content-type should be json: " + response.header("content-type"));

            final JsonObject body = response.bodyAsJsonObject();
            assertEquals("Alice", body.getString("name"));
            assertEquals(30, body.getInteger("age"));
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_sendJson_collection() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        final HttpServer server = start(v -> v.response()
                                              .sendJson(List.<Object>of(new Person("A", 1),
                                                                        new Person("B", 2))));
        try {
            final HttpResponse response = awaitValue(client.send(HttpGet.of("/json")));
            assertEquals(200, response.statusCode());

            final JsonArray body = response.bodyAsJsonArray();
            assertEquals(2, body.size());
            assertEquals("A", body.getJsonObject(0).getString("name"));
            assertEquals(2, body.getJsonObject(1).getInteger("age"));
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_sendJson_withView() {
        final HttpClient client = newHttpClient(false, PORT, HOST);
        // Serialize with the Public view: the Admin-only field must be dropped.
        final HttpServer server = start(v -> v.response().sendJson(new Account(), Views.Public.class));
        try {
            final HttpResponse response = awaitValue(client.send(HttpGet.of("/json")));
            assertEquals(200, response.statusCode());

            final JsonObject body = response.bodyAsJsonObject();
            assertEquals("alice", body.getString("name"));
            assertFalse(body.containsKey("secret"), "Admin-only field must be excluded by the Public view");
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

}
