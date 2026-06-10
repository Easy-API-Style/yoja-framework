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
import static com.easygoingapi.yoja.http.server.util.TestUtil.newHttpServer;
import static com.easygoingapi.yoja.http.server.util.TestUtil.newWebSocketClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.http.client.WebSocketClient;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.http.server.WebSocket.On;
import com.easygoingapi.yoja.http.server.WebSocketService.Status;

import io.vertx.core.CompositeFuture;

public class TestWebSocket {
    
    private static Logger LOGGER = LoggerFactory.getLogger(TestWebSocket.class);

    @Test
    public void test_01() {
        testWebSocket(9999, "localhost", false);
    }
    
    @Test
    public void test_02() {
        testWebSocket(9999, "localhost", true);
    }
    
    public void testWebSocket(final int port, 
                              final String host,
                              final boolean ssl) {
        final HttpRouter httpRouter = HttpRouter.builder().build();
        
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
        final HttpServer httpServer = newHttpServer(ssl, port, httpRouter, webSocketService);
        try {
            final WebSocketClient webSocketClient = newWebSocketClient(ssl, port, host, "/websocket/test");
            
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
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_03() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .contextPath("/path")
                                                .build();
        
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
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            final WebSocketClient webSocketClient = newWebSocketClient("/path/websocket/test?parameter=OK&done&parameter=good");
            
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
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_04() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .contextPath("/path")
                                                .build();
        final WebSocket webSocket = new WebSocket("/websocket/test",
                                                  p -> "OK".equals(p.firstValue("parameter")));
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/path/websocket/test?parameter=OK");
            assertTrue(webSocket.isOpened());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_05() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .contextPath("/path")
                                                .build();
        final WebSocket webSocket = new WebSocket("/websocket/test",
                                                  p -> "OK".equals(p.firstValue("parameter")));
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/path/websocket/test?parameter=KO");
            assertFalse(webSocket.isOpened());
            
            newWebSocketClient("/websocket/test?parameter=OK");
            assertFalse(webSocket.isOpened());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_06() {
        Exception exception = null;
        try {
            new WebSocket("websocket/test");
        }
        catch (final Exception e) {
            exception = e;
        }
        assertEquals("WebSocket path must begin with '/'", exception.getMessage());
    }
    
    @Test
    public void test_07() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final AtomicReference<Short> close = new AtomicReference<>();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onClose(v -> {
            close.set(v.statusCode());
        });
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/websocket/test");
            assertEquals(true, webSocket.isOpened());
            await(webSocket.close((short) 1010));
            assertEquals((short) 1010, close.get());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_08() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final AtomicReference<Short> close = new AtomicReference<>();
        final AtomicReference<String> message = new AtomicReference<>();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onClose(v -> {
            close.set(v.statusCode());
            message.set(v.reason());
        });
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/websocket/test");
            assertEquals(true, webSocket.isOpened());
            await(webSocket.close((short) 1009, "REASON"));
            assertEquals((short) 1009, close.get());
            assertEquals("REASON", message.get());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_09() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final AtomicBoolean open = new AtomicBoolean();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onOpen(v -> {
            open.set(true);
        });
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/websocket/test");
            assertTrue(webSocket.isOpened());
            assertTrue(open.get());
            open.set(false);
            webSocket.clear(On.open);
            await(webSocket.close());
            
            newWebSocketClient("/websocket/test");
            assertTrue(webSocket.isOpened());
            assertEquals(false, open.get());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_10() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final AtomicReference<String> byteMessage = new AtomicReference<>();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onBinaryMessage(v -> {
            byteMessage.set(new String(v.message()));
        });
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            final WebSocketClient webSocketClient = newWebSocketClient("/websocket/test");
            assertTrue(webSocket.isOpened());
            await(webSocketClient.send("hello client".getBytes()));
            assertEquals("hello client", byteMessage.get());
            byteMessage.set(null);
            
            webSocket.clear(On.binaryMessage);
            await(webSocketClient.send("hello client".getBytes()));
            assertNull(byteMessage.get());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_11() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final AtomicReference<String> textMessage = new AtomicReference<>();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onTextMessage(v -> {
            textMessage.set(v.message());
        });
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            final WebSocketClient webSocketClient = newWebSocketClient("/websocket/test");
            
            assertTrue(webSocket.isOpened());
            await(webSocketClient.send("hello client"));
            assertEquals("hello client", textMessage.get());
            textMessage.set(null);
            
            webSocket.clear(On.textMessage);
            await(webSocketClient.send("hello client"));
            assertNull(textMessage.get());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_12() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        final AtomicBoolean close = new AtomicBoolean();
        webSocket.onClose(v -> {
            close.set(true);
        });
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/websocket/test");
            assertTrue(webSocket.isOpened());
            await(webSocket.close());
            assertTrue(close.get());
            close.set(false);
            
            webSocket.clear(On.close);
            
            newWebSocketClient("/websocket/test");
            assertTrue(webSocket.isOpened());
            await(webSocket.close());
            assertEquals(false, close.get());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_13() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket);
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/websocket");
            assertFalse(webSocket.isOpened());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_14() {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .build();
        final WebSocket webSocket_1 = new WebSocket("/websocket/test_1");
        final WebSocket webSocket_2 = new WebSocket("/websocket/test_2");
        final WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(webSocket_1);
        webSocketService.add(webSocket_2);
        assertEquals(webSocket_1, webSocketService.add(webSocket_1));
        assertTrue(webSocket_1.equals(webSocket_1));
        assertFalse(webSocket_1.equals(webSocket_2));
        assertTrue(webSocketService.hasWebSocket("/websocket/test_1"));
        assertEquals("/websocket/test_1", webSocketService.getWebSocket("/websocket/test_1").getPath());
        assertEquals(Set.of("/websocket/test_1", "/websocket/test_2"), webSocketService.getWebSocketPaths());
        assertEquals(Set.of("/websocket/test_1", "/websocket/test_2"), webSocketService.getWebSocketPaths(Status.close));
        assertEquals(Set.of(), webSocketService.getWebSocketPaths(Status.open));
        
        final HttpServer httpServer = newHttpServer(httpRouter, webSocketService);
        try {
            newWebSocketClient("/websocket/test_1");
            assertEquals(Set.of("/websocket/test_2"), webSocketService.getWebSocketPaths(Status.close));
            assertEquals(Set.of("/websocket/test_1"), webSocketService.getWebSocketPaths(Status.open));
            
            await(webSocketService.remove("/websocket/test_1"));
            assertFalse(webSocketService.hasWebSocket("/websocket/test_1"));
            assertEquals(Set.of("/websocket/test_2"), webSocketService.getWebSocketPaths(Status.close));
            assertEquals(Set.of(), webSocketService.getWebSocketPaths(Status.open));
            
            CompositeFuture removeFuture = webSocketService.remove("/websocket/test_2");
            await(removeFuture);
            assertTrue(!removeFuture.failed());
            
            removeFuture = webSocketService.remove("/websocket/test_2");
            await(removeFuture);
            assertTrue(!removeFuture.failed());
            
            assertEquals(webSocketService, httpServer.webSocketService());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
}
