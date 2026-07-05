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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.Handler;

public class TestHttpResponseEvent {

    private static final int PORT = 9976;
    private static final String HOST = "localhost";

    public record Person(String name, int age) {}

    private static HttpServer start(final Handler<HttpRouting> handler,
                                    final Handler<HttpResponseEvent> onResponse) {
        final HttpRouter router = HttpRouter.builder()
                                            .onResponse(onResponse)
                                            .webService(new WebService(HttpMethod.GET, "/x", handler))
                                            .build();
        return newHttpServer(false, PORT, router);
    }

    @Test
    public void test_onResponse_reads_and_replaces_json_body() {
        final HttpClient client = newHttpClient(false, PORT, HOST);

        final AtomicReference<Boolean> hadBody = new AtomicReference<>();
        final AtomicReference<String> readName = new AtomicReference<>();
        final AtomicReference<HttpRoutingContext> routingContext = new AtomicReference<>();

        final Handler<HttpRouting> handler = v -> v.response().sendJson(new Person("Alice", 30));
        final Handler<HttpResponseEvent> hook = event -> {
            hadBody.set(event.hasBody());
            readName.set(event.bodyAsJsonObject().getString("name"));
            routingContext.set(event.routingContext());
            event.updateJsonBody(new Person("Bob", 99));
        };
        final HttpServer server = start(handler, hook);
        try {
            final var response = awaitValue(client.send(HttpGet.of("/x")));
            assertEquals(200, response.statusCode());

            // hook observations
            assertEquals(Boolean.TRUE, hadBody.get());
            assertEquals("Alice", readName.get());
            assertNotNull(routingContext.get());

            // the hook replaced the body
            final var body = response.bodyAsJsonObject();
            assertEquals("Bob", body.getString("name"));
            assertEquals(99, body.getInteger("age"));
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_onResponse_body_as_class() {
        final HttpClient client = newHttpClient(false, PORT, HOST);

        final AtomicReference<Person> asClass = new AtomicReference<>();

        final Handler<HttpRouting> handler = v -> v.response().sendJson(new Person("Carol", 42));
        final Handler<HttpResponseEvent> hook = event -> asClass.set(event.body(Person.class));
        final HttpServer server = start(handler, hook);
        try {
            final var response = awaitValue(client.send(HttpGet.of("/x")));
            assertEquals(200, response.statusCode());
            assertEquals(new Person("Carol", 42), asClass.get());
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_onResponse_reads_binary_body() {
        final HttpClient client = newHttpClient(false, PORT, HOST);

        final AtomicReference<Integer> binaryLength = new AtomicReference<>();

        final Handler<HttpRouting> handler = v -> v.response().send(new byte[] { 1, 2, 3, 4 });
        final Handler<HttpResponseEvent> hook = event -> binaryLength.set(event.bodyAsBinary().length);
        final HttpServer server = start(handler, hook);
        try {
            final var response = awaitValue(client.send(HttpGet.of("/x")));
            assertEquals(200, response.statusCode());
            assertEquals(4, binaryLength.get());
            assertEquals(4, response.bodyAsBinary().length);
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    @Test
    public void test_onResponse_clearBody() {
        final HttpClient client = newHttpClient(false, PORT, HOST);

        final Handler<HttpRouting> handler = v -> v.response().send("hello");
        final Handler<HttpResponseEvent> hook = event -> event.clearBody();
        final HttpServer server = start(handler, hook);
        try {
            final var response = awaitValue(client.send(HttpGet.of("/x")));
            assertNull(response.bodyAsText(), "clearBody() must drop the body");
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

}
