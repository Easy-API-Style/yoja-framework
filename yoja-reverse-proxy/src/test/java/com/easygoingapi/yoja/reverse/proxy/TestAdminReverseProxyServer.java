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
package com.easygoingapi.yoja.reverse.proxy;

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static com.easygoingapi.yoja.reverse.proxy.util.TestUtil.newHttpClient;
import static com.easygoingapi.yoja.reverse.proxy.util.TestUtil.startProxy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpPost;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpRouting;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.HttpSessionStore;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyServer;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class TestAdminReverseProxyServer {

    private static Logger LOGGER = LoggerFactory.getLogger(TestAdminReverseProxyServer.class);
    
    @Test
    public void test_01() {
        HttpServer server = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            server = awaitValue(server(1111));
            reverseProxyServer = awaitValue(startProxy(7777, null, 8888, "token"));
            await(reverseProxyServer.startAdmin());
            
            final HttpClient httpClient = newHttpClient(false, 8888, "localhost");
            
            final HttpResponse httpResponse_1 = awaitValue(httpClient.send(HttpGet.of("/load/rules")));
            assertEquals(403, httpResponse_1.statusCode());
            assertEquals("wrong token", httpResponse_1.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            if (server != null) {
                await(server.stop());
                assertTrue(server.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }
    
    @Test
    public void test_02() {
        HttpServer server = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            server = awaitValue(server(1111));
            reverseProxyServer = awaitValue(startProxy(7777, null, 8888, "token"));
            await(reverseProxyServer.startAdmin());
            
            final HttpClient httpClient = newHttpClient(false, 8888, "localhost");
            
            final HttpResponse httpResponse_1 = awaitValue(httpClient.send(HttpGet.builder("/load/rules")
                                                                     .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                     .build()));
            assertEquals(200, httpResponse_1.statusCode());
            assertEquals("[]", httpResponse_1.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            if (server != null) {
                await(server.stop());
                assertTrue(server.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }

    @Test
    public void test_03() {
        HttpServer server = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            server = awaitValue(server(1111));
            
            final Set<ReverseProxyRule> reverseProxyRules = new HashSet<>();
            reverseProxyRules.add(new ReverseProxyRule(Url.from("localhost", "/serverOne"),
                                                       Url.to(false, "localhost")
                                                          .cutsPathWith("/serverOne")
                                                          .port(1111)
                                                          .build()));
            
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules, 8888, "token"));
            await(reverseProxyServer.startAdmin());
            
            final HttpClient httpClient_admin = newHttpClient(false, 8888, "localhost");
            final HttpClient httpClient = newHttpClient(false, 7777, "localhost");
            
            final HttpResponse httpResponse_1 = awaitValue(httpClient_admin.send(HttpGet.builder("/load/rules")
                                                                           .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                           .build()));
            assertEquals(200, httpResponse_1.statusCode());
            JsonArray rules = httpResponse_1.bodyAsJsonArray();
            
            assertEquals("{\"from\":{\"host\":\"localhost\",\"path\":\"/serverOne\"},"
                          + "\"to\":{\"ssl\":false,\"host\":\"localhost\",\"ports\":[1111],\"cutsPathWith\":\"/serverOne\"}}", 
                          rules.getJsonObject(0).encode());
            
            final HttpResponse httpResponse_2 = awaitValue(httpClient.send(HttpGet.of("/serverOne/hello/get")));
            assertEquals("get", httpResponse_2.bodyAsText());
            
            final HttpResponse httpResponse_3 = awaitValue(httpClient_admin.send(HttpPost.builder("/remove/rule")
                                                                           .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                           .body(rules.getJsonObject(0).getJsonObject("from"))
                                                                           .build()));
            assertEquals(200, httpResponse_3.statusCode());
            
            final Future<HttpResponse> future_4 = httpClient.send(HttpGet.of("/serverOne/hello/get"));
            final HttpResponse httpResponse_4 = awaitValue(future_4);
            assertEquals(true, future_4.failed());
            assertEquals(null, httpResponse_4);
            
            final HttpResponse httpResponse_5 = awaitValue(httpClient_admin.send(HttpPost.builder("/put/rule")
                                                                           .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                           .body(rules.getJsonObject(0))
                                                                           .build()));
            assertEquals(200, httpResponse_5.statusCode());
            assertEquals(null, httpResponse_5.bodyAsText());
            
            final HttpResponse httpResponse_6 = awaitValue(httpClient.send(HttpGet.of("/serverOne/hello/get")));
            assertEquals("get", httpResponse_6.bodyAsText());
            
            final HttpResponse httpResponse_7 = awaitValue(httpClient_admin.send(HttpPost.builder("/update/token")
                                                                           .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                           .putHeader(ReverseProxyServer.adminNewTokenHeaderKey(), "tokenBis")
                                                                           .build()));
            assertEquals(200, httpResponse_7.statusCode());
            
            final HttpResponse httpResponse_8 = awaitValue(httpClient_admin.send(HttpGet.builder("/load/rules")
                                                                           .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                           .build()));
            assertEquals(403, httpResponse_8.statusCode());
            
            final HttpResponse httpResponse_9 = awaitValue(httpClient_admin.send(HttpGet.builder("/load/rules")
                                                                           .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "tokenBis")
                                                                           .build()));
            assertEquals(200, httpResponse_9.statusCode());
            assertEquals("{\"from\":{\"host\":\"localhost\",\"path\":\"/serverOne\"},"
                          + "\"to\":{\"ssl\":false,\"host\":\"localhost\",\"ports\":[1111],\"cutsPathWith\":\"/serverOne\"}}", 
                         httpResponse_9.bodyAsJsonArray().getJsonObject(0).encode());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            if (server != null) {
                await(server.stop());
                assertTrue(server.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }
    
    @Test
    public void test_04() {
        HttpServer server = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            server = awaitValue(server(1111));
            reverseProxyServer = awaitValue(startProxy(7777, null, 8888, "token"));
            await(reverseProxyServer.startAdmin());
            
            final HttpClient httpClient = newHttpClient(false, 7777, "localhost");
            final HttpClient adminHttpClient = newHttpClient(false, 8888, "localhost");
            
            final HttpResponse httpResponse_1 = awaitValue(adminHttpClient.send(HttpGet.builder("/load/rules")
                                                                          .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                          .build()));
            assertEquals(200, httpResponse_1.statusCode());
            assertEquals("[]", httpResponse_1.bodyAsText());
            
            final Set<ReverseProxyRule> reverseProxyRules = new HashSet<>();
            reverseProxyRules.add(new ReverseProxyRule(Url.from("localhost", "/serverOne"),
                                                       Url.to(false, "localhost")
                                                          .cutsPathWith("/serverOne")
                                                          .port(1111)
                                                          .build()));
            reverseProxyRules.add(new ReverseProxyRule(Url.from("localhost", "/serverTwo"),
                                                       Url.to(false, "localhost")
                                                          .cutsPathWith("/serverTwo")
                                                          .startsPathWith("/hello")
                                                          .port(1111)
                                                          .build()));

            final HttpResponse httpResponse_2 = 
                awaitValue(adminHttpClient.send(HttpPost.builder("/put/rules")
                                                        .body(JsonArray.of(reverseProxyRules.toArray()))
                                                        .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                        .build()));
            assertEquals(200, httpResponse_2.statusCode());

            final HttpResponse httpResponse_3 = awaitValue(adminHttpClient.send(HttpGet.builder("/load/rules")
                                                                          .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                          .build()));
            assertEquals(200, httpResponse_3.statusCode());
            assertEquals("[{\"from\":{\"host\":\"localhost\",\"path\":\"/serverOne\"},"
                         + "\"to\":{\"ssl\":false,\"host\":\"localhost\",\"ports\":[1111],\"cutsPathWith\":\"/serverOne\"}},"
                        + "{\"from\":{\"host\":\"localhost\",\"path\":\"/serverTwo\"},"
                         + "\"to\":{\"ssl\":false,\"host\":\"localhost\",\"ports\":[1111],\"cutsPathWith\":\"/serverTwo\",\"startsPathWith\":\"/hello\"}}]",
                         httpResponse_3.bodyAsText());
        
            final HttpResponse httpResponse_4 = awaitValue(httpClient.send(HttpGet.of("/serverOne/hello/get")));
            assertEquals(200, httpResponse_4.statusCode());
            assertEquals("get", httpResponse_4.bodyAsText());

            final HttpResponse httpResponse_5 = awaitValue(httpClient.send(HttpGet.of("/serverTwo/get")));
            assertEquals(200, httpResponse_5.statusCode());
            assertEquals("get", httpResponse_5.bodyAsText());

            final HttpResponse httpResponse_6 = 
                    awaitValue(adminHttpClient.send(HttpPost.builder("/remove/rules")
                                                            .body(JsonArray.of(reverseProxyRules.stream()
                                                                                                .map(ReverseProxyRule::from)
                                                                                                .toList()
                                                                                                .toArray()))
                                                            .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                            .build()));
            assertEquals(200, httpResponse_6.statusCode());

            final HttpResponse httpResponse_7 = awaitValue(adminHttpClient.send(HttpGet.builder("/load/rules")
                                                                          .putHeader(ReverseProxyServer.adminTokenHeaderKey(), "token")
                                                                          .build()));
            assertEquals(200, httpResponse_7.statusCode());
            assertEquals("[]", httpResponse_7.bodyAsText());
            
            final HttpResponse httpResponse_8 = awaitValue(httpClient.send(HttpGet.of("/serverOne/hello/get")));
            assertEquals(null, httpResponse_8);

            final HttpResponse httpResponse_9 = awaitValue(httpClient.send(HttpGet.of("/serverTwo/get")));
            assertEquals(null, httpResponse_9);
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            if (server != null) {
                await(server.stop());
                assertTrue(server.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }
    
    public static Future<HttpServer> server(final int port) {
        final Handler<HttpRouting> get = v -> {
            v.response().send("get");
        };
        final Handler<HttpRouting> post = v -> {
            v.response().send("post");
        };
        final WebService getHello = new WebService(HttpMethod.GET, "/hello/get", get);
        final WebService postHello = new WebService(HttpMethod.POST, "/hello/post", post);
        final HttpRouter.Builder httpRouterBuilder =
            HttpRouter.builder()
                      .session(new HttpSessionStore("session.yoja", Duration.ofDays(365)))
                      .webService(getHello)
                      .webService(postHello);
        final HttpRouter httpRouter = httpRouterBuilder.build();
        
        final HttpServer.Builder httpServerBuilder = HttpServer.builder(httpRouter, port);
        return httpServerBuilder.start();
    }

}
