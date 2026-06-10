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
package com.easygoingapi.yoja.reverse.proxy.util;

import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;

import java.time.Duration;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpEngine;
import com.easygoingapi.yoja.http.client.WebSocketClient;
import com.easygoingapi.yoja.http.client.WebSocketEngine;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyServer;

import io.vertx.core.Future;

public class TestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);
    
    private static HttpEngine httpEngine = new HttpEngine();
    private static WebSocketEngine webSocketEngine = new WebSocketEngine();
    
    public static WebSocketEngine webSocketEngine() {
        return webSocketEngine;
    }
    
    public static Future<ReverseProxyServer> startProxy(final int port, 
                                                        final Set<ReverseProxyRule> reverseProxyRules) {
        return startProxy(port, reverseProxyRules, false, false);
    }
    
    public static Future<ReverseProxyServer> startProxy(final int port, 
                                                        final Set<ReverseProxyRule> reverseProxyRules,
                                                        final int adminPort,
                                                        final String token) {
        final ReverseProxyServer.Builder reverseProxyServer = ReverseProxyServer.builder(port);
        reverseProxyServer.admin(adminPort, token);
        return reverseProxyServer.rules(reverseProxyRules)
                                 .start()
                                 .onSuccess(v -> LOGGER.info("REVERSE PROXY SERVER STARTED"));
    }

    public static Future<ReverseProxyServer> startProxy(final int port, 
                                                        final Set<ReverseProxyRule> reverseProxyRules,
                                                        final boolean sslSelfSigned,
                                                        final boolean sslProxy) {
        final ReverseProxyServer.Builder reverseProxyServer = ReverseProxyServer.builder(port);
        if (sslSelfSigned) {
            reverseProxyServer.sslSelfSigned();
        }
        reverseProxyServer.sslProxy(sslProxy);
        return reverseProxyServer.rules(reverseProxyRules)
                                 .onResolve(h -> LOGGER.info("ON_RESOLVE {}", h))
                                 .start()
                                 .onSuccess(v -> LOGGER.info("REVERSE PROXY SERVER STARTED"));
    }
    
    public static HttpClient newHttpClient(final boolean ssl, 
                                           final int port,
                                           final String host) {
        return HttpClient.builder(httpEngine)
                         .ssl(ssl)
                         .timeout(Duration.ofSeconds(3))
                         .port(port)
                         .host(host)
                         .build();
    }
    
    public static WebSocketClient newWebSocketClient(final boolean ssl,
                                                     final int port, 
                                                     final String host,
                                                     final String path) {
        final Future<WebSocketClient> futureWebSocketClient = 
            WebSocketClient.builder(webSocketEngine, path)
                           .port(port)
                           .ssl(ssl)
                           .host(host)
                           .timeout(Duration.ofSeconds(3))
                           .connect()
                           .onFailure(e -> LOGGER.error("webSocket connection failed", e));
        return awaitValue(futureWebSocketClient);
    }
    
}
