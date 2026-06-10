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
package com.easygoingapi.yoja.selenium;

import static com.easygoingapi.yoja.core.http.HttpMethod.GET;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.easygoingapi.yoja.core.http.HttpParameter;
import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.HttpSessionStore;
import com.easygoingapi.yoja.http.server.WebApp;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.http.server.WebSocketService;
import com.easygoingapi.yoja.http.server.HttpServer.State;

/**
 * Embedded Yoja {@link HttpServer} dedicated to serving the test fixtures
 * (HTML pages, JS modules, resources, WebSocket endpoints) that a Selenium
 * test scenario navigates to.
 * <p>
 * Each instance auto-assigns a port from a shared, monotonically increasing
 * sequence (starting at 8888) so multiple contexts can run in parallel
 * without colliding. {@link #start()} synchronously waits until the
 * underlying Vert.x server has bound; {@link #stop()} blocks until it has
 * closed. Both helpers throw a {@link SeleniumException} on failure.
 * <p>
 * A nested {@link HttpUrlBuilder} produces {@link HttpUrl} values pointing at
 * the running server, ready to be passed to
 * {@link SeleniumService#getHttpPage(HttpUrl)}.
 */
public class HttpServerContext implements AutoCloseable {

    /** Shared port allocator across every instance. */
    private static final AtomicInteger PORT_SEQUENCE = new AtomicInteger(8888);

    /** Host the embedded server binds to. */
    private final String host;
    /** Port allocated for this context (assigned at construction time). */
    private final int port;

    /** {@link WebApp}s mounted as JS-unit fixtures (each gets served under {@code /*}). */
    private final List<WebApp> jsUnitWebApps;
    /** Additional services and resources registered by the test scenario. */
    private final List<WebService> webServices;
    /** Optional WebSocket endpoints. */
    private final WebSocketService webSocketService;
    /** Extension → Content-Type table for the static resources. */
    private final Map<String, String> contentTypes;

    /** Underlying Yoja HTTP server; non-null between {@link #start()} and {@link #stop()}. */
    private HttpServer httpServer;

    /**
     * Constructs a context that will bind an embedded Yoja HTTP server on the given host.
     *
     * @param host             host the embedded server binds to
     * @param jsUnitWebApps    JS-unit web apps to mount under {@code /*}
     * @param webServices      additional services/resources to register
     * @param webSocketService WebSocket endpoints; may be empty
     * @param contentTypes     extension → Content-Type mapping
     */
    protected HttpServerContext(final String host,
                                final List<WebApp> jsUnitWebApps,
                                final List<WebService> webServices,
                                final WebSocketService webSocketService,
                                final Map<String, String> contentTypes) {
        super();
        this.host = host;
        this.port = PORT_SEQUENCE.getAndIncrement();
        this.jsUnitWebApps = jsUnitWebApps;
        this.webServices = webServices;
        this.webSocketService = webSocketService;
        this.contentTypes = contentTypes;
    }

    /**
     * Returns the host the embedded server binds to.
     *
     * @return the host the embedded server binds to
     */
    public String host() {
        return host;
    }

    /**
     * Returns the auto-assigned port.
     *
     * @return the auto-assigned port
     */
    public int port() {
        return port;
    }

    /**
     * Returns a builder producing URLs that target this context.
     *
     * @return a builder producing URLs that target this context
     */
    public HttpUrlBuilder httpUrlBuilder() {
        return new HttpUrlBuilder(host(), port());
    }

    /**
     * Returns the underlying server's lifecycle state.
     *
     * @return the underlying server's lifecycle state, or {@link State#stopped}
     *         when the server has not been created yet
     */
    public State httpServerState() {
        if (httpServer == null) {
            return State.stopped;
        }
        return httpServer.state();
    }

