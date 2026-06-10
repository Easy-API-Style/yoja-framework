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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.ServerWebSocketHandshake;

/**
 * Registry of {@link WebSocket} endpoints exposed by an {@link HttpServer}.
 * <p>
 * Routes incoming handshakes and accepted connections to the proper
 * {@link WebSocket} by matching the request path against the registered
 * mounts (an optional context path supplied by the router is stripped before
 * matching). Endpoints not declared in this service yield an immediate
 * handshake rejection.
 */
public class WebSocketService {

    /** Tag used by {@link #getWebSockets(Status)} to filter endpoints. */
    public static enum Status {
        /** Matches endpoints that have at least one live connection. */
        open,
        /** Matches endpoints that have no live connections. */
        close
    }

    /** Registered endpoints keyed by mount path. */
    protected final Map<String, WebSocket> webSockets = new ConcurrentHashMap<>();

    /** Package-visible default constructor. */
    public WebSocketService() {
        super();
    }

    /**
     * Returns {@code true} when an endpoint exists for the given path.
     *
     * @param path the mount path to test
     * @return {@code true} when an endpoint exists for the given path
     */
    public boolean hasWebSocket(final String path) {
        return webSockets.containsKey(path);
    }

    /**
     * Returns the endpoint registered at {@code path}, or {@code null}.
     *
     * @param path the mount path to look up
     * @return the endpoint registered at {@code path}, or {@code null}
     */
    public WebSocket getWebSocket(final String path) {
        return webSockets.get(path);
    }

    /**
     * Returns the paths of endpoints filtered by liveness.
     *
     * @param status whether to return paths whose endpoint has at least one
     *               live connection ({@link Status#open}) or none
     *               ({@link Status#close})
     * @return the matching paths as a sorted set
     */
    public Set<String> getWebSocketPaths(final Status status) {
        return new TreeSet<>(getWebSockets(status).stream()
                                                  .map(WebSocket::getPath)
                                                  .collect(Collectors.toSet()));
    }

    /**
     * Returns the registered endpoints filtered by liveness.
     *
     * @param status filter (see {@link #getWebSocketPaths(Status)})
     * @return the matching endpoints in natural order (by path)
     */
    public Set<WebSocket> getWebSockets(final Status status) {
        final Set<WebSocket> result = new TreeSet<>();
        for (final WebSocket webSocket : webSockets.values()) {
            if (Status.open == status) {
                if (webSocket.isOpened()) {
                    result.add(webSocket);
                }
            }
            else if (Status.close == status) {
                if (!webSocket.isOpened()) {
                    result.add(webSocket);
                }
            }
        }
        return result;
    }

    /**
     * Returns every registered path as a sorted set.
     *
     * @return every registered path as a sorted set
     */
    public Set<String> getWebSocketPaths() {
        return new TreeSet<>(webSockets.keySet());
    }

    /**
     * Registers {@code webSocket}; if an endpoint is already mounted at the
     * same path, the existing one wins (the call is a no-op for the new entry).
     *
     * @param webSocket the endpoint to register
     * @return the endpoint actually installed at the path
     */
    public WebSocket add(final WebSocket webSocket) {
        return webSockets.compute(webSocket.getPath(), (k, v) -> {
             if (v != null) {
                 return v;
             }
             return webSocket;
        });
    }

    /**
     * Unregisters the endpoint at the given path and closes any live
     * connections it holds.
     *
     * @param path the mount path to remove
     * @return a composite future completing when every per-client close completes
     */
    public CompositeFuture remove(final String path) {
        final WebSocket webSocket = webSockets.remove(path);
        final CompositeFuture result;
        if (webSocket != null) {
            result = webSocket.close();
        }
        else {
            result = Future.all(List.of());
        }
        return result;
    }

    /**
     * Looks up the endpoint matching {@code path} once the router's
     * context-path prefix has been stripped (if any).
     *
     * @return the matching endpoint, or {@code null} when none
     */
    private WebSocket get(final String contextPath,
                          final String path) {
        final WebSocket result;
        if (contextPath != null) {
            if (path.startsWith(contextPath)) {
                final String _path = path.substring(contextPath.length());
                result = webSockets.get(HttpRouter.formatPath(_path));
            }
            else {
                result = null;
            }
        }
        else {
            result = webSockets.get(path);
        }
        return result;
    }

    /**
     * Dispatches an incoming WebSocket handshake to the matching endpoint, or
     * rejects it when none is registered for the requested path.
     *
     * @param serverWebSocketHandshake the incoming handshake request
     * @param contextPath              router-level context path to strip, or {@code null}
     */
    protected void shakeHand(final ServerWebSocketHandshake serverWebSocketHandshake,
                             final String contextPath) {
        final String path = serverWebSocketHandshake.path();
        final WebSocket webSocket = get(contextPath, path);
        if (webSocket != null) {
            webSocket.shakeHand(serverWebSocketHandshake);
        }
        else {
            serverWebSocketHandshake.reject();
        }
    }

    /**
     * Hands an accepted connection to the matching endpoint so it can install
     * its frame handlers and fire its {@link WebSocket.OpenEvent}.
     *
     * @param serverWebSocket the newly accepted Vert.x WebSocket
     * @param contextPath     router-level context path to strip, or {@code null}
     */
    protected void open(final ServerWebSocket serverWebSocket,
                        final String contextPath) {
        final String path = serverWebSocket.path();
        final WebSocket webSocket = get(contextPath, path);
        if (webSocket != null) {
            webSocket.open(serverWebSocket);
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(WebSocketService.class.getSimpleName());
        result.append(" [webSockets=");
        result.append(webSockets.size());
        result.append("]");
        return result.toString();
    }

}
