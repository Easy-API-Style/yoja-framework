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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpCookie;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.google.common.collect.Lists;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.StreamResetException;

public class TestHttpCookie {
    
    private static Logger LOGGER = LoggerFactory.getLogger(TestHttpCookie.class);
    
    @Test
    public void test_01() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().addCookie(HttpCookie.of("test_cookie", "value_test_cookie"));
            v.response().addCookie(HttpCookie.of("test cookie", "value test cookie"));
            v.response().send("done");
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
            assertEquals(null, httpResponseClient);
            assertEquals(StreamResetException.class, httpResponseFuture.cause().getClass());
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
            v.response().addCookie(HttpCookie.of("test_cookie", "value_test_cookie_1"));
            v.response().send();
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().addCookie(HttpCookie.of("test_cookie", "value_test_cookie_3"));
            v.response().addCookie(HttpCookie.of("test_cookie", "value_test_cookie_2"));
            v.response().addCookie(HttpCookie.builder("test_cookie", 
                                                      "value_test_cookie_2")
                                             .path("/")
                                             .build());
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .webService(webService_2)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture_1 = httpClient.send(HttpGet.of("/webService_1"));
            final HttpResponse httpResponseClient_1 = awaitValue(httpResponseFuture_1);
            assertEquals(false, httpResponseFuture_1.failed());
            assertEquals(false, httpResponseClient_1.cookies().isEmpty());
            assertEquals(1, httpResponseClient_1.cookies().size());
            assertEquals(Set.of(HttpCookie.of("test_cookie", "value_test_cookie_1")), httpResponseClient_1.cookies("test_cookie"));
            assertEquals(200, httpResponseClient_1.statusCode());
            
