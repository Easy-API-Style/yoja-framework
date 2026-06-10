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
package com.easygoingapi.yoja.http.server.util;

import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpEngine;
import com.easygoingapi.yoja.http.client.WebSocketClient;
import com.easygoingapi.yoja.http.client.WebSocketEngine;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.WebSocketService;

import io.vertx.core.Future;

public class TestUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);
    
    public static final int port = 8888;
    public static final String host = "localhost";
    public static final boolean ssl = true;
    
    private static HttpEngine httpEngine = new HttpEngine();
    private static WebSocketEngine webSocketEngine = new WebSocketEngine();
    
    static {
        httpEngine.logOptions();
    }
    
    /*
     * 
     * client
     * 
     */
    public static HttpClient newHttpClient(final boolean ssl,
                                           final Duration timeout,
                                           final int port, 
                                           final String host) {
        return HttpClient.builder(httpEngine)
                         .ssl(ssl)
                         .timeout(timeout)
                         .port(port)
                         .host(host)
                         .build();
    }
    
    public static HttpClient newHttpClient() {
        return newHttpClient(ssl, port, host);
    }
    
    public static HttpClient newHttpClient(final boolean ssl,
                                           final int port, 
                                           final String host) {
        return HttpClient.builder(httpEngine)
                         .ssl(ssl)
                         .port(port)
                         .host(host)
                         .build();
    }
    
    /*
     * 
     * server
     * 
     */
    public static HttpServer newHttpServer(final HttpRouter httpRouter) {
        return newHttpServer(ssl, port, httpRouter);
    }
    
    public static HttpServer newHttpServer(final HttpRouter httpRouter, 
                                           final WebSocketService webSocketService) {
        return newHttpServer(ssl, port, httpRouter, webSocketService);
    }
    
    public static HttpServer newHttpServer(final boolean ssl,
                                           final int port, 
                                           final HttpRouter httpRouter) {
        return newHttpServer(ssl, port, httpRouter, null);
    }

    public static HttpServer newHttpServer(final boolean ssl,
                                           final int port, 
                                           final HttpRouter httpRouter,
                                           final WebSocketService webSocketService) {
        final HttpServer.Builder httpServerBuilder = HttpServer.builder(httpRouter, port);
        if (ssl) {
            httpServerBuilder.sslSelfSigned();
        }
        if (webSocketService != null) {
            httpServerBuilder.webSocketService(webSocketService);
        }
        final Future<HttpServer> future =
            httpServerBuilder.start()
                             .onFailure(e -> LOGGER.error("httpServer failed", e));
        return awaitValue(future);
    }
    
    /*
     * 
     * web socket client
     * 
     */
    public static WebSocketClient newWebSocketClient(final String path) {
        return newWebSocketClient(ssl, port, host, path);
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
                           .connect()
                           .onFailure(e -> LOGGER.error("webSocket connection failed", e));
        return awaitValue(futureWebSocketClient);
    }
    
}
