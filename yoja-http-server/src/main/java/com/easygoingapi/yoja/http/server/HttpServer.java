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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.Certificatable;
import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.util.TimeUtil;
import com.easygoingapi.yoja.core.worker.Worker;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * The Yoja HTTP server: a thin lifecycle wrapper around Vert.x'
 * {@link io.vertx.core.http.HttpServer} bound to an {@link HttpRouter}.
 * <p>
 * A server is configured through {@link Builder} (port, certificate strategy,
 * optional {@link WebSocketService}, custom {@link HttpServerOptions}) and
 * started with {@link Builder#start()}. The {@link State} field is kept in an
 * {@link AtomicReference} so {@link #state()} and {@link #is(State)} can be
 * polled safely from any thread.
 * <p>
 * The server implements {@link Certificatable}, so its TLS material can be
 * rotated at runtime through {@link #updateCertificate(Path, Path)} without
 * tearing the listener down.
 */
public class HttpServer implements Certificatable {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);

    /** Lifecycle states of the server, ordered by usual transitions. */
    public static enum State {
        /** The server is currently shutting down. */
        stopping,
        /** The server is not listening. */
        stopped,
        /** The server is in the process of binding. */
        starting,
        /** The server is bound and accepting connections. */
        started
    }

    /** Underlying Vert.x HTTP server instance. */
    private io.vertx.core.http.HttpServer httpServer;
    /** Port the server is listening on. */
    private int port;
    /** Active certificate strategy. */
    private HttpCertificate httpCertificate;
    /** Session store published to the world (also held by the router). */
    private HttpSessionStore httpSessionStore;
    /** WebSocket dispatcher; {@code null} when WebSockets are disabled. */
    private WebSocketService webSocketService;
    /** Vert.x options applied when starting the server. */
    private HttpServerOptions httpServerOptions;

    /** TLS key path (when {@link HttpCertificate#SSL} is used). */
    private Path sslKeyPath;
    /** TLS certificate path (when {@link HttpCertificate#SSL} is used). */
    private Path sslCertPath;

    /** Thread-safe current state, defaulted to {@link State#stopped}. */
    private AtomicReference<State> state = new AtomicReference<>(State.stopped);

    /** Private — instances are produced by {@link Builder#start()}. */
    private HttpServer() {
        super();
    }

    /**
     * Returns the TCP port the server is bound to.
     *
     * @return the TCP port the server is bound to
     */
    public int port() {
        return port;
    }

    /**
     * Returns the session store, or {@code null} when sessions are disabled.
     *
     * @return the session store, or {@code null} when sessions are disabled
     */
    public HttpSessionStore httpSessionStore() {
        return httpSessionStore;
    }

    /**
     * Returns the WebSocket dispatcher, or {@code null} when WebSockets are disabled.
     *
     * @return the WebSocket dispatcher, or {@code null} when WebSockets are disabled
     */
    public WebSocketService webSocketService() {
        return webSocketService;
    }

    /**
     * Returns the active certificate strategy.
     *
     * @return the active certificate strategy
     */
    public HttpCertificate certificate() {
        return httpCertificate;
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return the current lifecycle state
     */
    public State state() {
        return this.state.get();
    }

    /**
     * Returns {@code true} when the current lifecycle state equals {@code state}.
     *
     * @param state state to test against
     * @return {@code true} when the current state equals {@code state}
     */
    public boolean is(final State state) {
        return this.state.get() == state;
    }

    /** @return the TLS key path (may be {@code null} when no SSL is configured). */
    @Override
    public Path keyPath() {
        return sslKeyPath;
    }

    /** @return the TLS certificate path (may be {@code null} when no SSL is configured). */
    @Override
    public Path certificatePath() {
        return sslCertPath;
    }

    /**
     * Hot-swaps the server's TLS material without restarting the listener.
     *
     * @param keyPath  path to a PEM-encoded key
     * @param certPath path to a PEM-encoded certificate
     * @return future signalling completion of the SSL update
     */
    @Override
    public Future<Boolean> updateCertificate(final Path keyPath,
                                             final Path certPath) {
        final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        pemKeyCertOptions.setKeyPath(keyPath.toString());
        pemKeyCertOptions.setCertPath(certPath.toString());

        final PemTrustOptions pemTrustOptions = new PemTrustOptions();
        pemTrustOptions.addCertPath(certPath.toString());

        final ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(pemKeyCertOptions);
        sslOptions.setTrustOptions(pemTrustOptions);
        return httpServer.updateSSLOptions(sslOptions);
    }

    /**
     * Asks the Vert.x server to close the listener. The internal state moves
     * to {@link State#stopping} immediately and to {@link State#stopped} once
     * Vert.x reports closure (the latter transition is scheduled on a parallel
     * worker thread).
     *
     * @return a future completing with this server when closure is acknowledged
     */
    public Future<HttpServer> stop() {
        state.set(State.stopping);
        return httpServer.close()
                         .onComplete(v -> Worker.parallelThread.execute(()
                                       -> state.set(State.stopped)))
                         .map(v -> this);
    }

    /**
     * Wires the Vert.x server, configures SSL when requested, registers the
     * router request handler and starts listening on the configured port.
     * <p>
     * Logs a one-shot config summary at {@code INFO} level and transitions the
     * lifecycle state based on whether the listener bound successfully.
     *
     * @return a future completing with this server when bound, or failing with
     *         the bind error
     */
    private Future<HttpServer> start(final HttpRouter httpRouter,
                                     final int port,
                                     final HttpCertificate httpCertificate,
                                     final Path sslKeyPath,
                                     final Path sslCertPath,
                                     final WebSocketService webSocketService,
                                     final HttpServerOptions httpServerOptions) {
        state.set(State.starting);
        this.port = port;
        this.httpCertificate = httpCertificate;
        this.sslKeyPath = sslKeyPath;
        this.sslCertPath = sslCertPath;
        this.httpServerOptions = httpServerOptions;
        if (LOGGER.isInfoEnabled()) {
            final List<String> lines = new ArrayList<>();
            lines.add("# Port: " + port);
            lines.add("# Certificate: " + httpCertificate.name());
            if (HttpCertificate.SSL == httpCertificate) {
                lines.add("   sslKeyPath: " + sslKeyPath);
                lines.add("   sslCertPath: " + sslCertPath);
            }
            if (httpRouter.getContextPath() != null) {
                lines.add("# Context-Path: " + httpRouter.getContextPath());
            }
            final HttpSessionStore httpSessionStore = httpRouter.getHttpSessionStore();
            if (httpSessionStore != null) {
                lines.add("# Http-Session:");
                lines.add("   cookieName: " + httpSessionStore.cookieName());
                lines.add("   duration: " + TimeUtil.prettyPrint(httpSessionStore.timeout()));
            }
            if (!httpRouter.getContentTypes().isEmpty()) {
                lines.add("# Content-Type:");
                for (final Entry<String, String> entry : httpRouter.getContentTypes().entrySet()) {
                    lines.add("   " + entry.getKey() + " -> " + entry.getValue());
                }
            }
            if (webSocketService != null) {
                lines.add("# Web-Socket:");
                for (final String path : webSocketService.getWebSocketPaths()) {
                    lines.add("   " + path);
                }
            }
            if (!httpRouter.getWebServices().isEmpty()) {
                lines.add("# Web-Service:");
                for (final WebService webService : httpRouter.getWebServices()) {
                    lines.add("   " + webService.getPath() + " [" + webService.getMethod() + "] -> " + webService.getHandlers().size());
                }
            }
            LOGGER.info("[http] [server] [config] \n{}", String.join(System.lineSeparator(), lines));
        }

        // CERTIFICATE
        if (HttpCertificate.SSL == httpCertificate) {
            final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
            pemKeyCertOptions.setKeyPath(sslKeyPath.toString());
            pemKeyCertOptions.setCertPath(sslCertPath.toString());
            this.httpServerOptions.setKeyCertOptions(pemKeyCertOptions);
            final PemTrustOptions pemTrustOptions = new PemTrustOptions();
            pemTrustOptions.addCertPath(sslCertPath.toString());
            this.httpServerOptions.setTrustOptions(pemTrustOptions);
        }
        // SERVER
        httpServer = YojaApp.vertx().createHttpServer(httpServerOptions);
        httpServer.requestHandler(httpRouter.getRouter());
        httpServer.webSocketHandshakeHandler(webSocketHandshakeHandler -> webSocketService.shakeHand(webSocketHandshakeHandler,
                                                                                                     httpRouter.getContextPath()));
        httpServer.webSocketHandler(serverWebSocket -> webSocketService.open(serverWebSocket,
                                                                             httpRouter.getContextPath()));
        return httpServer.listen(port)
                         .onFailure(e -> {
                             state.set(State.stopped);
                             LOGGER.error("[http] [server] [failed] port={}, contextPath={}, certificate={}",
                                          port, httpRouter.getContextPath(), httpCertificate);
                         })
                         .onSuccess(h -> {
                             state.set(State.started);
                             LOGGER.info("[http] [server] [started] port={}, contextPath={}, certificate={}",
                                         port, httpRouter.getContextPath(), httpCertificate);
                         })
                         .map(this);
    }

    /**
     * Dumps every {@code get*}-style option of {@link WebClientOptions} (used
     * as a proxy for {@link HttpServerOptions}) into the log for diagnostics.
     */
    public void logOptions() {
        final Set<String> options = new TreeSet<>();
        for (final Method method : WebClientOptions.class.getMethods()) {
            if (method.getName().startsWith("get")) {
                final String methodName = method.getName();
                if (!"getClass".equals(methodName)) {
                    try {
                        options.add(methodName.substring(3) + ": " + method.invoke(httpServerOptions));
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

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpServer.class.getSimpleName());
        result.append(" [port=");
        result.append(port);
        result.append(", httpCertificate=");
        result.append(httpCertificate);
        result.append(", state=");
        result.append(state);
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder for a server serving the given router on the given port.
     *
     * @param httpRouter the router to serve
     * @param port       TCP port to listen on
     * @return a new builder
     */
    public static Builder builder(final HttpRouter httpRouter,
                                  final int port) {
        return new Builder(httpRouter, port);
    }

    /**
     * Fluent builder for {@link HttpServer}.
     * <p>
     * Defaults to plain HTTP with {@link HttpCertificate#NONE}; switch to TLS
     * with {@link #ssl(Path, Path)} or {@link #sslSelfSigned()}. WebSocket
     * support is enabled by attaching a {@link WebSocketService} via
     * {@link #webSocketService(WebSocketService)}.
     */
    public static class Builder {

        /** Router whose routes will be exposed by the server. */
        private final HttpRouter httpRouter;
        /** TCP port to bind. */
        private final int port;

        /** TLS strategy; defaults to {@link HttpCertificate#NONE}. */
        private HttpCertificate certificate = HttpCertificate.NONE;
        /** Optional Vert.x server options; built from defaults when {@code null}. */
        private HttpServerOptions httpServerOptions;
        /** TLS key path (set by {@link #ssl(Path, Path)}). */
        private Path sslKeyPath;
        /** TLS certificate path (set by {@link #ssl(Path, Path)}). */
        private Path sslCertPath;

        /** Optional WebSocket dispatcher. */
        private WebSocketService webSocketService;

        /** Private — use {@link HttpServer#builder(HttpRouter, int)}. */
        private Builder(final HttpRouter httpRouter,
                        final int port) {
            super();
            this.httpRouter = httpRouter;
            this.port = port;
        }

        /**
         * Enables TLS with PEM-encoded key and certificate files.
         *
         * @param sslKeyPath  path to the PEM-encoded key
         * @param sslCertPath path to the PEM-encoded certificate
         * @return this builder
         */
        public Builder ssl(final Path sslKeyPath,
                           final Path sslCertPath) {
            this.certificate = HttpCertificate.SSL;
            this.sslKeyPath = sslKeyPath;
            this.sslCertPath = sslCertPath;
            return this;
        }

        /**
         * Enables TLS using a self-signed certificate generated at start time.
         *
         * @return this builder
         */
        public Builder sslSelfSigned() {
            this.certificate = HttpCertificate.SELF_SIGNED;
            return this;
        }

        /**
         * Attaches a WebSocket dispatcher; the server will accept upgrade requests.
         *
         * @param webSocketService dispatcher to install
         * @return this builder
         */
        public Builder webSocketService(final WebSocketService webSocketService) {
            this.webSocketService = webSocketService;
            return this;
        }

        /**
         * Overrides the default Vert.x {@link HttpServerOptions}.
         *
         * @param httpServerOptions Vert.x options to use
         * @return this builder
         */
        public Builder options(final HttpServerOptions httpServerOptions) {
            this.httpServerOptions = httpServerOptions;
            return this;
        }

        /**
         * Builds the server and starts the listener.
         *
         * @return a future completing with the started server, or failing with
         *         the bind error
         */
        public Future<HttpServer> start() {
            final HttpServer httpServer = new HttpServer();
            httpServer.httpSessionStore = httpRouter.getHttpSessionStore();
            httpServer.webSocketService = webSocketService;
            if (this.httpServerOptions == null) {
                this.httpServerOptions = defaultOptions(certificate);
            }
            return httpServer.start(httpRouter, port,
                                    certificate,
                                    sslKeyPath, sslCertPath,
                                    webSocketService,
                                    this.httpServerOptions);
        }

    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Builds default {@link HttpServerOptions} matching the given certificate
     * strategy: HTTP/2 with ALPN fallback to HTTP/1.1 when TLS is enabled, and
     * a self-signed certificate when {@link HttpCertificate#SELF_SIGNED} is
     * selected.
     *
     * @param httpCertificate certificate strategy
     * @return ready-to-use Vert.x options
     */
    public static HttpServerOptions defaultOptions(final HttpCertificate httpCertificate) {
        final HttpServerOptions httpServerOptions = new HttpServerOptions();
        if (HttpCertificate.SSL == httpCertificate) {
            httpServerOptions.setSsl(true);
            httpServerOptions.setUseAlpn(true);
            httpServerOptions.setAlpnVersions(List.of(HttpVersion.HTTP_2,
                                                      HttpVersion.HTTP_1_1));
        }
        else if (HttpCertificate.SELF_SIGNED == httpCertificate) {
            httpServerOptions.setSsl(true);
            httpServerOptions.setUseAlpn(true);
            httpServerOptions.setAlpnVersions(List.of(HttpVersion.HTTP_2,
                                                      HttpVersion.HTTP_1_1));
            final SelfSignedCertificate selfSignedCertificate = SelfSignedCertificate.create();
            httpServerOptions.setKeyCertOptions(selfSignedCertificate.keyCertOptions());
            httpServerOptions.setTrustOptions(selfSignedCertificate.trustOptions());
        }
        return httpServerOptions;
    }

}
