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
package com.easygoingapi.yoja.http.client;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * Yoja-flavoured WebSocket client wrapping a connected Vert.x {@link WebSocket}.
 * <p>
 * Built and connected through {@link #builder(WebSocketEngine, String)},
 * which dials the supplied {@link WebSocketEngine} and wraps the resulting
 * Vert.x socket into a {@code WebSocketClient}. Once connected, handlers
 * can be attached through {@link #onTextMessage}, {@link #onBinaryMessage}
 * and {@link #onClose}; outgoing frames are written through {@link #send(String)}
 * / {@link #send(byte[])}.
 */
public class WebSocketClient {

	/**
	 * Event delivered to {@code onClose} handlers when the connection is closed.
	 *
	 * @param path path the client was connected on
	 */
	public final record CloseEvent(String path) {}
	/**
	 * Event delivered to {@code onTextMessage} handlers for each incoming text frame.
	 *
	 * @param path    path the client is connected on
	 * @param message text payload of the frame
	 */
	public final record TextMessageEvent(String path, String message) {}
	/**
	 * Event delivered to {@code onBinaryMessage} handlers for each incoming binary frame.
	 *
	 * @param path    path the client is connected on
	 * @param message binary payload of the frame
	 */
	public final record BinaryMessageEvent(String path, byte[] message) {}

	/** Path the client is connected on. */
	private final String path;
    /** Wrapped Vert.x WebSocket. */
    private final WebSocket webSocket;

    /** Close handlers (thread-safe). */
    private final Set<Handler<CloseEvent>> closeHandlers = ConcurrentHashMap.newKeySet();
    /** Text-frame handlers (thread-safe). */
    private final Set<Handler<TextMessageEvent>> textMessageHandlers = ConcurrentHashMap.newKeySet();
    /** Binary-frame handlers (thread-safe). */
    private final Set<Handler<BinaryMessageEvent>> binaryMessageHandlers = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new client wrapping the given Vert.x WebSocket, connected on {@code path}.
     *
     * @param path      path the client is connected on
     * @param webSocket connected Vert.x WebSocket
     */
    public WebSocketClient(final String path,
                           final WebSocket webSocket) {
        super();
        this.path = path;
        this.webSocket = webSocket;
        this.initialize();
    }

    /**
     * Returns the path the client is connected on.
     *
     * @return the path the client is connected on
     */
    public String getPath() {
		return path;
	}

    /**
     * Wires the underlying Vert.x socket's text/binary/close handlers to fan
     * out events into the local handler sets.
     */
	private void initialize() {
    	 webSocket.textMessageHandler(value -> {
         	for (Handler<TextMessageEvent> handler : textMessageHandlers) {
         		handler.handle(new TextMessageEvent(path, value));
 			}
         });
    	 webSocket.binaryMessageHandler(value -> {
          	for (Handler<BinaryMessageEvent> handler : binaryMessageHandlers) {
          		handler.handle(new BinaryMessageEvent(path, value.getBytes()));
  			}
          });
    	 webSocket.closeHandler(h -> {
          	for (Handler<CloseEvent> handler : closeHandlers) {
          		handler.handle(new CloseEvent(path));
  			}
          });
    }

    /**
     * Sends a binary frame.
     *
     * @param message payload to send
     * @return a future completing when the frame has been written
     */
    public Future<Void> send(final byte[] message) {
        return webSocket.writeBinaryMessage(Buffer.buffer(message));
    }

    /**
     * Sends a final text frame.
     *
     * @param message payload to send
     * @return a future completing when the frame has been written
     */
    public Future<Void> send(final String message) {
        return webSocket.writeFinalTextFrame(message);
    }

    /**
     * Registers a handler invoked on every incoming text frame.
     *
     * @param handler handler to register
     */
    public void onTextMessage(final Handler<TextMessageEvent> handler) {
    	textMessageHandlers.add(handler);
    }

    /**
     * Registers a handler invoked on every incoming binary frame.
     *
     * @param handler handler to register
     */
    public void onBinaryMessage(final Handler<BinaryMessageEvent> handler) {
    	binaryMessageHandlers.add(handler);
    }

    /**
     * Registers a handler invoked when the connection is closed.
     *
     * @param handler handler to register
     */
    public void onClose(final Handler<CloseEvent> handler) {
    	closeHandlers.add(handler);
    }

    /**
     * Closes the WebSocket connection.
     *
     * @return a future completing when the close has been acknowledged
     */
    public Future<Void> close() {
        return webSocket.close();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(WebSocketClient.class.getSimpleName());
        result.append(" [");
        result.append("path=");
        result.append(path);
        result.append(", closeHandlers=");
        result.append(closeHandlers.size());
        result.append(", textMessageHandlers=");
        result.append(textMessageHandlers.size());
        result.append(", binaryMessageHandlers=");
        result.append(binaryMessageHandlers.size());
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder for a {@link WebSocketClient} connecting on the given path.
     *
     * @param webSocketEngine engine the client will borrow its Vert.x socket from
     * @param path            path to connect to
     * @return a new builder
     */
    public static Builder builder(final WebSocketEngine webSocketEngine,
                                  final String path) {
        return new Builder(webSocketEngine, path);
    }

    /**
     * Fluent builder for {@link WebSocketClient}: collects the connection
     * parameters then {@link #connect()} actually dials.
     */
    public static class Builder {

        /** Engine to dial through. */
        private final WebSocketEngine webSocketEngine;
        /** Path to connect to. */
        private final String path;

        /** Target port; falls back to the engine default when {@code null}. */
        private Integer port;
        /** Target host; falls back to the engine default when {@code null}. */
        private String host;
        /** Whether to dial over TLS. */
        private boolean ssl = true;
        /** Optional connect timeout. */
        private Duration timeout;

        /**
         * @param webSocketEngine engine the client will dial through
         * @param path            path to connect to
         */
        private Builder(final WebSocketEngine webSocketEngine,
                        final String path) {
            super();
            this.webSocketEngine = webSocketEngine;
            this.path = path;
        }

        /**
         * Sets the target port (engine default applies when omitted).
         *
         * @param port target port
         * @return this builder
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the target host (engine default applies when omitted).
         *
         * @param host target host
         * @return this builder
         */
        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        /**
         * Toggles TLS for the connection.
         *
         * @param ssl {@code true} to dial over TLS
         * @return this builder
         */
        public Builder ssl(final boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        /**
         * Sets the connect timeout.
         *
         * @param timeout connect timeout
         * @return this builder
         */
        public Builder timeout(final Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Dials the engine and wraps the resulting Vert.x socket into a
         * {@link WebSocketClient}.
         *
         * @return a future resolving to the connected client
         */
        public Future<WebSocketClient> connect() {
            final WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions();
            if (port != null) {
                webSocketConnectOptions.setPort(port);
            }
            if (host != null) {
                webSocketConnectOptions.setHost(host);
            }
            if(timeout != null) {
                webSocketConnectOptions.setTimeout(timeout.toMillis());
            }
            webSocketConnectOptions.setURI(path);
            webSocketConnectOptions.setSsl(ssl);
            return webSocketEngine.webSocketClient()
                                  .connect(webSocketConnectOptions)
                                  .map(v -> new WebSocketClient(path, v));
        }

    }

}
