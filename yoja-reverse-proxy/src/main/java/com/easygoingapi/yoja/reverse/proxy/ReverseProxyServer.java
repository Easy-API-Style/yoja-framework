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
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ServerSSLOptions;

/**
 * Lifecycle entry point of the reverse-proxy module.
 * <p>
 * A {@code ReverseProxyServer} owns two Vert.x {@link HttpServer} instances
 * sharing the same TLS material:
 * <ul>
 *   <li>the <strong>proxy server</strong> listens on the public port and forwards every
 *       request — HTTP and WebSocket, streamed — through a {@link ReverseProxySwitcherHttpProxy};</li>
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
 * The names of the two admin headers ({@link #adminTokenHeaderKey()},
 * {@link #adminNewTokenHeaderKey()}) are exposed so that clients can attach
 * the right headers when calling the admin API.
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
    private static final String adminTokenHeader = "Reserve-Proxy-Token";
    /** Header name carrying the replacement token on {@code POST /update/token}. */
    private static final String adminNewTokenHeader = "New-Reserve-Proxy-Token";

    /** Captured server configuration, populated at start time; TLS paths refreshed by {@link #updateCertificate(Path, Path)}. */
    private volatile ServerConfig proxyServerConfig;

    /** Vert.x server backing the admin endpoints; created by {@link #startAdmin()}, from the current TLS material. */
    private HttpServer adminProxyServer;
    /** Lifecycle state of the admin server. */
    private AtomicReference<State> adminProxyServerState = new AtomicReference<>(State.stopped);

