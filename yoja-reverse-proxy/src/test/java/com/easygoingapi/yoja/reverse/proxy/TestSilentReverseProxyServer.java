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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.client.WebSocketClient;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpRouting;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.HttpSessionStore;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.http.server.WebSocket;
import com.easygoingapi.yoja.http.server.WebSocketService;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyResult;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyServer;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;
import com.easygoingapi.yoja.reverse.proxy.util.TestUtil;

import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.UpgradeRejectedException;

public class TestSilentReverseProxyServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSilentReverseProxyServer.class);

    @Test
    public void test_01() {
        final boolean silent = true;
        
        HttpServer server_1 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            final HttpClient httpClient = newHttpClient(false, 7777, "localhost");
            
            final AtomicBoolean open = new AtomicBoolean();
            final WebSocket webSocket = new WebSocket("/websocket/test");
            webSocket.onOpen(v -> {
                open.set(true);
            });
            
            final WebSocketService webSocketService = new WebSocketService();
            webSocketService.add(webSocket);
            server_1 = awaitValue(server(1111, webSocketService));
            
            final AtomicReference<ReverseProxyResult> reverseProxyResult = new AtomicReference<>();
            final Handler<ReverseProxyResult> onResolve = v -> {
                reverseProxyResult.set(v);
            };
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
            reverseProxyServer = awaitValue(startProxy(7777, silent, reverseProxyRules, onResolve));
            
            final Future<HttpResponse> future_1 = httpClient.send(HttpGet.of("/noway"));
            final HttpResponse httpResponse_1 = awaitValue(future_1);
            assertEquals(true, future_1.failed());
            assertEquals(null, httpResponse_1);
            
            assertFalse(reverseProxyResult.get().isResolved());
            assertEquals("HttpUrl [protocol=http, host=localhost, port=7777, path=/noway]", 
                         reverseProxyResult.get().fromUrl().toString());
            assertEquals(null, reverseProxyResult.get().toUrl());
            
            final Future<WebSocketClient> webSocetFuture_1 = newWebSocketClient(7777, "/noWebSocket");
            await(webSocetFuture_1);
            assertTrue(webSocetFuture_1.failed());
            assertEquals(WebSocketHandshakeException.class, webSocetFuture_1.cause().getClass());
            assertEquals("Connection closed while handshake in process", webSocetFuture_1.cause().getMessage());
            
            assertFalse(reverseProxyResult.get().isResolved());
            assertEquals("HttpUrl [protocol=ws, host=localhost, port=7777, path=/noWebSocket]", 
                         reverseProxyResult.get().fromUrl().toString());
            assertEquals(null, reverseProxyResult.get().toUrl());
            
            final Future<HttpResponse> future_2 = httpClient.send(HttpGet.of("/serverOne/hello/get"));
            final HttpResponse httpResponse_2 = awaitValue(future_2);
            assertFalse(future_2.failed());
            assertEquals(200, httpResponse_2.statusCode());
            
            assertTrue(reverseProxyResult.get().isResolved());
            assertEquals("HttpUrl [protocol=http, host=localhost, port=7777, path=/serverOne/hello/get]", 
                         reverseProxyResult.get().fromUrl().toString());
            assertEquals("HttpUrl [protocol=http, host=localhost, port=1111, path=/hello/get]",
                         reverseProxyResult.get().toUrl().toString());
            
            final Future<WebSocketClient> webSocetFuture_2 = newWebSocketClient(7777, "/serverOne/websocket/test");
            await(webSocetFuture_2);
            assertFalse(webSocetFuture_2.failed());
            
            assertTrue(reverseProxyResult.get().isResolved());
            assertEquals("HttpUrl [protocol=ws, host=localhost, port=7777, path=/serverOne/websocket/test]", 
                         reverseProxyResult.get().fromUrl().toString());
            assertEquals("HttpUrl [protocol=wss, host=localhost, port=1111, path=/websocket/test]",
                         reverseProxyResult.get().toUrl().toString());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
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
        final boolean silent = false;
        HttpServer server_1 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            final HttpClient httpClient = newHttpClient(false, 7777, "localhost");
            
            final AtomicBoolean open = new AtomicBoolean();
            final WebSocket webSocket = new WebSocket("/websocket/test");
            webSocket.onOpen(v -> {
                open.set(true);
            });
            
            final WebSocketService webSocketService = new WebSocketService();
            webSocketService.add(webSocket);
            server_1 = awaitValue(server(1111, webSocketService));
            
            final AtomicReference<ReverseProxyResult> reverseProxyResult = new AtomicReference<>();
            final Handler<ReverseProxyResult> onResolve = v -> {
                reverseProxyResult.set(v);
            };
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
            reverseProxyServer = awaitValue(startProxy(7777, silent, reverseProxyRules, onResolve));
            
            final Future<HttpResponse> future_4 = httpClient.send(HttpGet.of("/noway"));
            final HttpResponse httpResponse_4 = awaitValue(future_4);
            assertEquals(false, future_4.failed());
            assertEquals(404, httpResponse_4.statusCode());
            
            final Future<WebSocketClient> future = newWebSocketClient(7777, "/noway");
            await(future);
            assertTrue(future.failed());
            assertEquals(UpgradeRejectedException.class, future.cause().getClass());
            assertEquals("WebSocket upgrade failure: 502", future.cause().getMessage());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }
    
    public static Future<WebSocketClient> newWebSocketClient(final int port, 
                                                             final String path) {
        return WebSocketClient.builder(TestUtil.webSocketEngine(), path)
                              .port(port)
                              .host("localhost")
                              .ssl(false)
                              .timeout(Duration.ofSeconds(3))
                              .connect();
    }

    public static Future<ReverseProxyServer> startProxy(final int port, 
                                                        final boolean silent, 
                                                        final Set<ReverseProxyRule> reverseProxyRules,
                                                        final Handler<ReverseProxyResult> onResolve) {
        final ReverseProxyServer.Builder reverseProxyServer = ReverseProxyServer.builder(port);
        return reverseProxyServer.rules(reverseProxyRules)
                                 .onResolve(onResolve)
                                 .silent(silent)
                                 .start();
    }
    
    public static Future<HttpServer> server(final int port, 
                                            final WebSocketService webSocketService) {
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
        if (webSocketService != null) {
            httpServerBuilder.webSocketService(webSocketService);
        }
        return httpServerBuilder.start();
    }
    
}
