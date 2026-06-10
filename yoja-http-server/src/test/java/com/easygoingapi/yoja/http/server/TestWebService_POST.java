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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpPost;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.http.server.util.TestUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TestWebService_POST {

    @Test
    public void test_01() {
        final HttpClient httpClient = newHttpClient();
        final AtomicReference<HttpRequest> httpRequest = new AtomicReference<>();
        final AtomicReference<com.easygoingapi.yoja.http.server.HttpResponse> httpResponse = new AtomicReference<>();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            httpRequest.set(v.request());
            final String name = v.request().bodyAsJsonObject().getString("name");
            final int age = v.request().bodyAsJsonObject().getInteger("age");
            v.response().send(name + " " + age);
            httpResponse.set(v.response());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", 
                                                  JsonObject.of("name", "Paul", "age", 10));
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("Paul 10", httpResponseClient.bodyAsText());

            assertEquals(TestUtil.ssl, httpRequest.get().ssl());
            assertEquals(TestUtil.host, httpRequest.get().host());
            assertEquals("/webService_1", httpRequest.get().path());
            assertEquals(true, httpRequest.get().hasHeader());
            assertEquals(Set.of("content-length", "content-type", "user-agent"), 
                         httpRequest.get().headerNames());
            assertEquals(false, httpRequest.get().hasHeader("???"));
            assertEquals(false, httpRequest.get().hasParameter());
            assertEquals(false, httpRequest.get().hasParameter("???"));
            assertEquals(Set.of(), httpRequest.get().parameterNames());
            assertEquals(null, httpRequest.get().firstParameter("???"));
            assertEquals(List.of(), httpRequest.get().parameters());
            assertEquals(List.of(), httpRequest.get().parameters("???"));
            assertEquals(false, httpRequest.get().isEmptyBody());
            assertEquals(HttpMethod.POST, httpRequest.get().method());
            assertEquals(HttpVersion.HTTP_2, httpRequest.get().version());
            assertEquals(true, httpRequest.get().cookies().isEmpty());
            assertEquals(null, httpRequest.get().cookie("???"));

            assertEquals(200, httpResponse.get().statusCode());
            assertEquals(true, httpResponse.get().hasHeader());
            assertEquals(Set.of(":status", "content-length", "content-type", "cache-control"),
                         httpResponse.get().headerNames());
            assertEquals(false, httpResponse.get().hasHeader("???"));
            assertEquals(true, httpResponse.get().sent());
            assertEquals(true, httpResponse.get().cookies().isEmpty());
            assertEquals("text/plain", httpResponse.get().header("content-tyPE"));
            assertEquals(Set.of(), httpResponse.get().cookies("???"));
        } 
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_02() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, 
                                                       "/webService_1", 
                                                       httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webService_1"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(405, httpResponseClient.statusCode());
            assertEquals("Method Not Allowed", httpResponseClient.statusMessage());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @JsonPropertyOrder({ "value_3", "value_2", "value_1" })
    private static class Body {
        
        @JsonProperty("value_1")
        private final String value_1;
        @JsonProperty("value_2")
        private final String value_2;
        @JsonProperty("value_3")
        private final String value_3;
        
        @JsonCreator
        public Body(@JsonProperty("value_1") final String value_1,
                    @JsonProperty("value_2") final String value_2,
                    @JsonProperty("value_3") final String value_3) {
            super();
            this.value_1 = value_1;
            this.value_2 = value_2;
            this.value_3 = value_3;
        }

        public String getValue_1() {
            return value_1;
        }

        public String getValue_2() {
            return value_2;
        }

        public String getValue_3() {
            return value_3;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value_1, value_2, value_3);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Body other = (Body) obj;
            return Objects.equals(value_1, other.value_1) 
                    && Objects.equals(value_2, other.value_2)
                    && Objects.equals(value_3, other.value_3);
        }
        
    }
    
    @Test
    public void test_03() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().bodyAsJsonObject());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Body body = new Body("value_1", "value_2", "value_3");
            final HttpPost httpPost = HttpPost.of("/webService_1", JsonObject.mapFrom(body));
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("{\"value_3\":\"value_3\",\"value_2\":\"value_2\",\"value_1\":\"value_1\"}", httpResponseClient.bodyAsText());
            assertEquals("application/json", httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_04() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().bodyAsJsonObject());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Body body = new Body("value_1", "value_2", "value_3");
            final HttpPost httpPost = HttpPost.of("/webService_1", JsonObject.mapFrom(body));
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(JsonObject.mapFrom(body), 
                         httpResponseClient.bodyAsJsonObject());
            assertEquals("application/json", httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_05() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().bodyAsJsonObject());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Body body = new Body("value_1", "value_2", "value_3");
            final HttpPost httpPost = HttpPost.of("/webService_1", JsonObject.mapFrom(body));
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("{\"value_3\":\"value_3\",\"value_2\":\"value_2\",\"value_1\":\"value_1\"}", new String(httpResponseClient.bodyAsBinary()));
            assertEquals("application/json", httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_06() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().bodyAsJsonObject());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Body body = new Body("value_1", "value_2", "value_3");
            final HttpPost httpPost = HttpPost.of("/webService_1", JsonObject.mapFrom(body));
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(body, httpResponseClient.body(Body.class));
            assertEquals("application/json", httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_07() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().bodyAsText());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", 
                                                  "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("body", httpResponseClient.bodyAsText());
            assertEquals("text/plain", httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_08() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().bodyAsJsonArray());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Body body_1 = new Body("value_1", "value_2", "value_3");
            final Body body_2 = new Body("value_11", "value_22", "value_33");
            final HttpPost httpPost = HttpPost.of("/webService_1", 
                                                  JsonArray.of(body_1, body_2));
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(body_1, httpResponseClient.bodyAsJsonArray().getJsonObject(0).mapTo(Body.class));
            assertEquals(body_2, httpResponseClient.bodyAsJsonArray().getJsonObject(1).mapTo(Body.class));
            assertEquals("application/array-json", httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_09() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().bodyAsByteArray());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", 
                                                  "body".getBytes());
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("body", new String(httpResponseClient.bodyAsBinary()));
            assertEquals(null, httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_10() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.toString());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", 
                                                  "body".getBytes());
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("HttpRouting [version=HTTP_2, method=POST, uri=https://localhost:8888/webService_1]", 
                         httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_11() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.response().toString());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", 
                                                  "body".getBytes());
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("HttpResponse [version=HTTP_2, method=POST, uri=https://localhost:8888/webService_1]", 
                         httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_12() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.request().toString());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", 
                                                  "body".getBytes());
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("HttpRequest [version=HTTP_2, method=POST, uri=https://localhost:8888/webService_1]", 
                         httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_13() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            final Set<Object> emptyList = new HashSet<>();
            emptyList.addAll(v.request().parameterNames());
            emptyList.addAll(v.request().parameters(null));
            emptyList.addAll(v.request().cookies().values());
            emptyList.addAll(v.request().parameters());
            emptyList.addAll(v.response().headerNames());
            emptyList.addAll(v.response().cookies());
            emptyList.addAll(v.response().cookies(null));
            v.response().send(String.valueOf(emptyList.size()));
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("0", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_14() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(h.toString());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("HttpResponseEvent [statusCode=200, version=HTTP_2, method=POST, uri=https://localhost:8888/webService_1]",
                         httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_15() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            boolean emptyList = false;
            emptyList = emptyList || v.request().hasHeader(null);
            emptyList = emptyList || v.request().hasParameter();
            emptyList = emptyList || v.request().hasParameter(null);
            emptyList = emptyList || v.request().hasCookie(null);
            
            emptyList = emptyList || v.response().hasHeader();
            emptyList = emptyList || v.response().hasHeader(null);
            emptyList = emptyList || v.response().hasCookie();
            emptyList = emptyList || v.response().hasCookie(null);
            v.response().send(String.valueOf(emptyList));
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("false", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_16() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(JsonObject.mapFrom(v.request().body(Body.class)));
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", JsonObject.mapFrom(new Body("1", "2", "3")));
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("{\"value_3\":\"3\",\"value_2\":\"2\",\"value_1\":\"1\"}", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_17() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("body");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(h.bodyType().name());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(HttpBodyType.Text.name(), httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_18() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(JsonObject.of("key", "value"));
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(h.bodyType().name());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(HttpBodyType.JsonObject.name(), httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_19() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(JsonArray.of(new Body("1", "2", "3")));
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(h.bodyType().name());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(HttpBodyType.JsonArray.name(), httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_20() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("body".getBytes());
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(h.bodyType().name());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(HttpBodyType.binary.name(), httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_21() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(h.bodyType().name());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals(HttpBodyType.None.name(), httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_22() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(String.valueOf(h.cookie(null, "d", "/") == null));
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("true", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_23() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(String.valueOf(h.hasBody()));
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("false", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_24() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody("body");
                                                })
                                                .onResponse(h -> {
                                                    h.updateBody(String.valueOf(h.hasBody()));
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("true", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_25() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(String.valueOf(v.response().hasHeader()));
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("false", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_26() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("boby");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(h.toString());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("HttpResponseEvent [statusCode=200, version=HTTP_2, method=POST, uri=https://localhost:8888/webService_1]", 
                         httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_27() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(String.valueOf(v.response().headerNames()));
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("[]", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_28() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("boby");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody("body".getBytes());
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("body", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_29() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("boby");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(JsonObject.of("key", "value"));
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("{\"key\":\"value\"}", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_30() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("boby");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService_1)
                                                .onResponse(h -> {
                                                    h.updateBody(JsonArray.of("key", "value"));
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpPost));
            assertEquals("[\"key\",\"value\"]", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
//    @Test
//    public void test_31() {
//        final HttpClient httpClient = newHttpClient();
//        final Handler<HttpRouting> httpRouting_1 = v -> {
//            v.response().send("boby");
//        };
//        final WebService webService_1 = new WebService(HttpMethod.POST, "/webService_1", httpRouting_1);
//        final HttpRouter httpRouter = HttpRouter.builder()
//                                                .webService(webService_1)
//                                                .onResponse(h -> {
//                                                    h.updateBody(JsonWriter.writeValue(new Body("1", "2", "3")));
//                                                })
//                                                .build();
//        final HttpServer httpServer = newHttpServer(httpRouter);
//        try {
//            final HttpPost httpPost = HttpPost.of("/webService_1", "body");
//            final HttpResponse httpResponseClient = awaitValue(httpClient.post(httpPost));
//            assertEquals("[\"key\",\"value\"]", httpResponseClient.bodyAsText());
//        }
//        finally {
//            await(httpServer.stop());
//            assertTrue(httpServer.is(State.stopped));
//        }
//    }
    
}
