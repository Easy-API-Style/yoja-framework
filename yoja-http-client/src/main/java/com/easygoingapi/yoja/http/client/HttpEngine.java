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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Shared HTTP engine owning a single Vert.x {@link WebClient} that
 * {@link HttpClient} instances dial through.
 * <p>
 * One engine is typically shared by every {@link HttpClient} of a given
 * configuration: it owns the connection pool, the default host/port pulled
 * from {@link WebClientOptions} and the SSL / ALPN settings. The default
 * options ({@link #defaultOptions()}) negotiate HTTP/2 with HTTP/1.1
 * fallback, trust every certificate (suitable for self-signed dev setups,
 * less so for production) and verify the host name.
 * <p>
 * Implements {@link AutoCloseable}: closing the engine closes the wrapped
 * {@link WebClient} and tears down its connection pool.
 */
public class HttpEngine implements AutoCloseable {

    private static Logger LOGGER = LoggerFactory.getLogger(HttpEngine.class);

    /** Wrapped Vert.x web client. */
    private final WebClient webClient;
    /** Options used to build the wrapped client (kept for diagnostics). */
    private final WebClientOptions webClientOptions;

    /** Builds an engine with the default options of {@link #defaultOptions()}. */
    public HttpEngine() {
        this(defaultOptions());
    }

    /**
     * Builds an engine with custom Vert.x options.
     *
     * @param webClientOptions Vert.x options to apply to the wrapped client
     */
    public HttpEngine(final WebClientOptions webClientOptions) {
        super();
        this.webClientOptions = webClientOptions;
        webClient = WebClient.create(YojaApp.vertx(), webClientOptions);
    }

    /**
     * Returns the Vert.x options used by the engine.
     *
     * @return the Vert.x options used by the engine
     */
    public WebClientOptions options() {
        return webClientOptions;
    }

    /**
     * Returns the default host configured on the engine.
     *
     * @return the default host configured on the engine
     */
    public String defaultHost() {
        return webClientOptions.getDefaultHost();
    }

    /**
     * Returns the default port configured on the engine.
     *
     * @return the default port configured on the engine
     */
    public int defaultPort() {
        return webClientOptions.getDefaultPort();
    }

    /**
     * Returns the wrapped Vert.x web client.
     *
     * @return the wrapped Vert.x web client
     */
    public WebClient webClient() {
        return webClient;
    }

    /**
     * Reflectively dumps every {@code get*}-style option of
     * {@link WebClientOptions} into the SLF4J log at {@code INFO}. Useful for
     * one-shot debugging of an engine's effective configuration.
     */
    public void logOptions() {
        final Set<String> options = new TreeSet<>();
        for (final Method method : WebClientOptions.class.getMethods()) {
            if (method.getName().startsWith("get")) {
                final String methodName = method.getName();
                if (!"getClass".equals(methodName)) {
                    try {
                        options.add(methodName.substring(3) + ": " + method.invoke(webClientOptions));
                    }
                    catch (final Exception e) {
                        LOGGER.error("invoke method of WebClientOptions failed: {}", methodName, e);
                    }
                }
            }
        }
        LOGGER.info("WebClientOptions vertx configuration: \n{}",
                    String.join(System.lineSeparator(), options));
    }

    /** Closes the wrapped Vert.x web client; the engine must not be used after this call. */
    @Override
    public void close() {
        webClient.close();
    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Returns the default Vert.x options suitable for dev setups.
     *
     * @return the default Vert.x options: HTTP/2 with HTTP/1.1 ALPN fallback,
     *         TLS on, host verification on, trust-all on (suitable for dev
     *         setups using self-signed certificates)
     */
    public static WebClientOptions defaultOptions() {
        return new WebClientOptions()
                      .setSsl(true)
                      .setUseAlpn(true)
                      .setVerifyHost(true)
                      .setTrustAll(true)
                      .setAlpnVersions(List.of(HttpVersion.HTTP_2,
                                               HttpVersion.HTTP_1_1))
                      .setProtocolVersion(HttpVersion.HTTP_2);
    }

}
