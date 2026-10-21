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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyOptions;

/**
 * {@link ReverseProxySwitcher} forwarding the traffic with the Vert.x
 * {@link HttpProxy}: requests and responses are <b>streamed</b> -- never held
 * in memory, whatever their size -- with back-pressure, the hop-by-hop headers
 * ({@code Connection}, {@code Transfer-Encoding}...) are handled, and a
 * WebSocket handshake is forwarded as such, its frames then flowing both ways.
 * <p>
 * The request handler returned by {@link #proxyHandler()}:
 * <ol>
 *   <li>resolves the inbound URL through the inherited switcher logic -- a
 *       WebSocket handshake as {@code ws(s)://} -- and publishes the outcome
 *       to the {@code onResolve} listeners;</li>
 *   <li>unresolved: answers {@code 404}, or, in silent mode, drops the request
 *       (its stream is reset: no response is sent);</li>
 *   <li>resolved: hands the request to the {@link HttpProxy}, whose origin is
 *       the resolved upstream -- its host, its load-balanced port, over TLS
 *       when its rule says so or when every upstream is -- the request's path and query being rewritten
 *       to the resolved ones.</li>
 * </ol>
 * An upstream that can't be reached is answered {@code 502} by the proxy, the
 * cause being logged.
 */
public class ReverseProxySwitcherHttpProxy extends ReverseProxySwitcher {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReverseProxySwitcherHttpProxy.class);

    /** Client dialing the upstreams. */
    private final HttpClient proxyClient;
    /** Whether every upstream is dialed over TLS, whatever its rule says. */
    private final boolean sslProxy;
    /** The Vert.x reverse proxy, whose origin is {@link #originRequest(ProxyContext)}. */
    private final HttpProxy httpProxy;
    /**
     * Upstream URL resolved for each request handed to {@link #httpProxy},
     * until its origin request is created: the proxy asks for the origin
     * right away, the entry lives for an instant.
     */
    private final Map<HttpServerRequest, HttpUrl> origins = new ConcurrentHashMap<>();

    /**
     * Constructs a new switcher forwarding with the Vert.x {@link HttpProxy}.
     *
     * @param adminToken        initial admin bearer token
     * @param silent            when {@code true}, unresolved requests are dropped
     * @param proxyPort         port the proxy listens on
     * @param sslProxy          whether every upstream is dialed over TLS, whatever its rule says
     * @param clientOptions     options of the client dialing the upstreams (TLS trust...)
     * @param reverseProxyRules initial rule set
     * @param resolver          optional fallback resolver
     * @param onResolveActions  resolution listeners
     */
    protected ReverseProxySwitcherHttpProxy(final String adminToken,
                                            final boolean silent,
                                            final int proxyPort,
                                            final boolean sslProxy,
                                            final HttpClientOptions clientOptions,
                                            final Set<ReverseProxyRule> reverseProxyRules,
                                            final Function<HttpUrl, HttpUrl> resolver,
                                            final List<Handler<ReverseProxyResult>> onResolveActions) {
        super(adminToken, silent, proxyPort, reverseProxyRules, resolver, onResolveActions);
        this.sslProxy = sslProxy;
        this.proxyClient = YojaApp.vertx().createHttpClient(clientOptions);
        this.httpProxy = HttpProxy.reverseProxy(new ProxyOptions().setSupportWebSocket(true), proxyClient)
                                  .origin(this::originRequest);
    }

    /**
     * Returns the request handler of the proxy HTTP server. See the
     * class-level Javadoc for the dispatch sequence.
     *
     * @return the request handler, HTTP and WebSocket handshakes alike
     */
    public Handler<HttpServerRequest> proxyHandler() {
        return request -> {
            final ReverseProxyResult reverseProxyResult = resolve(request);
            if (reverseProxyResult != null
                    && reverseProxyResult.isResolved()) {
                origins.put(request, reverseProxyResult.toUrl());
                httpProxy.handle(request);
            }
            else if (isSilent()) {
                // dropped: no response, the stream (HTTP/2) or the connection (HTTP/1.x) is reset
                request.response().reset();
            }
            else {
                request.response().setStatusCode(404).end();
            }
        };
    }

    /**
     * Creates the request to the upstream resolved for an inbound request:
     * aimed at its host and port, over TLS when it is {@code https}/{@code wss}
     * (or when every upstream is),
     * its path and query rewritten to the resolved ones. The upstream port's
     * usage counter is held until the upstream's response is over.
     *
     * @param proxyContext the proxy's context of the inbound request
     * @return a future resolving to the request to the upstream
     */
    private Future<HttpClientRequest> originRequest(final ProxyContext proxyContext) {
        final HttpServerRequest request = proxyContext.request().proxiedRequest();
        final HttpUrl url = origins.remove(request);
        if (url == null) {
            return Future.failedFuture(new ReverseProxyException("no upstream resolved for " + request.uri()));
        }
        proxyContext.request().setURI(url.pathAndQuery(Format.encoded));
        if (request.version() == HttpVersion.HTTP_2
                && !request.headers().contains(HttpHeaders.CONTENT_LENGTH)
                && (request.method() == HttpMethod.GET
                        || request.method() == HttpMethod.HEAD)) {
            // an HTTP/2 GET has no length: the proxy would send the upstream (HTTP/1.1) an empty chunked
            // body -- sent without body instead, a GET's body having no meaning (RFC 9110)
            proxyContext.request().setBody(null);
        }
        final boolean ssl = sslProxy
                         || url.protocol() == HttpProtocole.https
                         || url.protocol() == HttpProtocole.wss;
        final RequestOptions requestOptions = new RequestOptions().setServer(SocketAddress.inetSocketAddress(url.port(),
                                                                                                             url.host()))
                                                                  .setHost(url.host())
                                                                  .setPort(url.port())
                                                                  .setSsl(ssl);
        usePort(url.port());
        final AtomicBoolean released = new AtomicBoolean();
        final Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                releasePort(url.port());
            }
        };
        return proxyContext.client()
                           .request(requestOptions)
                           .onSuccess(clientRequest -> clientRequest.response()
                                                                    .compose(HttpClientResponse::end)
                                                                    .onComplete(h -> release.run()))
                           .onFailure(e -> {
                               release.run();
                               LOGGER.error("[http] [reverseProxy] upstream unreachable {} for {} {}",
                                            url, request.method(), request.uri(), e);
                           });
    }

    /**
     * Closes the client dialing the upstreams.
     *
     * @return a future completing when the client has closed
     */
    @Override
    protected Future<Void> close() {
        return proxyClient.close();
    }

}
