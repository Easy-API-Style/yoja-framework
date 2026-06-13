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
package com.easygoingapi.yoja.web.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.http.server.WebSocket;
import com.easygoingapi.yoja.selenium.ScriptOption;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class TestWebSocket {

    @TestFactory
    public Stream<DynamicNode> testWebSocket_01() {
        final AtomicReference<String> message = new AtomicReference<>();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onTextMessage(v -> {
            message.set(v.message());
        });
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(60), "/TestWebSocket_01.js")
                         .loadModule("/TestWebSocket_02.js")
                         .await(Duration.ofSeconds(5))
                         .test("send message", c -> webSocket.send("hello"))
                         .await(Duration.ofSeconds(5))
                         .test("close", c -> webSocket.close((short) 200, "close websocket"))
                         .await(Duration.ofSeconds(5))
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_03.js")
                         .test("check message from websocket", c -> assertEquals("bye", message.get()))
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testWebSocket_02() {
        final AtomicReference<String> message = new AtomicReference<>();
        
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onTextMessage(v -> {
            message.set(v.message());
        });
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(5), "/TestWebSocket_01.js")
                         .loadModule("/TestWebSocket_02.js")
                         .await(Duration.ofSeconds(5))
                         .test("send message", c -> webSocket.send("hello"))
                         .await(Duration.ofSeconds(5))
                         .test("close", c -> webSocket.close((short) 200, "close websocket"))
                         .await(Duration.ofSeconds(5))
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_03.js")
                         .test("check message from websocket", c -> assertEquals("bye", message.get()))
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testWebSocket_03() {
        final AtomicReference<String> message = new AtomicReference<>();
        
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onClose(v -> {
            message.set(v.statusCode() + " " + v.reason());
        });
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(5), "/TestWebSocket_01.js")
                         .loadModule("/TestWebSocket_04.js")
                         .await(Duration.ofSeconds(10))
                         .test("check message from websocket", c -> assertEquals("3010 why not", message.get()))
                         .stream();
    }

    @TestFactory
    public Stream<DynamicNode> testWebSocket_04() {
        final AtomicReference<String> message = new AtomicReference<>();
        
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onClose(v -> {
            message.set(v.statusCode() + " " + v.reason());
        });
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(60), "/TestWebSocket_01.js")
                         .loadModule("/TestWebSocket_04.js")
                         .await(Duration.ofSeconds(10))
                         .test("check message from websocket", c -> assertEquals("3010 why not", message.get()))
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testWebSocket_05() {
        final AtomicReference<String> onOpen = new AtomicReference<>();
        final AtomicReference<String> message = new AtomicReference<>();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onOpen(v -> {
            onOpen.set(v.path());
        });
        webSocket.onTextMessage(v -> {
            message.set(v.message());
        });
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(5), "/TestWebSocket_01.js")
                         .test("close webSocket", c -> webSocket.close())
                         .await(Duration.ofSeconds(5))
                         .repeatTestModuleUntil(Duration.ofSeconds(5), "/TestWebSocket_07.js")
                         .loadModule("/TestWebSocket_08.js")
                         .await(Duration.ofSeconds(10))
                         .test("check message from websocket", c -> assertEquals("hi!", message.get()))
                         .test("check open from websocket", c -> assertEquals("/websocket/test", onOpen.get()))
                         .loadModule("/TestWebSocket_09.js")
                         .await(Duration.ofSeconds(10))
                         .test("check message from websocket", c -> assertEquals("yo!", message.get()))
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testWebSocket_06() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestWebSocket_10.js")
                         .stream();
    }
    
}
