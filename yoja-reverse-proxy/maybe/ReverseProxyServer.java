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

import com.google.common.collect.Lists;
import com.easygoingapi.yoja.certificate.server.Certificatable;
import com.easygoingapi.yoja.certificate.server.ServerCertificateManager;
import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.http.HttpUrl;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyOptions;

public class ReverseProxyServer implements Certificatable {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(ReverseProxyServer.class);
    
    public static enum State { stopping, stopped, starting, started }

    private HttpClient proxyClient;
    
    private HttpServer proxyServer;
    private int proxyPort;
    
    private HttpServer adminServer;
    private Integer adminPort;
    
    private HttpCertificate httpCertificate;
    
    private Path sslKeyPath;
    private Path sslCertPath;
    
    private AtomicReference<State> adminState = new AtomicReference<>(State.stopped);
    private AtomicReference<State> proxyState = new AtomicReference<>(State.stopped);
    
    private ReverseProxyServer() {
        super();
    }
    
    public int proxyPort() {
        return proxyPort;
    }

    public Integer adminPort() {
        return adminPort;
    }

    public HttpCertificate certificate() {
        return httpCertificate;
    }
    
    public State getAdminState() {
        return adminState.get();
    }
    
    public State getProxyState() {
        return proxyState.get();
    }
    
    @Override
    public Path getKeyPath() {
        return sslKeyPath;
    }

    @Override
    public Path getCertificatePath() {
        return sslCertPath;
    }

    @Override
    public Future<Boolean> updateCertificate(final Path sslKeyPath,
                                             final Path sslCertPath) {
        final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        pemKeyCertOptions.setKeyPath(sslKeyPath.toString());
        pemKeyCertOptions.setCertPath(sslCertPath.toString());
        
        final PemTrustOptions pemTrustOptions = new PemTrustOptions();
        pemTrustOptions.addCertPath(sslCertPath.toString());
        
        final SSLOptions sslOptions = new SSLOptions();
        sslOptions.setKeyCertOptions(pemKeyCertOptions);
        sslOptions.setTrustOptions(pemTrustOptions);
        final List<Future<?>> futures = new ArrayList<>();
        futures.add(proxyServer.updateSSLOptions(sslOptions));
        futures.add(adminServer.updateSSLOptions(sslOptions));
        return Future.all(futures).map(v -> v.failed() ? false : true);
    }

    public Future<Void> startAdmin() {
        if (adminPort != null) {
            adminState.set(State.stopping);
            return this.adminServer
                       .listen(adminPort)
                       .onFailure(e -> {
                            adminState.set(State.stopped);
                            LOGGER.error("[http] [reverseProxy] [admin] [starting] [failed] port={}, certificate={}", 
                                         adminPort, httpCertificate);
                       })
                       .onSuccess(h -> {
                             adminState.set(State.started);
                             LOGGER.info("[http] [reverseProxy] [admin] [started] port={}, certificate={}", 
                                         adminPort, httpCertificate);
                       })
                        .mapEmpty();
        }
        else {
            throw new ReverseProxyException("admin port not defined");
        }
    }
    
    public Future<Void> stopAdmin() {
        if (adminServer != null) {
            adminState.set(State.stopping);
            return adminServer.close()
                              .onFailure(e -> {
                                  adminState.set(State.started);
                                  LOGGER.error("[http] [reverseProxy] [admin] [stopping] [failed] port={}, certificate={}", 
                                               adminPort, httpCertificate);
                              })
                              .onSuccess(h -> {
                                  adminState.set(State.stopped);
                                  LOGGER.info("[http] [reverseProxy] [admin] [stopped] port={}, certificate={}", 
                                                adminPort, httpCertificate);
                              });
        }
        else {
            throw new ReverseProxyException("admin port no defined");
        }
    }
    public Future<Void> start() {
        proxyState.set(State.starting);
        return this.proxyServer
                   .listen(proxyPort)
                   .onFailure(e -> {
                       proxyState.set(State.stopped);
                       LOGGER.error("[http] [reverseProxy] [failed] port={}, certificate={}", 
                                    proxyPort, httpCertificate);
                   })
                   .onSuccess(h -> {
                       proxyState.set(State.started);
                       LOGGER.info("[http] [reverseProxy] [started] port={}, certificate={}", 
                                      proxyPort, httpCertificate);
                   })
                   .mapEmpty();
    }
    