            final Future<HttpResponse> httpResponseFuture_2 = httpClient.send(HttpGet.of("/webService_2"));
            final HttpResponse httpResponseClient_2 = awaitValue(httpResponseFuture_2);
            assertEquals(false, httpResponseFuture_2.failed());
            assertEquals(false, httpResponseClient_2.cookies().isEmpty());
            assertEquals(2, httpResponseClient_2.cookies().size());
            assertEquals(Set.of(HttpCookie.of("test_cookie", "value_test_cookie_3"),
                                HttpCookie.builder("test_cookie", 
                                                   "value_test_cookie_2")
                                          .path("/")
                                          .build()), 
                         httpResponseClient_2.cookies("test_cookie"));
            assertEquals(200, httpResponseClient_2.statusCode());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_03() {
        final HttpClient httpClient = newHttpClient();
        final AtomicReference<Map<String, String>> httpCookies = new AtomicReference<>();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            httpCookies.set(v.request().cookies());
            v.response().addCookie(HttpCookie.of("test_cookie_1", "value_test_cookie_1"));
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture_1 = httpClient.send(HttpGet.builder("/webService_1")
                                                                                    .putCookie("from-client", "hello")
                                                                                    .putCookie("to-server", "hello") 
                                                                                    .build());
            final HttpResponse httpResponseClient_1 = awaitValue(httpResponseFuture_1);
            assertEquals(Map.of("from-client", "hello", "to-server", "hello"), 
                         httpCookies.get());
            
            assertEquals(false, httpResponseFuture_1.failed());
            assertEquals(false, httpResponseClient_1.cookies().isEmpty());
            assertEquals(1, httpResponseClient_1.cookies().size());
            assertEquals(Set.of(HttpCookie.of("test_cookie_1", "value_test_cookie_1")), httpResponseClient_1.cookies("test_cookie_1"));
            assertEquals(200, httpResponseClient_1.statusCode());
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
            v.response().addCookie(HttpCookie.builder("test_cookie_1", 
                                                      "value_test_cookie_1")
                                             .path("/")
                                             .maxAge(120)
                                             .domain("domain")
                                             .sameSite(CookieSameSite.STRICT)
                                             .build());
            v.response().send();
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().addCookie(HttpCookie.of("test_cookie_2", "value_2"));
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .session(new HttpSessionStore("session.yoja", Duration.ofDays(365)))
                      .webService(webService_1)
                      .webService(webService_2)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet = HttpGet.builder("/webService_1")
                                           .putCookie("test_cookie_3", "value_test_cookie_3")
                                           .build();
            final Future<HttpResponse> httpResponseFuture_1 = httpClient.send(httpGet);
            final HttpResponse httpResponseClient_1 = awaitValue(httpResponseFuture_1);
            assertEquals(false, httpResponseFuture_1.failed());
            assertEquals(false, httpResponseClient_1.cookies().isEmpty());
            assertEquals(2, httpResponseClient_1.cookies().size());
            assertEquals(Set.of(HttpCookie.builder("test_cookie_1", 
                                                   "value_test_cookie_1")
                                          .path("/")
                                          .domain("domain")
                                          .maxAge(120)
                                          .sameSite(CookieSameSite.STRICT)
                                          .build()),
                         httpResponseClient_1.cookies("test_cookie_1"));
            assertEquals(200, httpResponseClient_1.statusCode());
            
            final Future<HttpResponse> httpResponseFuture_2 = httpClient.send(HttpGet.of("/webService_2"));
            final HttpResponse httpResponseClient_2 = awaitValue(httpResponseFuture_2);
            assertEquals(false, httpResponseFuture_2.failed());
            assertEquals(false, httpResponseClient_2.cookies().isEmpty());
            assertEquals(2, httpResponseClient_2.cookies().size());
            assertEquals(HttpCookie.of("test_cookie_2", "value_2"), 
                         Lists.newArrayList(httpResponseClient_2.cookies()).get(1));
            assertEquals("session.yoja", Lists.newArrayList(httpResponseClient_2.cookies()).get(0).getName());
            assertEquals(200, httpResponseClient_2.statusCode());
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
            v.response().addCookie(HttpCookie.of("test_cookie_1", "value_1"));
            v.response().addCookie(HttpCookie.of("test_cookie_2", "value_2"));
            v.response().addCookie(HttpCookie.of("test_cookie_3", "value_3"));
            v.response().removeCookies("test_cookie_2");
            v.response().send();
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
            assertEquals(2, httpResponseClient.cookies().size());
            assertEquals(0, httpResponseClient.cookies("test_cookie_2").size());
            assertEquals(1, httpResponseClient.cookies("test_cookie_3").size());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_06() {
        final HttpClient httpClient = newHttpClient();
        final AtomicReference<HttpCookie> httpCookies = new AtomicReference<>();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().addCookie(HttpCookie.builder("test_cookie_1", 
                                                      "value_test_cookie_1")
                                             .path("/")
                                             .domain("domain")
                                             .maxAge(120)
                                             .sameSite(CookieSameSite.STRICT)
                                             .build());
            v.response().addCookie(HttpCookie.builder("test_cookie_1", 
                                                      "value_test_cookie_2")
                                             .path("/root")
                                             .domain("domain")
                                             .maxAge(120)
                                             .sameSite(CookieSameSite.STRICT)
                                             .build());
            v.response().addCookie(HttpCookie.builder("test_cookie_1", 
                                                      "value_test_cookie_3")
                                             .path("/root")
                                             .domain("rootDomain")
                                             .maxAge(120)
                                             .sameSite(CookieSameSite.STRICT)
                                             .build());
            httpCookies.set(v.response().cookie("test_cookie_1", "rootDomain", "/root"));
            v.response().send();
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
            assertEquals(3, httpResponseClient.cookies().size());
            assertEquals(3, httpResponseClient.cookies("test_cookie_1").size());
            assertEquals(HttpCookie.builder("test_cookie_1", 
                                            "value_test_cookie_3")
                                   .path("/root")
                                   .domain("rootDomain")
                                   .maxAge(120)
                                   .sameSite(CookieSameSite.STRICT)
                                   .build(), 
                        httpCookies.get());
            assertEquals(HttpCookie.builder("test_cookie_1", 
                                            "value_test_cookie_3")
                                   .path("/root")
                                   .domain("domain")
                                   .maxAge(120)
                                   .sameSite(CookieSameSite.STRICT)
                                   .build(), 
                         httpResponseClient.cookie("test_cookie_1", "domain", "/root"));
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
            v.response().addCookie(HttpCookie.builder("test_cookie_1", 
                                                      "value_1")
                                             .domain("rootDomain")
                                             .path("/root")
                                             .maxAge(120)
                                             .sameSite(CookieSameSite.LAX)
                                             .build());
            v.response().addCookie(HttpCookie.of("test_cookie_2", "value_2"));
            v.response().addCookie(HttpCookie.builder("test_cookie_2", 
                                                      "value_2")
                                             .domain("rootDomain")
                                             .path("/root")
                                             .maxAge(120)
                                             .sameSite(CookieSameSite.NONE)
                                             .build());
            v.response().addCookie(HttpCookie.builder("test_cookie_3", 
                                                      "value_3")
                                             .domain("rootDomain")
                                             .path("/root")
                                             .maxAge(120)
                                             .sameSite(CookieSameSite.NONE)
                                             .build());
            v.response().removeCookie("test_cookie_1", "rootDomain", "/root");
            v.response().removeCookies("test_cookie_2");
            v.response().removeCookie("test_cookie_3", "domain", "/root");
            
            v.response().send();
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
            assertEquals(1, httpResponseClient.cookies().size());
            assertEquals(0, httpResponseClient.cookies("test_cookie_2").size());
            assertEquals(1, httpResponseClient.cookies("test_cookie_3").size());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_08() {
        final HttpClient httpClient = newHttpClient();
        final Set<HttpCookie> httpCookies_1 = new TreeSet<>();
        httpCookies_1.add(HttpCookie.builder("test_cookie_1", 
                                             "value_1")
                                    .path("/root")
                                    .domain("rootDomain")
                                    .maxAge(120)
                                    .sameSite(CookieSameSite.LAX)
                                    .build());
        httpCookies_1.add(HttpCookie.of("test_cookie_2", "value_2"));
        httpCookies_1.add(HttpCookie.builder("test_cookie_2", 
                                             "value_2")
                                    .path("/root")
                                    .domain("rootDomain")
                                    .maxAge(120)
                                    .sameSite(CookieSameSite.NONE)
                                    .build());
        httpCookies_1.add(HttpCookie.builder("test_cookie_3", 
                                             "value_3")
                                    .path("/root")
                                    .domain("rootDomain")
                                    .maxAge(120)
                                    .sameSite(CookieSameSite.NONE)
                                    .build());
        
        final Set<HttpCookie> httpCookies_2 = new TreeSet<>();
        httpCookies_2.add(HttpCookie.of("test_cookie_2", "value_2"));
        httpCookies_2.add(HttpCookie.builder("test_cookie_2", 
                                             "value_2")
                                    .path("/root")
                                    .domain("rootDomain")
                                    .maxAge(120)
                                    .sameSite(CookieSameSite.NONE)
                                    .build());
        
        final AtomicReference<Set<HttpCookie>> ref_1 = new AtomicReference<>();
        final AtomicReference<Set<HttpCookie>> ref_2 = new AtomicReference<>();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            httpCookies_1.forEach(c -> v.response().addCookie(c));
            v.nextHandler();
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            ref_1.set(v.response().cookies());
            ref_2.set(v.response().cookies("test_cookie_2"));
            v.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1, httpRouting_2);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            await(httpClient.send(HttpGet.of("/webService_1")));
            assertEquals(httpCookies_1, ref_1.get());
            assertEquals(httpCookies_2, ref_2.get());
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
            v.response().addCookie(HttpCookie.of("test_cookie_1", null));
            v.response().send();
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
            assertEquals(0, httpResponseClient.cookies().size());
            assertEquals(0, httpResponseClient.cookies("test_cookie_1").size());
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
            v.response().addCookie(HttpCookie.of("test_cookie_1", "value_1"));
            v.response().addCookie(HttpCookie.of("test_cookie_1", null));
            v.response().send();
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
            assertEquals(0, httpResponseClient.cookies().size());
            assertEquals(0, httpResponseClient.cookies("test_cookie_1").size());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
}