//    private HttpClient proxyClient;
    /** Vert.x server backing the proxy endpoints. */
    private HttpServer proxyServer;
    /** Lifecycle state of the proxy server. */
    private AtomicReference<State> proxyServerState = new AtomicReference<>(State.stopped);

    /** The switcher that resolves and dispatches every inbound request. */
    private ReverseProxySwitcherHttpProxy proxySwitcher;

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
     * Hot-swaps the TLS material on the proxy server, and on the admin server
     * when it is listening, without restarting either listener. An admin
     * server started later uses the new material too. On success,
     * {@link #keyPath()} and {@link #certificatePath()} are updated to reflect
     * the new paths.
     *
     * @param sslKeyPath  path to the PEM-encoded key
     * @param sslCertPath path to the PEM-encoded certificate
     * @return a future resolving to {@code true} when every listening server
     *         was updated, {@code false} when one of them refused the new
     *         material (the cause is logged); failed with a
     *         {@link ReverseProxyException} when TLS is not enabled
     */
    @Override
    public Future<Boolean> updateCertificate(final Path sslKeyPath,
                                             final Path sslCertPath) {
        if (HttpCertificate.SSL != proxyServerConfig.certificate()) {
            return Future.failedFuture(new ReverseProxyException("TLS not enabled: no certificate to update"));
        }
        final PemKeyCertOptions pemKeyCertOptions = pemKeyCertOptions(sslKeyPath, sslCertPath);
        final PemTrustOptions pemTrustOptions = pemTrustOptions(sslCertPath);

        final ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(pemKeyCertOptions);
        sslOptions.setTrustOptions(pemTrustOptions);
        final List<Future<?>> futures = new ArrayList<>();
        futures.add(proxyServer.updateSSLOptions(sslOptions));
        if (adminProxyServerState.get() == State.started) {
            futures.add(adminProxyServer.updateSSLOptions(sslOptions));
        }
        return Future.join(futures)
                     .transform(h -> {
                         if (h.failed()) {
                             LOGGER.error("[http] [reverseProxy] [certificate] [update] [failed] key={}, certificate={}",
                                          sslKeyPath, sslCertPath, h.cause());
                             return Future.succeededFuture(false);
                         }
                         // the admin server started later is created from these options
                         proxyServerConfig.options()
                                          .setKeyCertOptions(pemKeyCertOptions)
                                          .setTrustOptions(pemTrustOptions);
                         this.proxyServerConfig = new ServerConfig(proxyServerConfig.port(),
                                                                   proxyServerConfig.adminPort(),
                                                                   proxyServerConfig.options(),
                                                                   proxyServerConfig.certificate(),
                                                                   sslKeyPath,
                                                                   sslCertPath);
                         return Future.succeededFuture(true);
                     });
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
                this.adminProxyServer = YojaApp.vertx().createHttpServer(new HttpServerOptions(proxyServerConfig.options()));
                this.adminProxyServer.requestHandler(proxySwitcher.adminRouter());
                return this.adminProxyServer
                           .listen(proxyServerConfig.adminPort(), proxyServerConfig.options().getHost())
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
     * Closes the admin server's listener. No-op when the admin server is not
     * started.
     *
     * @return a future completing once the listener is closed (or already was)
     * @throws ReverseProxyException when admin support was not enabled in the builder
     */
    public Future<Void> stopAdmin() {
        if (proxyServerConfig.adminPort() != null) {
            if (adminState() != State.started) {
                return Future.succeededFuture();
            }
            adminProxyServerState.set(State.stopping);
            return adminProxyServer.close()
                                   .onFailure(e -> {
                                       adminProxyServerState.set(State.started);
                                       LOGGER.error("[http] [reverseProxy] [admin] [stopping] [failed] port={}, certificate={}",
                                                    proxyServerConfig.adminPort(),
                                                    proxyServerConfig.certificate(), e);
                                   })
                                   .onSuccess(h -> {
                                       adminProxyServerState.set(State.stopped);
                                       LOGGER.info("[http] [reverseProxy] [admin] [stopped] port={}, certificate={}",
                                                   proxyServerConfig.adminPort(),
                                                   proxyServerConfig.certificate());
                                   })
                                   .mapEmpty();
        }
        else {
            throw new ReverseProxyException("admin port no defined");
        }
    }

    /**
     * Binds the proxy server on its configured port and host. No-op when the
     * proxy server is not stopped.
     *
     * @return a future completing once the listener is bound (or already was)
     */
    public Future<Void> start() {
        if (proxyState() != State.stopped) {
            return Future.succeededFuture();
        }
        proxyServerState.set(State.starting);
        // on the options' host: listen(port) alone would ignore it and listen on every interface
        return this.proxyServer
                   .listen(proxyServerConfig.port(), proxyServerConfig.options().getHost())
                   .onFailure(e -> {
                       proxyServerState.set(State.stopped);
                       LOGGER.error("[http] [reverseProxy] [failed] port={}, certificate={}",
                                    proxyServerConfig.port(),
                                    proxyServerConfig.certificate(), e);
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
     * Stops the proxy server, the switcher's client to the upstreams and (when it
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
        // join, not all: every component is waited for, even when one fails
        return Future.join(futures)
                     .onFailure(e -> {
                         // not known to be stopped: started, so that stop() can be tried again
                         proxyServerState.set(State.started);
                         if (adminProxyServerState.get() == State.stopping) {
                             adminProxyServerState.set(State.started);
                         }
                         LOGGER.error("[http] [reverseProxy] [stopping] [failed] port={}, certificate={}",
                                      proxyServerConfig.port(),
                                      proxyServerConfig.certificate(), e);
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
     * @param sslProxy             whether every upstream is dialed over TLS, whatever its rule says
     * @param clientOptions        options of the client dialing the upstreams
     * @param reverseProxyRules    initial rule set
     * @param resolver             optional fallback resolver
     * @param onResolveActions     resolution listeners
     * @return a future resolving to this server once the proxy listener is bound
     */
    private Future<ReverseProxyServer> start(final ServerConfig proxyServerConfig,
                                             final String adminToken,
                                             final boolean silent,
                                             final boolean sslProxy,
                                             final HttpClientOptions clientOptions,
                                             final Set<ReverseProxyRule> reverseProxyRules,
                                             final Function<HttpUrl, HttpUrl> resolver,
                                             final List<Handler<ReverseProxyResult>> onResolveActions) {
        this.proxyServerConfig = proxyServerConfig;
        this.setHttpServerOptions(proxyServerConfig);
        this.proxySwitcher = new ReverseProxySwitcherHttpProxy(adminToken, silent, proxyServerConfig.port(),
                                                               sslProxy, clientOptions,
                                                               reverseProxyRules, resolver,
                                                               onResolveActions);
        // admin: created by startAdmin(), from the TLS material current then
        // proxy
        this.proxyServer = YojaApp.vertx().createHttpServer(proxyServerConfig.options());
        // no WebSocket handler: a handshake reaches the request handler, the proxy forwards it as such
        this.proxyServer.requestHandler(proxySwitcher.proxyHandler());
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
            serverConfig.options()
                        .setKeyCertOptions(pemKeyCertOptions(serverConfig.sslKeyPath(), serverConfig.sslCertPath()))
                        .setTrustOptions(pemTrustOptions(serverConfig.sslCertPath()));
        }
    }

    /**
     * Builds the PEM key/certificate options of a TLS material.
     *
     * @param sslKeyPath  path to the PEM-encoded key
     * @param sslCertPath path to the PEM-encoded certificate
     * @return the Vert.x key/certificate options
     */
    private static PemKeyCertOptions pemKeyCertOptions(final Path sslKeyPath,
                                                       final Path sslCertPath) {
        final PemKeyCertOptions result = new PemKeyCertOptions();
        result.setKeyPath(sslKeyPath.toString());
        result.setCertPath(sslCertPath.toString());
        return result;
    }

    /**
     * Builds the PEM trust options of a TLS material.
     *
     * @param sslCertPath path to the PEM-encoded certificate
     * @return the Vert.x trust options
     */
    private static PemTrustOptions pemTrustOptions(final Path sslCertPath) {
        final PemTrustOptions result = new PemTrustOptions();
        result.addCertPath(sslCertPath.toString());
        return result;
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
        if (proxyServerConfig.adminPort() != null) {
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
     * configuration and upstream TLS settings, then starts the server via
     * {@link #start()}. Sensible defaults are applied for the Vert.x server
     * options when not overridden.
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

        /** Whether every upstream is dialed over TLS, whatever its rule says. */
        private boolean sslProxy;
        /** PEM certificate(s) trusted for the upstreams, on top of the JVM's; {@code null}: the JVM's only. */
        private Path sslProxyTrustPath;

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
         * Whether every upstream is dialed over TLS, whatever its rule says
         * (default {@code false}: an upstream is dialed over TLS when its rule
         * says so, {@code Url.to(true, host)}). The upstreams' certificates are
         * verified -- trusted by the JVM, or by {@link #sslProxyTrust(Path)} --
         * and must match their host name.
         *
         * @param sslProxy {@code true} to dial every upstream over TLS
         * @return this builder
         */
        public Builder sslProxy(final boolean sslProxy) {
            this.sslProxy = sslProxy;
            return this;
        }

        /**
         * Dials every upstream over TLS (see {@link #sslProxy(boolean)}),
         * trusting the PEM certificate(s) of the given file on top of the
         * JVM's: the self-signed certificate of an upstream, or the private CA
         * that signed it.
         *
         * @param sslCertPath path to the PEM certificate(s) to trust
         * @return this builder
         */
        public Builder sslProxyTrust(final Path sslCertPath) {
            this.sslProxyTrustPath = Objects.requireNonNull(sslCertPath, "needs sslCertPath");
            this.sslProxy = true;
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
         * <p>
         * The resolver receives the full inbound {@link HttpUrl} (including its
         * query string) and returns the target URL used <em>as-is</em>. Unlike
         * rule-based resolution, the inbound query is <strong>not</strong>
         * merged automatically: if you want to preserve it, copy it onto the
         * returned URL, e.g.
         * {@code .parameterQuery(inbound.parameterQuery(Format.decoded))}.
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
         * Registers a resolution listener. It runs on the Vert.x event loop,
         * before the request is forwarded: it must not block.
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
            // the upstreams' certificates are verified: trusted, and matching their host name
            final HttpClientOptions clientOptions = new HttpClientOptions().setVerifyHost(true)
                                                                           .setTrustAll(false);
            if (sslProxyTrustPath != null) {
                clientOptions.setTrustOptions(new PemTrustOptions().addCertPath(sslProxyTrustPath.toString()));
            }

            return result.start(new ServerConfig(proxyPort,
                                                 adminPort,
                                                 proxyHttpServerOptions,
                                                 certificate,
                                                 sslKeyPath,
                                                 sslCertPath),
                                adminToken,
                                silent,
                                sslProxy,
                                clientOptions,
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
     * strategy: HTTP/2 with ALPN fallback to HTTP/1.1 when TLS is enabled via
     * {@link HttpCertificate#SSL}.
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
        return httpServerOptions;
    }

}
