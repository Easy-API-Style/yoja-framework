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

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;

/**
 * Server-side handle on a single WebSocket connection.
 * <p>
 * Wraps a Vert.x {@link ServerWebSocket} and exposes a Yoja-flavoured API to
 * inspect the connection (SSL, host, path, parsed parameters), close it, and
 * write text or binary frames. One {@code WebSocketServer} is created per
 * client connection and is held in {@link WebSocket#webSocketServers}.
 */
public class WebSocketServer {

    /** The wrapped Vert.x server-side WebSocket. */
    private final ServerWebSocket serverWebSocket;
    /** Parameters parsed from the handshake query string. */
    private final WebSocketParameter webSocketParameter;

    /**
     * Wraps the given Vert.x WebSocket and parses its query parameters.
     *
     * @param serverWebSocket the Vert.x WebSocket that just completed its handshake
     */
    protected WebSocketServer(final ServerWebSocket serverWebSocket) {
        super();
        this.serverWebSocket = serverWebSocket;
        this.webSocketParameter = WebSocketParameter.newInstance(serverWebSocket.query());
    }

    /**
     * Returns {@code true} when the WebSocket is running over TLS.
     *
     * @return {@code true} when the WebSocket is running over TLS
     */
    public boolean isSsl() {
        return serverWebSocket.isSsl();
    }

    /**
     * Returns {@code true} when the connection is still open.
     *
     * @return {@code true} when the connection is still open
     */
    public boolean isOpened() {
        return !serverWebSocket.isClosed();
    }

    /**
     * Returns the authority host of the underlying connection.
     *
     * @return the authority host of the underlying connection
     */
    public String host() {
        return serverWebSocket.authority().host();
    }

    /**
     * Returns the HTTP path of the upgrade request.
     *
     * @return the HTTP path of the upgrade request
     */
    public String path() {
        return serverWebSocket.path();
    }

    /**
     * Returns the parsed query parameters from the handshake.
     *
     * @return the parsed query parameters from the handshake
     */
    public WebSocketParameter parameterQuery() {
        return webSocketParameter;
    }

    /** Pauses inbound frame delivery until {@code resume()} is called by Vert.x. */
    public void pause() {
        serverWebSocket.pause();
    }

    /**
     * Closes the connection without a status code.
     *
     * @return a future completing when the connection is closed
     */
    public Future<Void> close() {
        return serverWebSocket.close();
    }

    /**
     * Closes the connection with the supplied WebSocket status code.
     *
     * @param statusCode WebSocket close status code
     * @return a future completing when the connection is closed
     */
    public Future<Void> close(final short statusCode) {
        return serverWebSocket.close(statusCode);
    }

    /**
     * Closes the connection with status code and human-readable reason.
     *
     * @param statusCode WebSocket close status code
     * @param reason     human-readable close reason
     * @return a future completing when the connection is closed
     */
    public Future<Void> close(final short statusCode, final String reason) {
        return serverWebSocket.close(statusCode, reason);
    }

    /*
     *
     * PROTECTED
     *
     */
    /**
     * Writes a text frame; reserved for {@link WebSocket#send(String)}.
     *
     * @param text text payload
     * @return a future completing when the frame is written
     */
    protected Future<Void> writeTextMessage(final String text) {
        return serverWebSocket.writeTextMessage(text);
    }

    /**
     * Writes a binary frame; reserved for {@link WebSocket#send(byte[])}.
     *
     * @param text binary payload
     * @return a future completing when the frame is written
     */
    protected Future<Void> writeBinaryMessage(final byte[] text) {
        return serverWebSocket.writeBinaryMessage(Buffer.buffer(text));
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(WebSocketServer.class.getSimpleName());
        result.append(" [ssl=");
        result.append(isSsl());
        result.append(", host=");
        result.append(host());
        result.append(", path=");
        result.append(path());
        result.append(", opened=");
        result.append(isOpened());
        result.append("]");
        return result.toString();
    }


}
