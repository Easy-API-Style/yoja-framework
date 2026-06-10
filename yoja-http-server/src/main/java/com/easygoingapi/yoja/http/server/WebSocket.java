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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.ServerWebSocketHandshake;

/**
 * A logical WebSocket endpoint mounted at a single URL path, fanning out
 * events to a set of subscribers.
 * <p>
 * Each {@code WebSocket} aggregates every client connection that completed
 * the handshake on its path (stored as {@link WebSocketServer} instances) and
 * exposes:
 * <ul>
 *   <li>handler registration via {@link #onOpen}, {@link #onClose},
 *       {@link #onTextMessage}, {@link #onBinaryMessage};</li>
 *   <li>broadcast helpers ({@link #send(String)}, {@link #send(byte[])}) that
 *       write to every still-open client;</li>
 *   <li>connection-level lifecycle controls ({@link #close()},
 *       {@link #close(short)}, {@link #close(short, String)}).</li>
 * </ul>
 * Acceptance of a new connection is controlled by the optional {@code accept}
 * predicate supplied at construction time: it receives the parsed query
 * parameters and returns {@code true} to accept the handshake, {@code false}
 * to reject it.
 * <p>
 * Natural ordering is by path; equality is also based on the path, so the
 * same path cannot be registered twice in a {@link WebSocketService}.
 */
public class WebSocket implements Comparable<WebSocket> {

    /** Natural-order comparator (path-based). */
    private static final Comparator<WebSocket> COMPARATOR =
        Comparator.comparing(WebSocket::getPath);

    /**
     * Event delivered to {@link On#close} handlers.
     *
     * @param path               endpoint path that was closed
     * @param webSocketParameter parameters parsed from the original handshake
     * @param statusCode         WebSocket close status code, or {@code null}
     * @param reason             human-readable close reason, or {@code null}
     */
    public final record CloseEvent(String path, WebSocketParameter webSocketParameter, Short statusCode, String reason) {}
    /**
     * Event delivered to {@link On#open} handlers when a client connects.
     *
     * @param path               endpoint path the client connected to
     * @param webSocketParameter parameters parsed from the handshake
     */
    public final record OpenEvent(String path, WebSocketParameter webSocketParameter) {}
    /**
     * Event delivered to {@link On#textMessage} handlers.
     *
     * @param path               endpoint path the frame was received on
     * @param webSocketParameter parameters parsed from the handshake
     * @param message            text payload of the frame
     */
    public final record TextMessageEvent(String path, WebSocketParameter webSocketParameter, String message) {}
    /**
     * Event delivered to {@link On#binaryMessage} handlers.
     *
     * @param path               endpoint path the frame was received on
     * @param webSocketParameter parameters parsed from the handshake
     * @param message            binary payload of the frame
     */
    public final record BinaryMessageEvent(String path, WebSocketParameter webSocketParameter, byte[] message) {}

    /** Live set of accepted connections, pruned on {@link #clean()}. */
    private final Set<WebSocketServer> webSocketServers = ConcurrentHashMap.newKeySet();

    /** Handlers notified whenever a new connection is opened. */
    private final Set<Handler<OpenEvent>> openHandlers = ConcurrentHashMap.newKeySet();
    /** Handlers notified whenever a connection is closed. */
    private final Set<Handler<CloseEvent>> closeHandlers = ConcurrentHashMap.newKeySet();
    /** Handlers notified for every incoming text frame. */
    private final Set<Handler<TextMessageEvent>> textMessageHandlers = ConcurrentHashMap.newKeySet();
    /** Handlers notified for every incoming binary frame. */
    private final Set<Handler<BinaryMessageEvent>> binaryMessageHandlers = ConcurrentHashMap.newKeySet();

    /** Identifies the four kinds of event-handler sets, used by {@link #clear(On...)}. */
    public static enum On {
        /** Identifies the open-handler set. */
        open,
        /** Identifies the close-handler set. */
        close,
        /** Identifies the text-message-handler set. */
        textMessage,
        /** Identifies the binary-message-handler set. */
        binaryMessage
    }

    /** Path the endpoint is mounted at. */
    private final String path;
    /** Optional handshake-time acceptance predicate. */
    private final Function<WebSocketParameter, Boolean> accept;

