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
import java.util.concurrent.atomic.AtomicInteger;
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
    
    /** The listeners in their order -- one taken off before any message not told, one failing not keeping the others
     *  from it, one taken off (twice) told no more. Closed: from zero. */
    @TestFactory
    public Stream<DynamicNode> testWebSocket_07() {
        final AtomicReference<String> message = new AtomicReference<>();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onTextMessage(v -> message.set(v.message()));
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_01.js")
                         .loadModule("/TestWebSocket_11.js")
                         .await(Duration.ofSeconds(2))
                         .test("the page listening", c -> assertEquals("ready", message.get()))
                         .test("send one", c -> webSocket.send("one"))
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_12.js")
                         .loadModule("/TestWebSocket_13.js")
                         .test("send two", c -> webSocket.send("two"))
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_14.js")
                         .testAsyncModule("/TestWebSocket_15.js")
                         .stream();
    }

    /** Cut by the server, the very same socket connects again, unseen -- its listeners kept, no 'close' told,
     *  'restart' told once connected --; closed by the page, it is not. */
    @TestFactory
    public Stream<DynamicNode> testWebSocket_08() {
        final AtomicInteger opens = new AtomicInteger();   // the server's, for every browser in turn
        final AtomicInteger before = new AtomicInteger();  // the ones before this browser's restart
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onOpen(v -> opens.incrementAndGet());
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_01.js")
                         .loadModule("/TestWebSocket_16.js")
                         .test("the server closes it", c -> {
                             before.set(opens.get());
                             webSocket.close();
                         })
                         .repeatTestModuleUntil(Duration.ofSeconds(15), "/TestWebSocket_17.js")
                         .test("connected again, once", c -> assertEquals(before.get() + 1, opens.get()))
                         .test("send after", c -> webSocket.send("after"))
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_18.js")
                         .loadModule("/TestWebSocket_19.js")
                         .await(Duration.ofSeconds(5))
                         .test("closed by the page: not opened again", c -> assertEquals(before.get() + 1, opens.get()))
                         .stream();
    }

    /** No more restart, cut at the moment: closed for good, 'close' told -- not connected again. */
    @TestFactory
    public Stream<DynamicNode> testWebSocket_09() {
        final AtomicInteger opens = new AtomicInteger();   // the server's, for every browser in turn
        final AtomicInteger before = new AtomicInteger();
        final WebSocket webSocket = new WebSocket("/websocket/test");
        webSocket.onOpen(v -> opens.incrementAndGet());
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.websocket")
                         .webSocket(webSocket)
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_01.js")
                         .loadModule("/TestWebSocket_16.js")
                         .test("the server closes it", c -> {
                             before.set(opens.get());
                             webSocket.close();
                         })
                         .loadModule("/TestWebSocket_20.js")
                         .repeatTestModuleUntil(Duration.ofSeconds(10), "/TestWebSocket_21.js")
                         .await(Duration.ofSeconds(4))
                         .test("not connected again", c -> assertEquals(before.get(), opens.get()))
                         .stream();
    }

}