    /**
     * Builds the router, attaches a long-lived ({@code 1 year}) session store,
     * starts the Vert.x server and blocks until the listener is bound (polling
     * every 100&nbsp;ms). No-op when the server is already started.
     *
     * @throws SeleniumException when the bind fails
     */
    protected void start() {
        if (State.stopped == httpServerState()) {
            try {
                // httpRouter
                final HttpRouter.Builder httpRouterBuilder =
                    HttpRouter.builder()
                              .contentTypes(contentTypes)
                              .webService(GET, "/favicon.ico", h -> h.response().send());
                // jsUnit
                for (final WebApp jsUnitWebApps: jsUnitWebApps) {
                    httpRouterBuilder.webResource(jsUnitWebApps, "/*");
                }
                // webService
                for (final WebService webService : webServices) {
                    httpRouterBuilder.webService(webService);
                }
                // http session
                final HttpSessionStore httpSessionStore = new HttpSessionStore("session.yoja",
                                                                               Duration.ofDays(365));
                httpRouterBuilder.session(httpSessionStore);
                // websocket

                // selenium
                final AtomicBoolean await = new AtomicBoolean(true);
                HttpServer.builder(httpRouterBuilder.build(), port())
                          .webSocketService(webSocketService)
                          .start()
                          .onFailure(e -> {
                              throw new SeleniumException(e);
                          })
                          .onComplete(h -> {
                              this.httpServer = h.result();
                              await.set(false);
                          });
                do Thread.sleep(100);
                while (await.get());
            }
            catch (final Exception e) {
                throw new SeleniumException("start package http server failed", e);
            }
        }
    }

    /**
     * Closes the underlying server and blocks until it reports stopped.
     *
     * @throws SeleniumException when the close fails
     */
    protected void stop() {
        try {
            if (httpServer != null) {
                final AtomicBoolean await = new AtomicBoolean(true);
                httpServer.stop()
                          .onComplete(h -> await.set(false));
                do Thread.sleep(100);
                while (await.get());
            }
        }
        catch (final Exception e) {
            throw new SeleniumException("stop package http server failed", e);
        }
    }

    /** Equivalent to {@link #stop()}; lets the context participate in try-with-resources. */
    @Override
    public void close() {
        stop();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpServerContext.class.getSimpleName());
        result.append(" [httpServer=");
        result.append(httpServer.state());
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Builder producing {@link HttpUrl} instances that target a running
     * {@link HttpServerContext}.
     * <p>
     * The host and port come from the context; the caller supplies the path,
     * query and fragment. The protocol is always {@link HttpProtocole#http}.
     */
    public static class HttpUrlBuilder {

        /** Host carried over from the context. */
        private final String host;
        /** Port carried over from the context. */
        private final int port;

        /** Path to apply to the URL. */
        private String path;
        /** Query parameters to apply (parsed or pre-built). */
        private HttpParameter httpParameter;
        /** Fragment (anchor) to apply. */
        private String fragment;

        /**
         * @param host host the URL will point at
         * @param port port the URL will point at
         */
        private HttpUrlBuilder(final String host,
                               final int port) {
            super();
            this.host = host;
            this.port = port;
        }

        /**
         * Sets the path from a {@link Path}; {@code null} clears the path.
         *
         * @param path filesystem-style path to use
         * @return this builder
         */
        public HttpUrlBuilder path(final Path path) {
            this.path = path != null ? path.toString() : null;
            return this;
        }

        /**
         * Sets the path verbatim.
         *
         * @param path URL path
         * @return this builder
         */
        public HttpUrlBuilder path(final String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the query from a parsed {@link HttpParameter}.
         *
         * @param httpParameter parsed query
         * @return this builder
         */
        public HttpUrlBuilder query(final HttpParameter httpParameter) {
            this.httpParameter = httpParameter;
            return this;
        }

        /**
         * Sets the query from a raw query string (parsed via
         * {@link HttpParameter#parse(String)}).
         *
         * @param query raw query string
         * @return this builder
         */
        public HttpUrlBuilder query(final String query) {
            this.httpParameter = HttpParameter.parse(query);
            return this;
        }

        /**
         * Sets the URL fragment (anchor).
         *
         * @param fragment fragment value
         * @return this builder
         */
        public HttpUrlBuilder fragment(final String fragment) {
            this.fragment = fragment;
            return this;
        }

        /**
         * Returns an {@link HttpUrl} assembled with the current settings.
         *
         * @return an {@link HttpUrl} assembled with the current settings
         */
        public HttpUrl build() {
            return HttpUrl.builder(host)
                          .protocol(HttpProtocole.http)
                          .port(port)
                          .path(path)
                          .parameter(httpParameter)
                          .fragment(fragment)
                          .build();
        }

    }

}
