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

import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;

import io.vertx.core.Future;
import io.vertx.core.http.WebSocketClientOptions;

/**
 * WebSocket counterpart of {@link HttpEngine}: owns a single Vert.x
 * {@link io.vertx.core.http.WebSocketClient} that {@link WebSocketClient}
 * instances dial through.
 * <p>
 * The default options ({@link #defaultOptions()}) enable TLS, ALPN, host
 * verification and trust-all (suitable for dev setups). The wrapped client
 * is closed asynchronously via {@link #close()}.
 */
public class WebSocketEngine {

    private static Logger LOGGER = LoggerFactory.getLogger(WebSocketEngine.class);

    /** Wrapped Vert.x WebSocket client. */
    private final io.vertx.core.http.WebSocketClient webSocketClient;
    /** Options used to build the wrapped client (kept for diagnostics). */
    private final WebSocketClientOptions webSocketClientOptions;

    /** Builds an engine with the default options of {@link #defaultOptions()}. */
    public WebSocketEngine() {
        this(defaultOptions());
    }

    /**
     * Builds an engine with custom Vert.x options.
     *
     * @param webSocketClientOptions Vert.x options to apply to the wrapped client
     */
    public WebSocketEngine(final WebSocketClientOptions webSocketClientOptions) {
        this.webSocketClientOptions = webSocketClientOptions;
        this.webSocketClient = YojaApp.vertx().createWebSocketClient(webSocketClientOptions);
    }

    /**
     * Returns the Vert.x options used by the engine.
     *
     * @return the Vert.x options used by the engine
     */
    public WebSocketClientOptions options() {
        return webSocketClientOptions;
    }

    /**
     * Returns the default host configured on the engine.
     *
     * @return the default host configured on the engine
     */
    public String defaultHost() {
        return webSocketClientOptions.getDefaultHost();
    }

    /**
     * Returns the default port configured on the engine.
     *
     * @return the default port configured on the engine
     */
    public int defaultPort() {
        return webSocketClientOptions.getDefaultPort();
    }

    /**
     * Returns the wrapped Vert.x WebSocket client.
     *
     * @return the wrapped Vert.x WebSocket client
     */
    public io.vertx.core.http.WebSocketClient webSocketClient() {
        return webSocketClient;
    }

    /**
     * Reflectively dumps every {@code get*}-style option of
     * {@link WebSocketClientOptions} into the SLF4J log at {@code INFO}.
     * Useful for one-shot debugging of an engine's effective configuration.
     */
    public void logOptions() {
        final Set<String> options = new TreeSet<>();
        for (final Method method : WebSocketClientOptions.class.getMethods()) {
            if (method.getName().startsWith("get")) {
                final String methodName = method.getName();
                if (!"getClass".equals(methodName)) {
                    try {
                        options.add(methodName.substring(3) + ": " + method.invoke(webSocketClientOptions));
                    }
                    catch (final Exception e) {
                        LOGGER.error("invoke method of WebClientOptions failed: {}", methodName, e);
                    }
                }
            }
        }
        LOGGER.info("WebSocketClientOptions vertx configuration: \n{}",
                    String.join(System.lineSeparator(), options));
    }

    /**
     * Asynchronously closes the wrapped Vert.x WebSocket client.
     *
     * @return a future completing when the client has closed
     */
    public Future<Void> close() {
       return webSocketClient.close();
    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Returns the default Vert.x options suitable for dev setups.
     *
     * @return the default Vert.x options: TLS on, ALPN on, host verification
     *         on, trust-all on (suitable for dev setups using self-signed
     *         certificates)
     */
    public static WebSocketClientOptions defaultOptions() {
        return new WebSocketClientOptions()
                     .setSsl(true)
                     .setVerifyHost(true)
                     .setTrustAll(true)
                     .setUseAlpn(true);
    }

}
