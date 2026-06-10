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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.http.server.util.TestUtil;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.NoStackTraceTimeoutException;

public class TestWebService_GET {

    private static Logger LOGGER = LoggerFactory.getLogger(TestWebService_GET.class);
    
    @Test
    public void test_01() {
        testWebService(9999, "localhost", false);
    }
    
    @Test
    public void test_02() {
        testWebService(9999, "localhost", true);
    }
    
    public void testWebService(final int port, 
                               final String host,
                               final boolean ssl) {
        final HttpClient httpClient = newHttpClient(ssl, port, host);
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("webService_1");
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().send("webService_2");
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .webService(webService_2)
                      .build();
        
        final HttpServer httpServer = newHttpServer(ssl, port, httpRouter);
        try {
            if (ssl) {
                assertEquals(HttpCertificate.SELF_SIGNED, httpServer.certificate());
            }
            else {
                assertEquals(HttpCertificate.NONE, httpServer.certificate());
            }
            
            final Future<HttpResponse> httpResponse_1 = httpClient.send(HttpGet.of("/webService_1"))
                                                                  .onFailure(e -> LOGGER.error("/webService_1 failed", e));
            await(httpResponse_1);
            assertEquals(200, httpResponse_1.result().statusCode());
            assertEquals(HttpVersion.HTTP_2, httpResponse_1.result().version());
            assertEquals("webService_1", httpResponse_1.result().bodyAsText());
            
            final Future<HttpResponse> httpResponse_2 = httpClient.send(HttpGet.of("/webService_2"))
                                                                  .onFailure(e -> LOGGER.error("/webService_2 failed", e));
            await(httpResponse_2);
            assertEquals(200, httpResponse_2.result().statusCode());
            assertEquals(HttpVersion.HTTP_2, httpResponse_2.result().version());
            assertEquals("webService_2", httpResponse_2.result().bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_03() {
        final HttpClient httpClient = newHttpClient();
        final AtomicReference<HttpRequest> httpRequest = new AtomicReference<>();
        final AtomicReference<com.easygoingapi.yoja.http.server.HttpResponse> httpResponse = new AtomicReference<>();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            httpRequest.set(v.request());
            v.response().send("Paul");
            httpResponse.set(v.response());
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(HttpGet.of("/webService_1")));
            assertEquals("Paul", httpResponseClient.bodyAsText());
            
            assertEquals(TestUtil.ssl, httpRequest.get().ssl());
            assertEquals(TestUtil.host, httpRequest.get().host());
            assertEquals("/webService_1", httpRequest.get().path());
            assertEquals(true, httpRequest.get().hasHeader());
            assertEquals(Set.of("user-agent"), 
                         httpRequest.get().headerNames());
            assertEquals(false, httpRequest.get().hasHeader("???"));
            assertEquals(false, httpRequest.get().hasParameter());
            assertEquals(false, httpRequest.get().hasParameter("???"));
            assertEquals(Set.of(), httpRequest.get().parameterNames());
            assertEquals(null, httpRequest.get().firstParameter("???"));
            assertEquals(List.of(), httpRequest.get().parameters());
            assertEquals(List.of(), httpRequest.get().parameters("???"));
            assertEquals(true, httpRequest.get().isEmptyBody());
            assertEquals(HttpMethod.GET, httpRequest.get().method());
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
            assertEquals(Set.of(), httpResponse.get().cookies("???"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_04() {
        final HttpClient httpClient = newHttpClient();
        final AtomicReference<HttpRequest> httpRequest = new AtomicReference<>();
        final AtomicReference<com.easygoingapi.yoja.http.server.HttpResponse> httpResponse = new AtomicReference<>();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            httpRequest.set(v.request());
            final String name = v.request().firstParameter("name");
            final List<String> age = v.request().parameters("age");
            v.response().send(name + " " + age.get(2));
            httpResponse.set(v.response());
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet = HttpGet.builder("/webService_1")
                                           .addParameter("name", "Paul")
                                           .addParameter("age", List.of("1", "2", "3"))
                                           .build();
            final HttpResponse httpResponseClient = awaitValue(httpClient.send(httpGet));
            assertEquals("Paul 3", httpResponseClient.bodyAsText());
            
            assertEquals(TestUtil.ssl, httpRequest.get().ssl());
            assertEquals(TestUtil.host, httpRequest.get().host());
            assertEquals("/webService_1", httpRequest.get().path());
            assertEquals(false, httpRequest.get().hasHeader(null));
            assertEquals(true, httpRequest.get().hasHeader());
            assertEquals(Set.of("user-agent"), 
                         httpRequest.get().headerNames());
            assertEquals(false, httpRequest.get().hasHeader("???"));
            assertEquals(true, httpRequest.get().hasParameter());
            assertEquals(false, httpRequest.get().hasParameter("???"));
            assertEquals(true, httpRequest.get().hasParameter("age"));
            assertEquals("1", httpRequest.get().firstParameter("age"));
            assertEquals(null, httpRequest.get().firstParameter(null));
            assertEquals(Set.of("name", "age"), httpRequest.get().parameterNames());
            assertEquals(null, httpRequest.get().firstParameter("???"));
            assertEquals(httpGet.parameters(), httpRequest.get().parameters());
            assertEquals(List.of(), httpRequest.get().parameters("???"));
            assertEquals(true, httpRequest.get().isEmptyBody());
            assertEquals(HttpMethod.GET, httpRequest.get().method());
            assertEquals(HttpVersion.HTTP_2, httpRequest.get().version());
            assertEquals(false, httpRequest.get().hasCookie());
            assertEquals(true, httpRequest.get().cookies().isEmpty());
            assertEquals(null, httpRequest.get().cookie("???"));
            
            assertEquals(200, httpResponse.get().statusCode());
            assertEquals(true, httpResponse.get().hasHeader());
            assertEquals(Set.of(":status", "content-length", "content-type", "cache-control"), 
                         httpResponse.get().headerNames());
            assertEquals(false, httpResponse.get().hasHeader("???"));
            assertEquals(true, httpResponse.get().sent());
            assertEquals(true, httpResponse.get().cookies().isEmpty());
            assertEquals(Set.of(), httpResponse.get().cookies("???"));
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
            throw new RuntimeException("error test_05");
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webService_1"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(500, httpResponseClient.statusCode());
            assertEquals("Internal Server Error", httpResponseClient.statusMessage());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_06() {
        final HttpClient httpClient = newHttpClient(TestUtil.ssl, 
                                                    Duration.ofSeconds(1), 
                                                    TestUtil.port,
                                                    TestUtil.host);
        final Handler<HttpRouting> httpRouting_1 = v -> {
            // do nothing
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webService_1"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(true, httpResponseFuture.failed());
            assertEquals(NoStackTraceTimeoutException.class, httpResponseFuture.cause().getClass());
            assertEquals(null, httpResponseClient);
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
            v.response().send("test_07");
            throw new RuntimeException("error test_07");
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webService_1"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals("test_07", httpResponseClient.bodyAsText());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
}
