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

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.util.PathUtil;
import com.easygoingapi.yoja.core.worker.Worker;
import com.easygoingapi.yoja.http.client.WebSocketEngine;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;
import com.google.common.base.Strings;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.ClientWebSocket;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.ServerWebSocketHandshake;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Dispatch engine of the reverse proxy: resolves inbound URLs against the
 * configured {@link ReverseProxyRule} table and exposes an HTTP admin API to
 * mutate that table at runtime.
 * <p>
 * Resolution is performed by {@link #resolve(HttpUrl)}: starting from the full
 * inbound path, the switcher walks back segment by segment until a rule key
 * (host + sub-path) is found in the table, then applies the rule's
 * {@link Url.To} (path rewriting, load-balanced port pick, protocol coercion).
 * If no rule matches, the optional fallback {@code resolver} function is
 * consulted. Every resolution — successful or not — is published to the
 * {@code onResolve} listeners.
 * <p>
 * The admin API is mounted on its own Vert.x {@link Router} returned by
 * {@link #adminRouter()} and protected by a token header
 * ({@code Reserve-Proxy-Token}). Admin operations run serialized through
 * {@link Worker.singleThread} keyed by this switcher's port to avoid races
 * between concurrent updates and lookups. WebSocket connections are also
 * dispatched here ({@link #websocketHandshake} / {@link #websocket}); HTTP
 * dispatching is the responsibility of the subclass
 * {@link ReverseProxySwitcherWebClient}.
 * <p>
 * Per-upstream-port usage counters are kept in {@link #usedPorts} so the
 * {@link #loadBalancing(List)} helper can pick the least-loaded port among
 * the candidates declared by a rule.
 */
public class ReverseProxySwitcher {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReverseProxySwitcher.class);

    /** When {@code true}, missed resolutions return silently instead of {@code 404}. */
    private final boolean silent;
    /** WebSocket engine used to open client-side sockets towards the upstream. */
    private final WebSocketEngine proxyWebSocketEngine;

    /** Current rule table keyed by {@code from.id()}. */
    private final Map<String, ReverseProxyRule> rules = new ConcurrentHashMap<>();
    /** Live count of in-flight requests per upstream port, used by {@link #loadBalancing(List)}. */
    private final Map<Integer, Integer> usedPorts = new ConcurrentHashMap<>();

    /** Listeners invoked for every resolution attempt (resolved or not). */
    protected final List<Handler<ReverseProxyResult>> onResolveActions;
    /** Fallback resolver consulted when no rule matches (may be {@code null}). */
    private final Function<HttpUrl, HttpUrl> resolver;

    /** Port the proxy listens on (used as part of the admin worker key). */
    private final int proxyPort;
    /** Stable worker identifier used to serialize admin operations. */
    private final String workerId;

    /** Internal Vert.x router carrying the admin endpoints. */
    private final Router adminRouter;
    /** Current admin token; rotated by {@code POST /update/token}. */
    private String adminToken;

    /**
     * Constructs a new switcher with the given configuration.
     *
     * @param adminToken          initial admin bearer token
     * @param silent              when {@code true}, missed handshakes are not rejected explicitly
     * @param proxyPort           port the proxy listens on
     * @param proxyWebSocketEngine WebSocket engine used to dial upstream
     * @param reverseProxyRules   initial rule set
     * @param resolver            optional fallback resolver consulted when no rule matches
     * @param onResolveActions    listeners invoked for every resolution
     */
    protected ReverseProxySwitcher(final String adminToken,
                                   final boolean silent,
                                   final int proxyPort,
                                   final WebSocketEngine proxyWebSocketEngine,
                                   final Set<ReverseProxyRule> reverseProxyRules,
                                   final Function<HttpUrl, HttpUrl> resolver,
                                   final List<Handler<ReverseProxyResult>> onResolveActions) {
        super();
        this.adminToken = adminToken;
        this.workerId = ReverseProxySwitcher.class.getName() + "_" + proxyPort;
        this.silent = silent;
        this.proxyPort = proxyPort;
        this.proxyWebSocketEngine = proxyWebSocketEngine;
        this.resolver = resolver;
        this.onResolveActions = onResolveActions;
        this.adminRouter = Router.router(YojaApp.vertx());
        this.initialzeRules(reverseProxyRules);
        this.initialzeRoute();
    }

    /**
     * Seeds the rule table and the port-usage counter with the initial rule
     * set.
     *
     * @param reverseProxyRules initial rules
     */
    private void initialzeRules(final Set<ReverseProxyRule> reverseProxyRules) {
        for (final ReverseProxyRule reverseProxyRule : reverseProxyRules) {
            rules.put(reverseProxyRule.from().id(), reverseProxyRule);
            for (final Integer port : reverseProxyRule.to().ports()) {
                usedPorts.put(port, 0);
            }
        }
    }

    /**
     * Returns whether handshake misses are silenced (instead of being rejected).
     *
     * @return whether handshake misses are silenced (instead of being rejected)
     */
    public boolean isSilent() {
        return silent;
    }

    /**
     * Returns the port the proxy listens on.
     *
     * @return the port the proxy listens on
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /*
     *
     * TRAFIC
     *
     */
    /**
     * Resolves the inbound WebSocket handshake URL and either accepts or
     * rejects the upgrade based on whether a target could be derived.
     *
     * @param serverWebSocketHandshake the inbound handshake from Vert.x
     */
    protected void websocketHandshake(final ServerWebSocketHandshake serverWebSocketHandshake) {
        final HttpUrl fromUrl = HttpUrl.builder(serverWebSocketHandshake.authority().host())
                                       .protocol(HttpProtocole.ws)
                                       .port(serverWebSocketHandshake.authority().port())
                                       .path(serverWebSocketHandshake.path())
                                       .parameterQuery(serverWebSocketHandshake.query())
                                       .build();
        final ReverseProxyResult reverseProxyResult = resolve(fromUrl);
        if (reverseProxyResult.isResolved()) {
            serverWebSocketHandshake.accept();
        }
        else if (!isSilent()) {
            serverWebSocketHandshake.reject();
        }
        for (final Handler<ReverseProxyResult> onResolveAction : onResolveActions) {
            onResolveAction.handle(reverseProxyResult);
        }
    }

    /**
     * Wires a bidirectional WebSocket bridge between the accepted server
     * socket and a freshly-dialed client socket aimed at the resolved
     * upstream. Each direction copies text and binary frames; close on either
     * side closes the other.
     * <p>
     * The upstream port's usage counter is incremented on connect and
     * decremented on either close to feed the load-balancing heuristic.
     *
     * @param serverWebSocket the accepted Vert.x server socket
     */
    protected void websocket(final ServerWebSocket serverWebSocket) {
        final HttpUrl fromUrl = HttpUrl.builder(serverWebSocket.authority().host())
                                       .protocol(HttpProtocole.ws)
                                       .port(serverWebSocket.authority().port())
                                       .path(serverWebSocket.path())
                                       .parameterQuery(serverWebSocket.query())
                                       .build();
        final ReverseProxyResult reverseProxyResult = resolve(fromUrl);
        if (reverseProxyResult.isResolved()) {
            final HttpUrl url = reverseProxyResult.toUrl();
            usedPorts.compute(url.port(), (k, v) -> v + 1);
            final ClientWebSocket clientWebSocket = proxyWebSocketEngine.webSocketClient().webSocket();
            serverWebSocket.binaryMessageHandler(v -> clientWebSocket.writeBinaryMessage(v));
            serverWebSocket.textMessageHandler(v -> clientWebSocket.writeTextMessage(v));
            serverWebSocket.closeHandler(h -> {
                clientWebSocket.close();
                usedPorts.compute(url.port(), (k, v) -> Math.max(0, v - 1));
            });
            clientWebSocket.textMessageHandler(v -> serverWebSocket.writeTextMessage(v));
            clientWebSocket.binaryMessageHandler(v -> serverWebSocket.write(v));
            clientWebSocket.closeHandler(h -> {
                serverWebSocket.close();
                usedPorts.compute(url.port(), (k, v) -> Math.max(0, v - 1));
            });
            final StringBuilder pathAndQuery = new StringBuilder(url.path());
            if (!Strings.isNullOrEmpty(url.parameterQuery(Format.encoded))) {
                pathAndQuery.append("?");
                pathAndQuery.append(url.parameterQuery(Format.encoded));
            }
            clientWebSocket.connect(url.port(), url.host(), pathAndQuery.toString())
                           .onFailure(v -> {
                               LOGGER.error("webSocket connection failed {}", url, v.getCause());
                           });
        }
    }

    /**
     * Picks the least-loaded port among {@code ports} using the
     * {@link #usedPorts} counter (ties broken in declaration order).
     *
     * @param ports candidate upstream ports (must be non-empty)
     * @return the selected port
     */
    private int loadBalancing(final List<Integer> ports) {
        final int port;
        if (ports.size() == 1) {
            port = ports.get(0);
        }
        else {
            int portToUse = ports.get(0);
            int previousCount = usedPorts.get(portToUse);
            for (int i = 1; i < ports.size(); i++) {
                final int _port = ports.get(i);
                final int count = usedPorts.get(_port);
                if (count < previousCount) {
                    previousCount = count;
                    portToUse = _port;
                }
            }
            port = portToUse;
        }
        return port;
    }

    /**
     * Increments the usage counter of an upstream port (called when a new
     * request is dispatched).
     *
     * @param port upstream port that just received a new request
     */
    protected void usePort(final int port) {
    	usedPorts.compute(port, (k, v) -> {
            return v == null ? 1 : v + 1;
        });
    }

    /**
     * Decrements the usage counter of an upstream port (called when a request
     * completes or fails).
     *
     * @param port upstream port whose request just finished
     */
    protected void releasePort(final int port) {
    	usedPorts.compute(port, (k, v) -> Math.max(0, v - 1));
    }

    /*
     *
     * URI
     *
     */
    /**
     * Public entry point used by the HTTP request handler: parses the absolute
     * URI of the server request into an {@link HttpUrl} and delegates to
     * {@link #resolve(HttpUrl)}. The result is always handed to the
     * {@code onResolve} listeners, even when resolution failed.
     *
     * @param httpServerRequest the inbound Vert.x request
     * @return the resolution outcome (may be {@code null} only when the URI
     *         itself was {@code null})
     */
    public ReverseProxyResult resolve(final HttpServerRequest httpServerRequest) {
        ReverseProxyResult result = null;
        HttpUrl fromUrl = null;
        try {
            if (httpServerRequest.absoluteURI() != null) {
                fromUrl = toHttpUrl(httpServerRequest.absoluteURI());
                result = resolve(fromUrl);
            }
        }
        catch (final Exception e) {
            LOGGER.error("reverse resolution path failed {}", httpServerRequest.absoluteURI());
            result = new ReverseProxyResult(fromUrl, null);
        }
        for (final Handler<ReverseProxyResult> onResolveAction : onResolveActions) {
            onResolveAction.handle(result);
        }
        return result;
    }

    /**
     * Walks back the inbound path one segment at a time, looking up the
     * longest-prefix rule whose key ({@code host/path}) matches. When no rule
     * matches, the fallback resolver (when configured) is consulted. A
     * non-{@code null} result is always returned (with {@code toUrl == null}
     * on a miss).
     *
     * @param fromUrl URL the request was received on
     * @return the resolution outcome
     */
    protected ReverseProxyResult resolve(final HttpUrl fromUrl) {
        final String host = fromUrl.host();
        final List<String> pathAsList = List.of(fromUrl.path().substring(1).split("/"));
        ReverseProxyResult result = null;
        for (int i =  pathAsList.size(); i >= 0; i--) {
            final String uriKey = host + "/" + PathUtil.formatPath(Path.of(String.join("/", pathAsList.subList(0, i))));
            final ReverseProxyRule reverseProxyRule = rules.get(uriKey);
            if (reverseProxyRule != null) {
                final HttpUrl toUrl = toHttpUrl(fromUrl, reverseProxyRule.to());
                result = new ReverseProxyRuleResult(fromUrl, toUrl, reverseProxyRule);
                break;
            }
        }
        if (result == null && resolver != null) {
            final HttpUrl toUrl = resolver.apply(fromUrl);
            if (toUrl != null) {
                result = new ReverseProxyResult(fromUrl, toUrl);
            }
        }
        if (result == null) {
            result = new ReverseProxyResult(fromUrl, null);
        }
        return result;
    }

    /**
     * Parses an absolute URI string into an {@link HttpUrl}.
     *
     * @param uri absolute URI to parse
     * @return the parsed URL
     * @throws ReverseProxyException when the URI is malformed
     */
    protected static HttpUrl toHttpUrl(final String uri) {
        try {
            final URL url = new URI(uri).toURL();
            return HttpUrl.builder(url.getHost())
                          .protocol(HttpProtocole.valueOf(url.getProtocol()))
                          .port(url.getPort())
                          .path(url.getPath())
                          .parameterQuery(url.getQuery())
                          .fragment(url.getRef())
                          .build();
        }
        catch (final Exception e) {
            throw new ReverseProxyException("reverse proxy traffic uri syntax wrong", e);
        }
    }

    /**
     * Builds the upstream URL from an inbound URL and a target description:
     * rewrites the path via {@link Url.To#apply(String)}, picks a port through
     * load balancing, and coerces the protocol to the {@code http(s)}/{@code ws(s)}
     * scheme matching the target's SSL flag.
     *
     * @param fromUrl inbound URL
     * @param to      target description from the matched rule
     * @return the upstream URL
     * @throws ReverseProxyException when path rewriting fails
     */
    private HttpUrl toHttpUrl(final HttpUrl fromUrl,
                              final Url.To to) {
        try {
            final String path = to.apply(fromUrl.path());
            final List<Integer> ports = new ArrayList<>(to.ports());
            final int port = loadBalancing(ports);
            HttpProtocole protocol = fromUrl.protocol();
            if (protocol.name().toLowerCase().startsWith("http")) {
                protocol = to.ssl() ? HttpProtocole.https : HttpProtocole.http;
            }
            else if (protocol.name().toLowerCase().startsWith("ws")) {
                protocol = to.ssl() ? HttpProtocole.ws : HttpProtocole.wss;
            }
            return HttpUrl.builder(to.host())
                          .protocol(protocol)
                          .port(port)
                          .path(path)
                          .parameterQuery(fromUrl.parameterQuery(Format.decoded))
                          .fragment(fromUrl.fragment(Format.decoded))
                          .build();
        }
        catch (final Exception e) {
            throw new ReverseProxyException("reverse proxy traffic uri failed", e);
        }
    }

    /*
     *
     * ROUTER
     *
     */
    /**
     * Wires the admin endpoints on {@link #adminRouter}:
     * <ul>
     *   <li>{@code POST /update/token} — rotate the admin token;</li>
     *   <li>{@code POST /put/rule} — add or replace a single rule;</li>
     *   <li>{@code POST /put/rules} — batch add/replace;</li>
     *   <li>{@code POST /remove/rule} — drop a rule by its {@link Url.From};</li>
     *   <li>{@code POST /remove/rules} — batch drop;</li>
     *   <li>{@code GET  /load/rules} — list every active rule.</li>
     * </ul>
     */
    private void initialzeRoute() {
        adminRouter.post("/update/token")
                   .blockingHandler(updateToken(), true);
        adminRouter.post("/put/rule")
                   .handler(BodyHandler.create())
                   .blockingHandler(putRule(), true);
        adminRouter.post("/put/rules")
                   .handler(BodyHandler.create())
                   .blockingHandler(putRules(), true);
        adminRouter.post("/remove/rule")
                   .handler(BodyHandler.create())
                   .blockingHandler(removeRule(), true);
        adminRouter.post("/remove/rules")
                   .handler(BodyHandler.create())
                   .blockingHandler(removeRules(), true);
        adminRouter.get("/load/rules")
                   .blockingHandler(rules(), false);
    }

    /**
     * Returns the Vert.x router exposing the admin endpoints.
     *
     * @return the Vert.x router exposing the admin endpoints
     */
    protected Router adminRouter() {
        return adminRouter;
    }

    /**
     * Builds the handler for {@code POST /update/token}: validates the current
     * token, reads the new one from the {@code New-Reserve-Proxy-Token} header
     * and swaps it in atomically.
     */
    private Handler<RoutingContext> updateToken() {
        return routingContext -> {
            Worker.singleThread.once(workerId, () -> {
                if (checkToken(routingContext)) {
                    final String newToken = routingContext.request()
                                                          .headers()
                                                          .get(ReverseProxyServer.adminNewTokenHeaderKey());
                    if (Strings.isNullOrEmpty(newToken)) {
                        StatusCode.badRequest(routingContext, "needs new token");
                    }
                    else {
                        this.adminToken = newToken;
                        StatusCode.ok(routingContext);
                    }
                }
            })
            .onFailure(e -> StatusCode.serverError(routingContext, e));
        };
    }

    /**
     * Builds the handler for {@code POST /put/rule}: reads a JSON-encoded
     * {@link ReverseProxyRule} from the body and adds/replaces it in the
     * rule table.
     */
    private Handler<RoutingContext> putRule() {
        return routingContext -> {
            Worker.singleThread.once(workerId, () -> {
                if (checkToken(routingContext)) {
                    final ReverseProxyRule reverseProxyRule = routingContext.body()
                                                                            .asJsonObject()
                                                                            .mapTo(ReverseProxyRule.class);
                    if (reverseProxyRule != null) {
                        rules.put(reverseProxyRule.from().id(), reverseProxyRule);
                        StatusCode.ok(routingContext);
                    }
                    else {
                        StatusCode.badRequest(routingContext, "needs rule");
                    }
                }
            })
            .onFailure(e -> {
                LOGGER.error("put rule failed", e);
                StatusCode.serverError(routingContext, e);
            });
        };
    }

    /**
     * Builds the handler for {@code POST /put/rules}: reads a JSON array of
     * rules and adds/replaces each.
     */
    private Handler<RoutingContext> putRules() {
        return routingContext -> {
            Worker.singleThread.once(workerId, () -> {
                if (checkToken(routingContext)) {
                    final JsonArray jsonArray = routingContext.body().asJsonArray();
                    if (jsonArray != null) {
                        for (final Object rule : jsonArray) {
                            if (rule instanceof JsonObject jsonObject) {
                                final ReverseProxyRule reverseProxyRule = jsonObject.mapTo(ReverseProxyRule.class);
                                rules.put(reverseProxyRule.from().id(), reverseProxyRule);
                            }
                        }
                        StatusCode.ok(routingContext);
                    }
                    else {
                        StatusCode.badRequest(routingContext, "needs rules");
                    }
                }
            })
            .onFailure(e -> {
                LOGGER.error("put rules failed", e);
                StatusCode.serverError(routingContext, e);
            });
        };
    }

    /**
     * Builds the handler for {@code POST /remove/rule}: drops the rule whose
     * {@link Url.From} matches the JSON body. Responds with {@code 404} when
     * no rule was registered for the given {@code From}.
     */
    private Handler<RoutingContext> removeRule() {
        return routingContext -> {
            Worker.singleThread.once(workerId, () -> {
                if (checkToken(routingContext)) {
                    final Url.From from = routingContext.body()
                                                        .asJsonObject()
                                                        .mapTo(Url.From.class);
                    if (rules.remove(from.id()) != null) {
                        StatusCode.ok(routingContext);
                    }
                    else {
                        StatusCode.notFound(routingContext);
                    }
                }
            })
            .onFailure(e -> {
                LOGGER.error("remove rule failed", e);
                StatusCode.serverError(routingContext, e);
            });
        };
    }

    /**
     * Builds the handler for {@code POST /remove/rules}: drops every rule
     * whose {@link Url.From} appears in the supplied JSON array.
     */
    private Handler<RoutingContext> removeRules() {
        return routingContext -> {
            Worker.singleThread.once(workerId, () -> {
                if (checkToken(routingContext)) {
                    final JsonArray jsonArray = routingContext.body().asJsonArray();
                    if (jsonArray != null) {
                        for (final Object rule : jsonArray) {
                            if (rule instanceof JsonObject jsonObject) {
                                final Url.From from = jsonObject.mapTo(Url.From.class);
                                rules.remove(from.id());
                            }
                        }
                        StatusCode.ok(routingContext);
                    }
                    else {
                        StatusCode.badRequest(routingContext, "needs rules");
                    }
                }
            })
            .onFailure(e -> {
                LOGGER.error("remove rules failed", e);
                StatusCode.serverError(routingContext, e);
            });
        };
    }

    /**
     * Builds the handler for {@code GET /load/rules}: emits every active rule
     * as a JSON array.
     */
    private Handler<RoutingContext> rules() {
        return routingContext -> {
            Worker.singleThread.once(workerId, () -> {
                if (checkToken(routingContext)) {
                    StatusCode.jsonArray(routingContext, rules.values());
                }
            })
            .onFailure(e -> StatusCode.serverError(routingContext, e));
        };
    }

    /**
     * Validates the admin token carried in the {@code Reserve-Proxy-Token}
     * request header. Sends {@code 403} with {@code "wrong token"} when the
     * token is missing or does not match.
     *
     * @param routingContext routing context whose request and response are inspected/written
     * @return {@code true} when the token matches; {@code false} when the
     *         response was already finalized with {@code 403}
     */
    private boolean checkToken(final RoutingContext routingContext) {
        boolean result = false;
        final String token = routingContext.request()
                                           .headers()
                                           .get(ReverseProxyServer.adminTokenHeaderKey());
        if (adminToken.equals(token)) {
            result = true;
        }
        else {
            StatusCode.forbidden(routingContext, "wrong token");
        }
        return result;
    }

    /*
     *
     *
     *
     */
    /**
     * Releases the underlying WebSocket engine. Called by
     * {@link ReverseProxyServer#stop()}.
     *
     * @return a future completing when the engine has finished closing
     */
    protected Future<Void> close() {
        return this.proxyWebSocketEngine.close();
    }

}