    public Future<Void> stop() {
        proxyState.set(State.stopping);
        final List<Future<?>> futures = new ArrayList<>();
        futures.add(proxyClient.close());
        futures.add(proxyServer.close());
        return Future.all(futures)
                     .onFailure(e -> {
                         proxyState.set(State.started);
                         LOGGER.error("[http] [reverseProxy] [stopping] [failed] port={}, certificate={}", 
                                         proxyPort, httpCertificate);
                     })
                     .onSuccess(h -> {
                         proxyState.set(State.stopped);
                         LOGGER.info("[http] [reverseProxy] [stopped] port={}, certificate={}", 
                                     proxyPort, httpCertificate);
                     })
                     .mapEmpty();
    }
    
    private Future<ReverseProxyServer> start(final Integer adminPort,
                                             final String adminToken,
                                             final int proxyPort,
                                             final HttpCertificate httpCertificate,
                                             final Path sslKeyPath,
                                             final Path sslCertPath,
                                             final Set<ReverseProxyRule> reverseProxyRules,
                                             final Function<HttpUrl, HttpUrl> resolver,
                                             final List<Handler<ReverseProxyResult>> onResolveActions) {
        proxyState.set(State.starting);
        this.proxyPort = proxyPort;
        this.adminPort = adminPort;
        this.httpCertificate = httpCertificate;
        this.sslKeyPath = sslKeyPath;
        this.sslCertPath = sslCertPath;
        // CERTIFICATE
        final HttpServerOptions httpServerOptions = getHttpServerOptions();
        final ReverseProxySwitcher proxySwitcher = new ReverseProxySwitcher(adminToken, proxyPort, 
                                                                            reverseProxyRules, resolver, 
                                                                            onResolveActions);
        this.proxyClient = YojaApp.vertx().createHttpClient();
        final ProxyOptions proxyOptions = new ProxyOptions();
        final HttpProxy httpProxy = HttpProxy.reverseProxy(proxyOptions, this.proxyClient);
        httpProxy.originRequestProvider((request, client) ->  proxySwitcher.resolveOrigin(request, client));
        httpProxy.addInterceptor(proxySwitcher.proxyInterceptor());
        // admin
        this.adminServer = YojaApp.vertx().createHttpServer(httpServerOptions);
        this.adminServer.requestHandler(proxySwitcher.router());
        // proxy
        this.proxyPort = proxyPort;
        this.proxyServer = YojaApp.vertx().createHttpServer(httpServerOptions);
        this.proxyServer.webSocketHandler(serverWebSocket -> {
            proxySwitcher.websocket(serverWebSocket);
        });
        this.proxyServer.requestHandler(httpProxy);
        return start().map(this);
    }
    