    /**
     * Builds an endpoint that accepts every connection.
     *
     * @param path path the endpoint is mounted at (must begin with {@code /})
     */
    public WebSocket(final String path) {
        this(path, null);
    }

    /**
     * Constructs an endpoint at the given path with an optional acceptance predicate.
     *
     * @param path   path the endpoint is mounted at (must begin with {@code /})
     * @param accept optional predicate consulted during the handshake; when it
     *               returns {@code false}, the connection is rejected
     * @throws HttpServerException when {@code path} is null or does not start with {@code /}
     */
    public WebSocket(final String path,
                     final Function<WebSocketParameter, Boolean> accept) {
        super();
        if (path == null || !HttpRouter.formatPath(path).startsWith("/")) {
            throw new HttpServerException("WebSocket path must begin with '/'");
        }
        this.path = path;
        this.accept = accept;
    }

    /**
     * Returns the path the endpoint is mounted at.
     *
     * @return the path the endpoint is mounted at
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns {@code true} when at least one client connection is still open.
     *
     * @return {@code true} when at least one client connection is still open
     *         (closed sockets are pruned as a side-effect)
     */
    public boolean isOpened() {
        synchronized (webSocketServers) {
            clean();
            return !webSocketServers.isEmpty();
        }
    }

    /**
     * Closes every connection on this endpoint.
     *
     * @return a composite future completing when all per-client closes complete
     */
    public CompositeFuture close() {
        final List<Future<?>> futures = new ArrayList<>();
        for (final WebSocketServer webSocketServer : webSocketServers) {
            futures.add(webSocketServer.close());
        }
        clean();
        return Future.all(futures);
    }

    /**
     * Closes every connection with the given WebSocket status code.
     *
     * @param statusCode WebSocket close status code
     * @return a composite future completing when all per-client closes complete
     */
    public CompositeFuture close(final short statusCode) {
        final List<Future<?>> futures = new ArrayList<>();
        for (final WebSocketServer webSocketServer : webSocketServers) {
            futures.add(webSocketServer.close(statusCode));
        }
        clean();
        return Future.all(futures);
    }

    /**
     * Closes every connection with the given status code and reason.
     *
     * @param statusCode WebSocket close status code
     * @param reason     human-readable close reason
     * @return a composite future completing when all per-client closes complete
     */
    public CompositeFuture close(final short statusCode, final String reason) {
        final List<Future<?>> futures = new ArrayList<>();
        for (final WebSocketServer webSocketServer : webSocketServers) {
            futures.add(webSocketServer.close(statusCode, reason));
        }
        clean();
        return Future.all(futures);
    }

    /** Drops connections that the underlying socket already reports as closed. */
    private void clean() {
        final Iterator<WebSocketServer> iterator = webSocketServers.iterator();
        while (iterator.hasNext()) {
            final WebSocketServer webSocketServer = iterator.next();
            if (!webSocketServer.isOpened()) {
                iterator.remove();
            }
        }
    }

    /*
     *
     * ON
     *
     */
    /**
     * Registers a handler invoked when a client connects.
     *
     * @param handler handler to register
     */
    public void onOpen(final Handler<OpenEvent> handler) {
        openHandlers.add(handler);
    }

    /**
     * Registers a handler invoked when a connection closes.
     *
     * @param handler handler to register
     */
    public void onClose(final Handler<CloseEvent> handler) {
        closeHandlers.add(handler);
    }

    /**
     * Registers a handler invoked for every incoming text frame.
     *
     * @param handler handler to register
     */
    public void onTextMessage(final Handler<TextMessageEvent> handler) {
        textMessageHandlers.add(handler);
    }

    /**
     * Registers a handler invoked for every incoming binary frame.
     *
     * @param handler handler to register
     */
    public void onBinaryMessage(final Handler<BinaryMessageEvent> handler) {
        binaryMessageHandlers.add(handler);
    }

    /*
     *
     * CLEAR
     *
     */
    /**
     * Clears the handler set for each {@link On} kind supplied. Unknown kinds
     * are silently ignored.
     *
     * @param onActions the handler sets to clear
     */
    public void clear(final On... onActions) {
        for (On onAction : onActions) {
            if (On.open == onAction) {
                openHandlers.clear();
            }
            else if (On.close == onAction) {
                closeHandlers.clear();
            }
            else if (On.textMessage == onAction) {
                textMessageHandlers.clear();
            }
            else if (On.binaryMessage == onAction) {
                binaryMessageHandlers.clear();
            }
        }
    }

