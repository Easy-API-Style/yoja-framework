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
package com.easygoingapi.yoja.http.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.YojaAppException;
import com.easygoingapi.yoja.http.client.WebSocketEngine;

import io.vertx.core.http.WebSocketClientOptions;

public class TestWebSocketEngine {

    @Test
    public void test_01() {
        final WebSocketEngine webSocketEngine = new WebSocketEngine();
        try {
            YojaApp.start();
        }
        catch (final Exception e) {
            assertEquals(YojaAppException.class, e.getClass());
            assertEquals("yoja app already started", e.getMessage());
        }
        finally {
            YojaApp.stop();
            YojaApp.awaitStop();
        }
    }

    @Test
    public void test_02() {
        final WebSocketEngine webSocketEngine = new WebSocketEngine();
        try {
            assertEquals("localhost", webSocketEngine.options().getDefaultHost());
            assertEquals(80, webSocketEngine.options().getDefaultPort());
        }
        finally {
            YojaApp.stop();
            YojaApp.awaitStop();
        }
    }
    
    @Test
    public void test_03() {
        final WebSocketEngine webSocketEngine = new WebSocketEngine(new WebSocketClientOptions()
                                                                          .setSsl(false));
        try {
            webSocketEngine.logOptions();
            assertEquals(false, webSocketEngine.options().isSsl());
        }
        finally {
            YojaApp.stop();
            YojaApp.awaitStop();
        }
    }
    
}
