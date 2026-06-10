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
import static com.easygoingapi.yoja.reverse.proxy.util.TestUtil.newWebSocketClient;
import static com.easygoingapi.yoja.reverse.proxy.util.TestUtil.startProxy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpCookie;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpPost;
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
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyServer;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;
import com.google.common.base.Strings;

import io.vertx.core.Future;
import io.vertx.core.Handler;

public class TestReverseProxyServer {

    private static Logger LOGGER = LoggerFactory.getLogger(TestReverseProxyServer.class);
    
    @Test
    public void test_01() {
        HttpServer server_1 = null;
        HttpServer server_2 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            server_1 = awaitValue(server(1, 1111));
            server_2 = awaitValue(server(2, 2222));
            
            final Set<ReverseProxyRule> reverseProxyRules = rules();
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules));
            
            final HttpClient httpClient = newHttpClient(false, 7777, "localhost");
            final HttpResponse httpResponse_1 = awaitValue(httpClient.send(HttpGet.of("/serverOne/hello/forest", "name=Paul")));
            final HttpResponse httpResponse_2 = awaitValue(httpClient.send(HttpGet.of("/serverTwo/hello/forest", "name=Jérôme")));
            final HttpResponse httpResponse_3 = awaitValue(httpClient.send(HttpGet.of("/hello/forest", "name=Jeanne")));
            final HttpResponse httpResponse_4 = awaitValue(httpClient.send(HttpGet.of("/path/road/hello/forest", "name=Romuald")));
            assertEquals("server 1 hello Paul", httpResponse_1.bodyAsText());
            assertEquals("server 2 hello Jérôme", httpResponse_2.bodyAsText());
            assertEquals("server 2 hello Jeanne", httpResponse_3.bodyAsText());
            assertEquals("server 2 hello Romuald", httpResponse_4.bodyAsText());
        }
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (server_2 != null) {
                await(server_2.stop());
                assertTrue(server_2.is(State.stopped));
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
        HttpServer server_1 = null;
        HttpServer server_2 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            server_1 = awaitValue(server(1, 1111));
            server_2 = awaitValue(server(2, 2222));
            
            final Set<ReverseProxyRule> reverseProxyRules = rules();
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules));
            
            final HttpClient httpClient = newHttpClient(false, 7777, "localhost");
            final HttpResponse httpResponse_1 = awaitValue(httpClient.send(HttpPost.of("/serverOne/hello")));
            assertEquals("server 1 hello  ", httpResponse_1.bodyAsText());
            assertEquals(true, httpResponse_1.hasHeader("header"));
            assertEquals("headerValue", httpResponse_1.header("header"));
            assertEquals(1, httpResponse_1.cookies("cookie").size());
            assertEquals("HttpCookie [name=cookie, value=cookieValue, maxAge=0, sameSite=null, httpOnly=false, secure=false]", 
                         httpResponse_1.cookies("cookie").iterator().next().toString());
            
            final HttpResponse httpResponse_2 = awaitValue(httpClient.send(HttpGet.builder("/serverOne/hello/forest")
                                                                                  .putHeader("header_1", "value_1")
                                                                                  .putHeader("header_2", "value_2")
                                                                                  .build()));
            assertEquals("header_1=value_1;header_2=value_2", httpResponse_2.bodyAsText());
            
            final HttpResponse httpResponse_3 = awaitValue(httpClient.send(HttpGet.builder("/serverOne/hello/forest")
                                                                                   .putCookie("cookie_1", "value_1")
                                                                                   .putCookie("cookie_2", "value_2")
                                                                                   .build()));
            assertEquals("cookie_1=value_1;cookie_2=value_2", httpResponse_3.bodyAsText());
        }
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (server_2 != null) {
                await(server_2.stop());
                assertTrue(server_2.is(State.stopped));
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
        HttpServer server_1 = null;
        HttpServer server_2 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            final AtomicReference<String> textMessage = new AtomicReference<>();
            final AtomicReference<String> byteMessage = new AtomicReference<>();
            final AtomicBoolean open = new AtomicBoolean();
            final AtomicBoolean close = new AtomicBoolean();
            final WebSocket webSocket = new WebSocket("/websocket/test");
            webSocket.onOpen(v -> {
                open.set(true);
                LOGGER.debug("open {}", v);
            });
            webSocket.onBinaryMessage(v -> {
                byteMessage.set(new String(v.message()));
                LOGGER.debug("message {}", new String(v.message()));
            });
            webSocket.onTextMessage(v -> {
                textMessage.set(v.message());
                LOGGER.debug("message {}", v);
            });
            webSocket.onClose(v -> {
                close.set(true);
                LOGGER.debug("close {}", v);
            });
            
            final WebSocketService webSocketService = new WebSocketService();
            webSocketService.add(webSocket);
            server_1 = awaitValue(server(1, 1111, webSocketService));
            server_2 = awaitValue(server(2, 2222));
            
            final Set<ReverseProxyRule> reverseProxyRules = rules();
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules));
            final WebSocketClient webSocketClient = newWebSocketClient(false, 7777, 
                                                                       "localhost", 
                                                                       "/serverOne/websocket/test");
            assertTrue(open.get());
            final AtomicReference<String> textMessageClient = new AtomicReference<>();
            final AtomicReference<String> byteMessageClient = new AtomicReference<>();
            final AtomicBoolean closeClient = new AtomicBoolean();
            webSocketClient.onTextMessage(v -> {
                textMessageClient.set(v.message());
            });
            webSocketClient.onBinaryMessage(v -> {
                byteMessageClient.set(new String(v.message()));
            });
            webSocketClient.onClose(v -> {
                closeClient.set(true);
            });
            await(webSocket.send("hello client"));
            assertTrue(webSocket.isOpened());
            assertEquals("hello client", textMessageClient.get());
            await(webSocket.send("bye client".getBytes()));
            assertEquals("bye client", byteMessageClient.get());
            
            await(webSocketClient.send("byte message".getBytes()));
            assertEquals("byte message", byteMessage.get());
            await(webSocketClient.send("text message"));
            assertEquals("text message", textMessage.get());
            
            await(webSocketClient.close());
            assertTrue(close.get());
            assertTrue(closeClient.get());
        }
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (server_2 != null) {
                await(server_2.stop());
                assertTrue(server_2.is(State.stopped));
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
        HttpServer server_1 = null;
        HttpServer server_2 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            final AtomicReference<String> textMessage = new AtomicReference<>();
            final AtomicReference<String> byteMessage = new AtomicReference<>();
            final AtomicBoolean open = new AtomicBoolean();
            final AtomicBoolean close = new AtomicBoolean();
            
            final WebSocket webSocket = new WebSocket("/websocket/test",
                                                      p -> { 
                                                          return "OK".equals(p.firstValue("parameter"))
                                                                    && p.size() == 3
                                                                    && !p.isEmpty()
                                                                    && p.entries().size() == 3
                                                                    && p.names().size() == 2
                                                                    && p.values("parameter").size() == 2
                                                                    && p.values("done").size() == 0
                                                                    && p.hasName("done");
                                                          });
            
            webSocket.onOpen(v -> {
                open.set(true);
            });
            webSocket.onBinaryMessage(v -> {
                byteMessage.set(new String(v.message()));
            });
            webSocket.onTextMessage(v -> {
                textMessage.set(v.message());
            });
            webSocket.onClose(v -> {
                close.set(true);
            });
            
            final WebSocketService webSocketService = new WebSocketService();
            webSocketService.add(webSocket);
            server_1 = awaitValue(server(1, 1111, webSocketService));
            server_2 = awaitValue(server(2, 2222));
            
            final Set<ReverseProxyRule> reverseProxyRules = rules();
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules));
            final WebSocketClient webSocketClient = newWebSocketClient(false, 7777, 
                                                                       "localhost", 
                                                                       "/serverOne/websocket/test?parameter=OK&done&parameter=good");
            assertTrue(open.get());
            final AtomicReference<String> textMessageClient = new AtomicReference<>();
            final AtomicReference<String> byteMessageClient = new AtomicReference<>();
            final AtomicBoolean closeClient = new AtomicBoolean();
            webSocketClient.onTextMessage(v -> {
                textMessageClient.set(v.message());
            });
            webSocketClient.onBinaryMessage(v -> {
                byteMessageClient.set(new String(v.message()));
            });
            webSocketClient.onClose(v -> {
                closeClient.set(true);
            });
            await(webSocket.send("hello client"));
            assertEquals("hello client", textMessageClient.get());
            await(webSocket.send("bye client".getBytes()));
            assertEquals("bye client", byteMessageClient.get());
            
            await(webSocketClient.send("byte message".getBytes()));
            assertEquals("byte message", byteMessage.get());
            await(webSocketClient.send("text message"));
            assertEquals("text message", textMessage.get());
            await(webSocket.close());
            assertTrue(close.get());
            assertTrue(closeClient.get());
        }
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (server_2 != null) {
                await(server_2.stop());
                assertTrue(server_2.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }
    
    @Test
    public void test_05() {
        HttpServer server_1 = null;
        HttpServer server_2 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            final boolean ssl = true;
            final boolean sslProxy = false;
            server_1 = awaitValue(server(1, 1111, null, sslProxy));
            server_2 = awaitValue(server(2, 2222, null, sslProxy));
            
            final Set<ReverseProxyRule> reverseProxyRules = rules();
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules, ssl, sslProxy));
            
            final HttpClient httpClient = newHttpClient(ssl, 7777, "localhost");
            final HttpResponse httpResponse_1 = awaitValue(httpClient.send(HttpGet.of("/serverOne/hello/forest", "name=Paul")));
            final HttpResponse httpResponse_2 = awaitValue(httpClient.send(HttpGet.of("/serverTwo/hello/forest", "name=Jérôme")));
            final HttpResponse httpResponse_3 = awaitValue(httpClient.send(HttpGet.of("/hello/forest", "name=Jeanne")));
            final HttpResponse httpResponse_4 = awaitValue(httpClient.send(HttpGet.of("/path/road/hello/forest", "name=Romuald")));
            assertEquals("server 1 hello Paul", httpResponse_1.bodyAsText());
            assertEquals("server 2 hello Jérôme", httpResponse_2.bodyAsText());
            assertEquals("server 2 hello Jeanne", httpResponse_3.bodyAsText());
            assertEquals("server 2 hello Romuald", httpResponse_4.bodyAsText());
        }
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (server_2 != null) {
                await(server_2.stop());
                assertTrue(server_2.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }

    @Test
    public void test_07() {
        HttpServer server_1 = null;
        HttpServer server_2 = null;
        ReverseProxyServer reverseProxyServer = null;
        
        final boolean ssl = true;
        try {
            final AtomicReference<String> textMessage = new AtomicReference<>();
            final AtomicReference<String> byteMessage = new AtomicReference<>();
            final AtomicBoolean open = new AtomicBoolean();
            final AtomicBoolean close = new AtomicBoolean();
            final WebSocket webSocket = new WebSocket("/websocket/test");
            webSocket.onOpen(v -> {
                open.set(true);
                LOGGER.debug("open {}", v);
            });
            webSocket.onBinaryMessage(v -> {
                byteMessage.set(new String(v.message()));
                LOGGER.debug("message {}", new String(v.message()));
            });
            webSocket.onTextMessage(v -> {
                textMessage.set(v.message());
                LOGGER.debug("message {}", v);
            });
            webSocket.onClose(v -> {
                close.set(true);
                LOGGER.debug("close {}", v);
            });
            
            final WebSocketService webSocketService = new WebSocketService();
            webSocketService.add(webSocket);
            server_1 = awaitValue(server(1, 1111, webSocketService));
            server_2 = awaitValue(server(2, 2222));
            
            final Set<ReverseProxyRule> reverseProxyRules = rules();
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules, ssl, false));
            final WebSocketClient webSocketClient = newWebSocketClient(ssl,
                                                                       7777, 
                                                                       "localhost", 
                                                                       "/serverOne/websocket/test");
            assertTrue(open.get());
            final AtomicReference<String> textMessageClient = new AtomicReference<>();
            final AtomicReference<String> byteMessageClient = new AtomicReference<>();
            final AtomicBoolean closeClient = new AtomicBoolean();
            webSocketClient.onTextMessage(v -> {
                textMessageClient.set(v.message());
            });
            webSocketClient.onBinaryMessage(v -> {
                byteMessageClient.set(new String(v.message()));
            });
            webSocketClient.onClose(v -> {
                closeClient.set(true);
            });
            await(webSocket.send("hello client"));
            assertTrue(webSocket.isOpened());
            assertEquals("hello client", textMessageClient.get());
            await(webSocket.send("bye client".getBytes()));
            assertEquals("bye client", byteMessageClient.get());
            
            await(webSocketClient.send("byte message".getBytes()));
            assertEquals("byte message", byteMessage.get());
            await(webSocketClient.send("text message"));
            assertEquals("text message", textMessage.get());
            
            await(webSocketClient.close());
            assertTrue(close.get());
            assertTrue(closeClient.get());
        }
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (server_2 != null) {
                await(server_2.stop());
                assertTrue(server_2.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }

    @Test
    public void test_06() {
        HttpServer server_1 = null;
        HttpServer server_2 = null;
        ReverseProxyServer reverseProxyServer = null;
        try {
            final boolean ssl = true;
            final boolean sslProxy = true;
            server_1 = awaitValue(server(1, 1111, null, sslProxy));
            server_2 = awaitValue(server(2, 2222, null, sslProxy));
            
            final Set<ReverseProxyRule> reverseProxyRules = rules();
            reverseProxyServer = awaitValue(startProxy(7777, reverseProxyRules, ssl, sslProxy));
            
            final HttpClient httpClient = newHttpClient(ssl, 7777, "localhost");
            final HttpResponse httpResponse_1 = awaitValue(httpClient.send(HttpGet.of("/serverOne/hello/forest", "name=Paul")));
            final HttpResponse httpResponse_2 = awaitValue(httpClient.send(HttpGet.of("/serverTwo/hello/forest", "name=Jérôme")));
            final HttpResponse httpResponse_3 = awaitValue(httpClient.send(HttpGet.of("/hello/forest", "name=Jeanne")));
            final HttpResponse httpResponse_4 = awaitValue(httpClient.send(HttpGet.of("/path/road/hello/forest", "name=Romuald")));
            assertEquals("server 1 hello Paul", httpResponse_1.bodyAsText());
            assertEquals("server 2 hello Jérôme", httpResponse_2.bodyAsText());
            assertEquals("server 2 hello Jeanne", httpResponse_3.bodyAsText());
            assertEquals("server 2 hello Romuald", httpResponse_4.bodyAsText());
        }
        finally {
            if (server_1 != null) {
                await(server_1.stop());
                assertTrue(server_1.is(State.stopped));
            }
            if (server_2 != null) {
                await(server_2.stop());
                assertTrue(server_2.is(State.stopped));
            }
            if (reverseProxyServer != null) {
                await(reverseProxyServer.stop());
                assertEquals(State.stopped, reverseProxyServer.adminState());
                assertEquals(State.stopped, reverseProxyServer.proxyState());
            }
        }
    }
    
    private static Set<ReverseProxyRule> rules() {
        final Set<ReverseProxyRule> result = new HashSet<>();
        result.add(new ReverseProxyRule(Url.from("localhost", "/serverOne"),
                                                   Url.to(false, "localhost")
                                                      .cutsPathWith("/serverOne")
                                                      .port(1111)
                                                      .build()));
        result.add(new ReverseProxyRule(Url.from("localhost", "/serverTwo"),
                                                   Url.to(false, "localhost")
                                                      .cutsPathWith("/serverTwo")
                                                      .port(2222)
                                                      .build()));
        result.add(new ReverseProxyRule(Url.from("localhost"),
                                                   Url.to(false, "localhost")
                                                      .startsPathWith("/bye")
                                                      .port(2222)
                                                      .build()));
        result.add(new ReverseProxyRule(Url.from("localhost", "/path/road"),
                                                   Url.to(false, "localhost")
                                                      .cutsPathWith("/path/road")
                                                      .startsPathWith("/bye")
                                                      .port(2222)
                                                      .build()));
        return result;
    }
    
    public static Future<HttpServer> server(final int number, 
                                            final int port) {
        return server(number, port, null, false);
    }
    
    public static Future<HttpServer> server(final int number, 
                                            final int port,
                                            final WebSocketService webSocketService) {
        return server(number, port, webSocketService, false);
    }

    public static Future<HttpServer> server(final int number, 
                                            final int port,
                                            final WebSocketService webSocketService,
                                            final boolean sslSelfSigned) {
        final Handler<HttpRouting> get = v -> {
            if (v.request().hasParameter("name")) {
                v.response()
                 .send("server " 
                     + number + " hello " 
                     + v.request().firstParameter("name"));
            }
            else if (v.request().hasCookie()) {
                final Set<String> response = new TreeSet<>();
                for (final String name : v.request().cookies().keySet()) {
                    response.add(name + "=" + v.request().cookie(name));
                }
                v.response()
                 .send(String.join(";", response));
            }
            else if (v.request().hasHeader()) {
                final Set<String> response = new TreeSet<>();
                for (final String name : v.request().headerNames()) {
                    if (!name.equals("content-length") && !name.equals("user-agent")) {
                        response.add(name + "=" + v.request().header(name));
                    }
                }
                v.response()
                 .send(String.join(";", response));
            }
        };
        final Handler<HttpRouting> post = v -> {
            v.response().putHeader("header", "headerValue");
            v.response().addCookie(HttpCookie.of("cookie", "cookieValue"));
            v.response().send("server " + number + " hello " 
                             + Strings.nullToEmpty(v.request().firstParameter("name"))
                             + " "
                             + v.request().bodyAsText());
        };
        
        final WebService getHello_1 = new WebService(HttpMethod.GET, "/hello/forest", get);
        final WebService getHello_2 = new WebService(HttpMethod.GET, "/bye/hello/forest", get);
        final WebService postHello = new WebService(HttpMethod.POST, "/hello", post);
        final HttpRouter.Builder httpRouterBuilder =
            HttpRouter.builder()
                      .session(new HttpSessionStore("session.yoja", Duration.ofDays(365)))
                      .webService(getHello_1)
                      .webService(getHello_2)
                      .webService(postHello);
        final HttpRouter httpRouter = httpRouterBuilder.build();
        
        final HttpServer.Builder httpServerBuilder = HttpServer.builder(httpRouter, port);
        if (webSocketService != null) {
            httpServerBuilder.webSocketService(webSocketService);
        }
        if (sslSelfSigned) {
            httpServerBuilder.sslSelfSigned();
        }
        return httpServerBuilder.start();
    }

}