    /*
     *
     * SEND
     *
     */
    /**
     * Broadcasts a binary frame to every still-open client.
     *
     * @param message payload to broadcast
     * @return a composite future completing when every per-client write completes
     */
    public CompositeFuture send(final byte[] message) {
        final List<Future<?>> futures = new ArrayList<>();
        for (final WebSocketServer webSocketServer : webSocketServers) {
            if (webSocketServer.isOpened()) {
                futures.add(webSocketServer.writeBinaryMessage(message));
            }
        }
        return Future.all(futures);
    }

    /**
     * Broadcasts a text frame to every still-open client.
     *
     * @param message payload to broadcast
     * @return a composite future completing when every per-client write completes
     */
    public CompositeFuture send(final String message) {
        final List<Future<?>> futures = new ArrayList<>();
        for (final WebSocketServer webSocketServer : webSocketServers) {
            if (webSocketServer.isOpened()) {
                futures.add(webSocketServer.writeTextMessage(message));
            }
        }
        return Future.all(futures);
    }

    /**
     * Decides whether to accept the handshake based on the optional
     * {@code accept} predicate. Invoked from {@link WebSocketService}.
     *
     * @param serverWebSocketHandshake the incoming handshake request
     */
    protected void shakeHand(final ServerWebSocketHandshake serverWebSocketHandshake) {
        boolean doAccept = false;
        if (accept == null) {
            doAccept = true;
        }
        else {
            final WebSocketParameter webSocketParameter = WebSocketParameter.newInstance(serverWebSocketHandshake.query());
            doAccept = accept.apply(webSocketParameter);
        }
        if (doAccept) {
            serverWebSocketHandshake.accept();
        }
        else {
            serverWebSocketHandshake.reject();
        }
    }

    /**
     * Registers a freshly accepted Vert.x WebSocket as a new
     * {@link WebSocketServer}, wires its close/text/binary handlers and fires
     * the {@link OpenEvent} on registered listeners.
     *
     * @param serverWebSocket the newly accepted Vert.x WebSocket
     */
    protected void open(final ServerWebSocket serverWebSocket) {
        final WebSocketServer webSocketServer = new WebSocketServer(serverWebSocket);
        webSocketServers.add(webSocketServer);
        final WebSocketParameter webSocketParameter = webSocketServer.parameterQuery();
        // open event
        for (final Handler<OpenEvent> openHandler : openHandlers) {
            openHandler.handle(new OpenEvent(path, webSocketParameter));
        }
        // close event
        serverWebSocket.closeHandler(handler -> {
            for (final Handler<CloseEvent> closeHandler : closeHandlers) {
                closeHandler.handle(new CloseEvent(path,
                		                           webSocketParameter,
                                                   serverWebSocket.closeStatusCode(),
                                                   serverWebSocket.closeReason()));
            }
        });
        // message event
        serverWebSocket.textMessageHandler(message -> {
            for (final Handler<TextMessageEvent> jsonArrayHandler : textMessageHandlers) {
                jsonArrayHandler.handle(new TextMessageEvent(path, webSocketParameter, message));
            }
        });
        serverWebSocket.binaryMessageHandler(message -> {
            for (final Handler<BinaryMessageEvent> binaryHandler : binaryMessageHandlers) {
                binaryHandler.handle(new BinaryMessageEvent(path, webSocketParameter, message.getBytes()));
            }
        });
    }

    @Override
    public int compareTo(final WebSocket webSocket) {
        return COMPARATOR.compare(this, webSocket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final WebSocket other = (WebSocket) obj;
        return Objects.equals(path, other.path);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(WebSocket.class.getSimpleName());
        result.append(" [path=");
        result.append(path);
        result.append(", openHandlers=");
        result.append(openHandlers.size());
        result.append(", closeHandlers=");
        result.append(closeHandlers.size());
        result.append(", textMessageHandlers=");
        result.append(textMessageHandlers.size());
        result.append(", binaryMessageHandlers=");
        result.append(binaryMessageHandlers.size());
        result.append("]");
        return result.toString();
    }

}
