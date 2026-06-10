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
package com.easygoingapi.yoja.reverse.proxy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.Certificatable;
import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.http.client.HttpEngine;
import com.easygoingapi.yoja.http.client.WebSocketEngine;
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Lifecycle entry point of the reverse-proxy module.
 * <p>
 * A {@code ReverseProxyServer} owns two Vert.x {@link HttpServer} instances
 * sharing the same TLS material:
 * <ul>
 *   <li>the <strong>proxy server</strong> listens on the public port and forwards every
 *       request — HTTP and WebSocket — through a {@link ReverseProxySwitcherWebClient};</li>
 *   <li>the <strong>admin server</strong> (optional) listens on a separate port and exposes
 *       the switcher's rule-management endpoints (see
 *       {@link ReverseProxySwitcher#adminRouter()}).</li>
 * </ul>
 * Both servers are started/stopped independently through {@link #start()} /
 * {@link #stop()} and {@link #startAdmin()} / {@link #stopAdmin()}. Their
 * states are tracked in {@link AtomicReference}s so they can be polled from
 * any thread.
 * <p>
 * The class implements {@link Certificatable}, so its TLS material can be
 * rotated at runtime through {@link #updateCertificate(Path, Path)} on both
 * servers atomically.
 * <p>
 * Two static {@code adminTokenHeader}/{@code adminNewTokenHeader} field names
 * are exposed via static accessors so that clients can attach the right
 * headers when calling the admin API; their values can be customised by
 * setting the static fields before constructing the server (not exposed by
 * the public API).
 */
public class ReverseProxyServer implements Certificatable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReverseProxyServer.class);

    /**
     * Bundle of every server-level setting computed by the builder.
     *
     * @param port        port of the proxy server
     * @param adminPort   port of the admin server, or {@code null} when admin is disabled
     * @param options     Vert.x {@link HttpServerOptions} shared by both servers
     * @param certificate TLS strategy
     * @param sslKeyPath  PEM key path when {@code certificate} is {@link HttpCertificate#SSL}
     * @param sslCertPath PEM certificate path when {@code certificate} is {@link HttpCertificate#SSL}
     */
    private final record ServerConfig(int port,
                                      Integer adminPort,
                                      HttpServerOptions options,
                                      HttpCertificate certificate,
                                      Path sslKeyPath,
                                      Path sslCertPath) {}

    /** Header name carrying the current admin token. */
    private static String adminTokenHeader = "Reserve-Proxy-Token";
    /** Header name carrying the replacement token on {@code POST /update/token}. */
    private static String adminNewTokenHeader = "New-Reserve-Proxy-Token";

    /** Captured server configuration, populated at start time. */
    private ServerConfig proxyServerConfig;

    /** Vert.x server backing the admin endpoints. */
    private HttpServer adminProxyServer;
    /** Lifecycle state of the admin server. */
    private AtomicReference<State> adminProxyServerState = new AtomicReference<>(State.stopped);

//    private HttpClient proxyClient;
    /** Vert.x server backing the proxy endpoints. */
    private HttpServer proxyServer;
    /** Lifecycle state of the proxy server. */
    private AtomicReference<State> proxyServerState = new AtomicReference<>(State.stopped);

    /** The switcher that resolves and dispatches every inbound request. */
    private ReverseProxySwitcherWebClient proxySwitcher;

    /** Private — instances are produced through {@link #builder(int)}. */
    private ReverseProxyServer() {
        super();
    }

    /**
     * Returns the port the proxy server listens on.
     *
     * @return the port the proxy server listens on
     */
    public int proxyPort() {
        return proxyServerConfig.port();
    }

    /**
     * Returns the port the admin server listens on, or {@code null} when admin is disabled.
     *
     * @return the port the admin server listens on, or {@code null} when admin is disabled
     */
    public Integer adminPort() {
        return proxyServerConfig.adminPort();
    }

    /**
     * Returns the TLS strategy active on both servers.
     *
     * @return the TLS strategy active on both servers
     */
    public HttpCertificate certificate() {
        return proxyServerConfig.certificate();
    }

    /**
     * Returns the current lifecycle state of the admin server.
     *
     * @return the current lifecycle state of the admin server
     */
    public State adminState() {
        return adminProxyServerState.get();
    }

    /**
     * Returns the current lifecycle state of the proxy server.
     *
     * @return the current lifecycle state of the proxy server
     */
    public State proxyState() {
        return proxyServerState.get();
    }

    /** @return the TLS key path (may be {@code null} when no SSL is configured). */
    @Override
    public Path keyPath() {
        return proxyServerConfig.sslKeyPath();
    }

    /** @return the TLS certificate path (may be {@code null} when no SSL is configured). */
    @Override
    public Path certificatePath() {
        return proxyServerConfig.sslCertPath();
    }

    /**
     * Hot-swaps the TLS material on both the proxy and admin servers without
     * restarting either listener.
     *
     * @param sslKeyPath  path to the PEM-encoded key
     * @param sslCertPath path to the PEM-encoded certificate
     * @return a future resolving to {@code true} when both updates succeeded
     */
    @Override
    public Future<Boolean> updateCertificate(final Path sslKeyPath,
                                             final Path sslCertPath) {
        final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        pemKeyCertOptions.setKeyPath(sslKeyPath.toString());
        pemKeyCertOptions.setCertPath(sslCertPath.toString());

        final PemTrustOptions pemTrustOptions = new PemTrustOptions();
        pemTrustOptions.addCertPath(sslCertPath.toString());

        final ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(pemKeyCertOptions);
        sslOptions.setTrustOptions(pemTrustOptions);
        final List<Future<?>> futures = new ArrayList<>();
        futures.add(proxyServer.updateSSLOptions(sslOptions));
        futures.add(adminProxyServer.updateSSLOptions(sslOptions));
        return Future.all(futures)
                     .map(h -> h.succeeded(0) && h.succeeded(1));
    }

    /**
     * Binds the admin server on its configured port. No-op when the admin
     * server is already started; throws when no admin port was configured.
     *
     * @return a future completing once the listener is bound (or already was)
     * @throws ReverseProxyException when admin support was not enabled in the builder
     */
    public Future<Void> startAdmin() {
        if (proxyServerConfig.adminPort() != null) {
            if (adminState() == State.stopped) {
                adminProxyServerState.set(State.starting);
                return this.adminProxyServer
                           .listen(proxyServerConfig.adminPort())
                           .onFailure(e -> {
                                adminProxyServerState.set(State.stopped);
                                LOGGER.error("[http] [reverseProxy] [admin] [starting] [failed] port={}, certificate={}",
                                             proxyServerConfig.adminPort(),
                                             proxyServerConfig.certificate());
                           })
                           .onSuccess(h -> {
                                 adminProxyServerState.set(State.started);
                                 LOGGER.info("[http] [reverseProxy] [admin] [started] port={}, certificate={}",
                                             proxyServerConfig.adminPort(),
                                             proxyServerConfig.certificate());
                           })
                           .mapEmpty();
            }
            else {
                return Future.succeededFuture();
            }
        }
        else {
            throw new ReverseProxyException("admin port not defined");
        }
    }

    /**
     * Closes the admin server's listener.
     *
     * @return a future completing once the listener is closed
     * @throws ReverseProxyException when admin support was not enabled in the builder
     */
    public Future<Void> stopAdmin() {
        if (proxyServerConfig.adminPort() != null) {
            adminProxyServerState.set(State.stopping);
            return adminProxyServer.close()
                                   .onFailure(e -> {
                                       LOGGER.error("[http] [reverseProxy] [admin] [stopping] [failed] port={}, certificate={}",
                                                    proxyServerConfig.adminPort(),
                                                    proxyServerConfig.certificate());
                                   })
                                   .onSuccess(h -> {
                                       adminProxyServerState.set(State.stopped);
                                       LOGGER.info("[http] [reverseProxy] [admin] [stopped] port={}, certificate={}",
                                                   proxyServerConfig.port(),
                                                   proxyServerConfig.certificate());
                                   })
                                   .mapEmpty();
        }
        else {
            throw new ReverseProxyException("admin port no defined");
        }
    }

    /**
     * Binds the proxy server on its configured port.
     *
     * @return a future completing once the listener is bound
     */
    public Future<Void> start() {
        proxyServerState.set(State.starting);
        return this.proxyServer
                   .listen(proxyServerConfig.port())
                   .onFailure(e -> {
                       proxyServerState.set(State.stopped);
                       LOGGER.error("[http] [reverseProxy] [failed] port={}, certificate={}",
                                    proxyServerConfig.port(),
                                    proxyServerConfig.certificate());
                   })
                   .onSuccess(h -> {
                       proxyServerState.set(State.started);
                       LOGGER.info("[http] [reverseProxy] [started] port={}, certificate={}",
                                  proxyServerConfig.port(),
                                  proxyServerConfig.certificate());
                   })
                   .mapEmpty();
    }

    /**
     * Stops the proxy server, the switcher's underlying engines and (when it
     * was running) the admin server. All listeners are closed in parallel.
     *
     * @return a future completing once every component has stopped
     */
    public Future<Void> stop() {
        proxyServerState.set(State.stopping);
        final List<Future<?>> futures = new ArrayList<>();
        futures.add(proxySwitcher.close());
        futures.add(proxyServer.close());
        if (adminProxyServerState.get() != State.stopped) {
            adminProxyServerState.set(State.stopping);
            futures.add(adminProxyServer.close());
        }
        return Future.all(futures)
                     .onFailure(e -> {
                         LOGGER.error("[http] [reverseProxy] [stopping] [failed] port={}, certificate={}",
                                      proxyServerConfig.port(),
                                      proxyServerConfig.certificate());
                     })
                     .onSuccess(h -> {
                         proxyServerState.set(State.stopped);
                         adminProxyServerState.set(State.stopped);
                         LOGGER.info("[http] [reverseProxy] [stopped] port={}, certificate={}",
                                     proxyServerConfig.port(),
                                     proxyServerConfig.certificate());
                     })
                     .mapEmpty();
    }

    /**
     * Wires the Vert.x servers, attaches the switcher, then starts the proxy
     * listener. Called from {@link Builder#start()}.
     *
     * @param proxyServerConfig    server configuration bundle
     * @param adminToken           initial admin bearer token
     * @param silent               whether unresolved requests are silently dropped
     * @param proxyHttpEngine      HTTP engine used to dial upstream HTTP services
     * @param proxyWebSocketEngine WebSocket engine used to dial upstream WS services
     * @param reverseProxyRules    initial rule set
     * @param resolver             optional fallback resolver
     * @param onResolveActions     resolution listeners
     * @return a future resolving to this server once the proxy listener is bound
     */
    private Future<ReverseProxyServer> start(final ServerConfig proxyServerConfig,
                                             final String adminToken,
                                             final boolean silent,
                                             final HttpEngine proxyHttpEngine,
                                             final WebSocketEngine proxyWebSocketEngine,
                                             final Set<ReverseProxyRule> reverseProxyRules,
                                             final Function<HttpUrl, HttpUrl> resolver,
                                             final List<Handler<ReverseProxyResult>> onResolveActions) {
        this.proxyServerConfig = proxyServerConfig;
        this.setHttpServerOptions(proxyServerConfig);
        this.proxySwitcher = new ReverseProxySwitcherWebClient(adminToken, silent, proxyServerConfig.port(),
        		                                               proxyHttpEngine, proxyWebSocketEngine,
        		                                               reverseProxyRules, resolver,
        		                                               onResolveActions);
        // admin
        this.adminProxyServer = YojaApp.vertx().createHttpServer(proxyServerConfig.options());
        this.adminProxyServer.requestHandler(proxySwitcher.adminRouter());
        // proxy
        this.proxyServer = YojaApp.vertx().createHttpServer(proxyServerConfig.options());
//        final ProxyOptions proxyOptions = new ProxyOptions();
//        this.proxyClient = YojaApp.vertx().createHttpClient();
//        final HttpProxy httpProxy = HttpProxy.reverseProxy(proxyOptions, this.proxyClient);
//        httpProxy.originRequestProvider((request, client) ->  proxySwitcher.resolveOrigin(request, client));
//        httpProxy.addInterceptor(proxySwitcher.proxyInterceptor());
        this.proxyServer.webSocketHandshakeHandler(ServerWebSocketHandshake -> proxySwitcher.websocketHandshake(ServerWebSocketHandshake));
        this.proxyServer.webSocketHandler(serverWebSocket -> proxySwitcher.websocket(serverWebSocket));
        this.proxyServer.requestHandler(proxySwitcher.proxyRouter());
        return this.start().map(this);
    }

    /**
     * Applies the SSL key/cert paths from {@code serverConfig} onto its
     * Vert.x options when the certificate strategy is {@link HttpCertificate#SSL}.
     *
     * @param serverConfig configuration whose options will be mutated in place
     */
    private void setHttpServerOptions(final ServerConfig serverConfig) {
        if (HttpCertificate.SSL == serverConfig.certificate()) {
            final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
            pemKeyCertOptions.setKeyPath(serverConfig.sslKeyPath().toString());
            pemKeyCertOptions.setCertPath(serverConfig.sslCertPath().toString());
            serverConfig.options()
                        .setKeyCertOptions(pemKeyCertOptions);

            final PemTrustOptions pemTrustOptions = new PemTrustOptions();
            pemTrustOptions.addCertPath(serverConfig.sslCertPath().toString());
            serverConfig.options()
                        .setTrustOptions(pemTrustOptions);
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(ReverseProxyServer.class.getSimpleName());
        result.append(" [proxyPort=");
        result.append(proxyServerConfig.port());
        result.append(", certificate=");
        result.append(proxyServerConfig.certificate());
        result.append(", proxyState=");
        result.append(proxyServerState.get());
        if (adminProxyServer != null) {
            result.append(", adminPort=");
            result.append(proxyServerConfig.adminPort());
        }
        result.append(", adminState=");
        result.append(adminProxyServerState.get());
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder for a proxy server listening on the given port.
     *
     * @param proxyPort port the proxy server will listen on
     * @return a new builder
     */
    public static Builder builder(final int proxyPort) {
        return new Builder(proxyPort);
    }

    /**
     * Fluent builder for {@link ReverseProxyServer}.
     * <p>
     * Collects rules, resolution listeners, TLS strategy, optional admin
     * configuration and engine overrides, then starts the server via
     * {@link #start()}. Sensible defaults are applied for HTTP engines and
     * Vert.x server options when not overridden.
     */
    public static class Builder {

        /** Port the proxy server will listen on. */
        private final int proxyPort;
        /** Rule accumulator keyed by {@code from.id()} to detect duplicates. */
        private final Map<String, ReverseProxyRule> reverseProxyRules = new HashMap<>();
        /** Resolution listeners. */
        private final List< Handler<ReverseProxyResult>> onResolveActions = new ArrayList<>();

        /** Optional fallback resolver consulted when no rule matches. */
        private Function<HttpUrl, HttpUrl> resolver;

        /** Optional admin port; when {@code null}, admin support is disabled. */
        private Integer adminPort;
        /** Initial admin token (required when {@link #adminPort} is set). */
        private String adminToken;

        /** Override of the default Vert.x server options. */
        private HttpServerOptions proxyHttpServerOptions;
        /** TLS strategy; defaults to {@link HttpCertificate#NONE}. */
        private HttpCertificate certificate = HttpCertificate.NONE;

        /** TLS key path (set by {@link #ssl(Path, Path)}). */
        private Path sslKeyPath;
        /** TLS certificate path (set by {@link #ssl(Path, Path)}). */
        private Path sslCertPath;

        /** Whether the upstream-facing engines should themselves use TLS. */
        private boolean sslProxyEngine;
        /** HTTP engine override; defaults to a freshly built one. */
        private HttpEngine proxyHttpEngine;
        /** WebSocket engine override; defaults to a freshly built one. */
        private WebSocketEngine proxyWebSocketEngine;

        /** Whether unresolved requests are silently dropped or rejected. */
        private boolean silent = true;

        /** Private — use {@link ReverseProxyServer#builder(int)}. */
        private Builder(final int proxyPort) {
            super();
            this.proxyPort = proxyPort;
        }

        /**
         * Enables the admin server on the given port with the supplied initial
         * token.
         *
         * @param adminPort  port to bind the admin server on
         * @param adminToken initial bearer token (must not be {@code null})
         * @return this builder
         */
        public Builder admin(final int adminPort,
                             final String adminToken) {
            Objects.requireNonNull(adminToken, "needs token");
            this.adminPort = adminPort;
            this.adminToken = adminToken;
            return this;
        }

        /**
         * Enables TLS with PEM-encoded key and certificate files.
         *
         * @param sslKeyPath  path to the PEM key (must not be {@code null})
         * @param sslCertPath path to the PEM certificate (must not be {@code null})
         * @return this builder
         */
        public Builder ssl(final Path sslKeyPath,
                           final Path sslCertPath) {
            Objects.requireNonNull(sslKeyPath, "needs sslKeyPath");
            Objects.requireNonNull(sslCertPath, "needs sslCertPath");
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
            this.sslKeyPath = null;
            this.sslCertPath = null;
            return this;
        }

        /**
         * Overrides the default Vert.x server options applied to both servers.
         *
         * @param httpServerOptions options to use
         * @return this builder
         */
        public Builder options(final HttpServerOptions httpServerOptions) {
            this.proxyHttpServerOptions = httpServerOptions;
            return this;
        }

        /**
         * Whether the upstream-facing engines themselves use TLS (matches the
         * scheme of the upstreams declared in the rules).
         *
         * @param sslProxy {@code true} to enable TLS on the upstream engines
         * @return this builder
         */
        public Builder sslProxy(final boolean sslProxy) {
            this.sslProxyEngine = sslProxy;
            return this;
        }

        /**
         * Sets the silent mode for unresolved requests.
         *
         * @param silent {@code true} to drop unresolved requests silently;
         *               {@code false} to reject with {@code 404}
         * @return this builder
         */
        public Builder silent(final boolean silent) {
            this.silent = silent;
            return this;
        }

//        public Builder webSocketEngine(final WebSocketEngine webSocketEngine) {
//            this.proxyWebSocketEngine = webSocketEngine;
//            return this;
//        }

//        public Builder httpEngine(final HttpEngine httpEngine) {
//            this.proxyHttpEngine = httpEngine;
//            return this;
//        }

        /**
         * Adds a rule; throws when a rule with the same {@code from} is
         * already registered.
         *
         * @param reverseProxyRule rule to add (no-op when {@code null})
         * @return this builder
         * @throws ReverseProxyException when a duplicate {@code from} is registered
         */
        public Builder rule(final ReverseProxyRule reverseProxyRule) {
            if (reverseProxyRule != null) {
                final String fullPath = reverseProxyRule.from().id();
                if (reverseProxyRules.containsKey(fullPath)) {
                    throw new ReverseProxyException("reverse proxy rule already exits: "
                                                   + reverseProxyRules.get(fullPath));
                }
                else {
                    this.reverseProxyRules.put(fullPath, reverseProxyRule);
                }
            }
            return this;
        }

        /**
         * Adds a batch of rules through {@link #rule(ReverseProxyRule)}.
         *
         * @param reverseProxyRules rules to add (no-op when {@code null})
         * @return this builder
         */
        public Builder rules(final Set<ReverseProxyRule> reverseProxyRules) {
            if (reverseProxyRules != null) {
                for (final ReverseProxyRule reverseProxyRule : reverseProxyRules) {
                    rule(reverseProxyRule);
                }
            }
            return this;
        }

        /**
         * Installs a fallback resolver consulted when no rule matches.
         *
         * @param resolver function mapping an inbound URL to a target URL (or
         *                 {@code null} to signal a miss)
         * @return this builder
         */
        public Builder elseRule(final Function<HttpUrl, HttpUrl> resolver) {
            this.resolver = resolver;
            return this;
        }

        /**
         * Registers a resolution listener.
         *
         * @param handler handler invoked for every resolution outcome
         * @return this builder
         */
        public Builder onResolve(final Handler<ReverseProxyResult> handler) {
            onResolveActions.add(handler);
            return this;
        }

        /**
         * Materializes the server, applying defaults for any unset options,
         * and starts the proxy listener.
         *
         * @return a future resolving to the started server
         */
        public Future<ReverseProxyServer> start() {
            final ReverseProxyServer result = new ReverseProxyServer();
            if (this.proxyHttpServerOptions == null) {
                this.proxyHttpServerOptions = ReverseProxyServer.defaultOptions(certificate);
            }
            if (this.proxyHttpEngine == null) {
                final WebClientOptions webClientOptions = HttpEngine.defaultOptions();
                webClientOptions.setSsl(sslProxyEngine)
                                .setUseAlpn(sslProxyEngine)
                                .setVerifyHost(sslProxyEngine)
                                .setTrustAll(sslProxyEngine);
                this.proxyHttpEngine = new HttpEngine(webClientOptions);
            }
            if (this.proxyWebSocketEngine == null) {
                final WebSocketClientOptions webSocketClientOptions = WebSocketEngine.defaultOptions();
                webSocketClientOptions.setSsl(sslProxyEngine)
                                      .setVerifyHost(sslProxyEngine)
                                      .setTrustAll(sslProxyEngine)
                                      .setUseAlpn(sslProxyEngine);
                this.proxyWebSocketEngine = new WebSocketEngine(webSocketClientOptions);
            }

            return result.start(new ServerConfig(proxyPort,
                                                 adminPort,
                                                 proxyHttpServerOptions,
                                                 certificate,
                                                 sslKeyPath,
                                                 sslCertPath),
                                adminToken,
                                silent,
                                proxyHttpEngine,
                                proxyWebSocketEngine,
                                new HashSet<>(reverseProxyRules.values()),
                                resolver,
                                onResolveActions);
        }

    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Returns the header name used to carry the admin token on every admin request.
     *
     * @return the header name used to carry the admin token on every admin request
     */
    public static String adminTokenHeaderKey() {
        return adminTokenHeader;
    }

    /**
     * Returns the header name used to carry the replacement token on token rotations.
     *
     * @return the header name used to carry the replacement token on token rotations
     */
    public static String adminNewTokenHeaderKey() {
        return adminNewTokenHeader;
    }

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