    private HttpServerOptions getHttpServerOptions() {
        final HttpServerOptions httpServerOptions = new HttpServerOptions();
        if (HttpCertificate.SSL == httpCertificate) {
            httpServerOptions.setSsl(true);
            httpServerOptions.setUseAlpn(true);
            httpServerOptions.setAlpnVersions(Lists.newArrayList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            
            final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
            pemKeyCertOptions.setKeyPath(sslKeyPath.toString());
            pemKeyCertOptions.setCertPath(sslCertPath.toString());
            httpServerOptions.setKeyCertOptions(pemKeyCertOptions);
            
            final PemTrustOptions pemTrustOptions = new PemTrustOptions();
            pemTrustOptions.addCertPath(sslCertPath.toString());
            httpServerOptions.setTrustOptions(pemTrustOptions);
            ServerCertificateManager.add(this);
        }
        else if (HttpCertificate.SELF_SIGNED == httpCertificate) {
            httpServerOptions.setSsl(true);
            httpServerOptions.setUseAlpn(true);
            httpServerOptions.setAlpnVersions(Lists.newArrayList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            final SelfSignedCertificate selfSignedCertificate = SelfSignedCertificate.create();
            httpServerOptions.setKeyCertOptions(selfSignedCertificate.keyCertOptions());
            httpServerOptions.setTrustOptions(selfSignedCertificate.trustOptions());
        }
        return httpServerOptions;
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(ReverseProxyServer.class.getSimpleName());
        result.append(" [proxyPort=");
        result.append(proxyPort);
        result.append(", adminPort=");
        result.append(adminPort);
        result.append(", certificate=");
        result.append(httpCertificate);
        result.append(", adminState=");
        result.append(adminState.get());
        result.append(", proxyState=");
        result.append(proxyState.get());
        result.append("]");
        return result.toString();
    }

    /*
     * 
     * BUILDER
     * 
     */
    public static Builder builder(final int proxyPort) {
        return new Builder(proxyPort);
    }

    public static class Builder {
        
        private final int proxyPort;
        private final Map<String, ReverseProxyRule> reverseProxyRules = new HashMap<>();
        private final List< Handler<ReverseProxyResult>> onResolveActions = new ArrayList<>();
       
        private Function<HttpUrl, HttpUrl> resolver;
        
        private Integer adminPort;
        private String adminToken;
        
        private HttpCertificate certificate = HttpCertificate.NONE;
        private Path sslKeyPath;
        private Path sslCertPath;
        
        private Builder(final int proxyPort) {
            super();
            this.proxyPort = proxyPort;
        }
        
        public Builder admin(final Integer adminPort, 
                             final String adminToken) {
            Objects.requireNonNull(adminToken, "needs token");
            this.adminPort = adminPort;
            this.adminToken = adminToken;
            return this;
        }
        
        public Builder ssl(final Path sslKeyPath,
                           final Path sslCertPath) {
            Objects.requireNonNull(sslKeyPath, "needs sslKeyPath");
            Objects.requireNonNull(sslCertPath, "needs sslCertPath");
            this.certificate = HttpCertificate.SSL;
            this.sslKeyPath = sslKeyPath;
            this.sslCertPath = sslCertPath;
            return this;
        }
        
        public Builder sslSelfSigned() {
            this.certificate = HttpCertificate.SELF_SIGNED;
            return this;
        }
        
        public Builder rule(final ReverseProxyRule reverseProxyRule) {
            if (reverseProxyRule != null) {
                final String fullPath = reverseProxyRule.from().id();
                if (reverseProxyRules.containsKey(fullPath)) {
                    throw new ReverseProxyException("reverse proxy rule already exits: " + reverseProxyRules.get(fullPath));
                }
                else {
                    this.reverseProxyRules.put(fullPath, reverseProxyRule);
                }
            }
            return this;
        }
        
        public Builder rules(final Set<ReverseProxyRule> reverseProxyRules) {
            if (reverseProxyRules != null) {
                for (final ReverseProxyRule reverseProxyRule : reverseProxyRules) {
                    rule(reverseProxyRule);
                }
            }
            return this;
        }
        
        public Builder elseRule(final Function<HttpUrl, HttpUrl> resolver) {
            this.resolver = resolver;
            return this;
        }
        
        public Builder onResolve(final Handler<ReverseProxyResult> handler) {
            onResolveActions.add(handler);
            return this;
        }
        
        public Future<ReverseProxyServer> start() {
            final ReverseProxyServer result = new ReverseProxyServer();
            return result.start(adminPort,
                                adminToken,
                                proxyPort, 
                                certificate, 
                                sslKeyPath, 
                                sslCertPath,
                                new HashSet<>(reverseProxyRules.values()),
                                resolver,
                                onResolveActions);
        }
        
    }
    
}
