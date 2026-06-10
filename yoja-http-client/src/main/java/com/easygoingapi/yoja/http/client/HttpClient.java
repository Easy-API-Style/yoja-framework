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

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.easygoingapi.yoja.core.http.ContentType;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.http.HttpParameter;

import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

/**
 * Per-endpoint HTTP client: a small façade that dials a single
 * {@code host:port} (optionally over TLS, with an optional timeout) through
 * the Vert.x {@link WebClient} owned by an {@link HttpEngine}.
 * <p>
 * Build instances through {@link #builder(HttpEngine)}; the builder falls
 * back to the engine's default host/port when none is supplied. Each
 * {@link #send(HttpGet)} / {@link #send(HttpPost)} produces a fresh Vert.x
 * request, copies the headers and cookies of the Yoja request onto it, and
 * — for POST — picks the right {@code Content-Type} from the body's runtime
 * type:
 * <ul>
 *   <li>{@link JsonObject} → {@code application/json};</li>
 *   <li>{@link JsonArray} → {@code application/json};</li>
 *   <li>{@link String} → {@code text/plain};</li>
 *   <li>{@code byte[]} → no implicit content type;</li>
 *   <li>{@code null} → no body.</li>
 * </ul>
 * Any I/O failure is wrapped into an {@link HttpException}.
 */
public class HttpClient {

//    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    /** Target host. */
    private String host;
    /** Target port. */
    private Integer port;
    /** Whether to dial over TLS. */
    private boolean ssl = true;
    /** Optional per-request timeout. */
    private Duration timeout;
    /** Wrapped Vert.x web client (borrowed from the engine). */
    private WebClient webClient;

    /** Private — use {@link #builder(HttpEngine)}. */
    private HttpClient() {
        super();
    }

    /**
     * Returns the target host.
     *
     * @return the target host
     */
    public String host() {
        return host;
    }

    /**
     * Returns the target port.
     *
     * @return the target port
     */
    public Integer port() {
        return port;
    }

    /**
     * Returns {@code true} when requests are dialed over TLS.
     *
     * @return {@code true} when requests are dialed over TLS
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * Returns the per-request timeout, or {@code null} for none.
     *
     * @return the per-request timeout, or {@code null} for none
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Sends a GET request and returns a future resolving to the response.
     *
     * @param httpGet GET request to send
     * @return future resolving to the response
     * @throws HttpException when the request cannot be built
     */
    public Future<HttpResponse> send(final HttpGet httpGet) {
        try {
            final String uri = toUri(httpGet.path(), httpGet.httpParameter());
            final HttpRequest<Buffer> request = webClient.get(port, host, uri.toString())
                                                         .ssl(ssl);
            putHeaders(request, httpGet);
            if (timeout != null) {
                request.timeout(timeout.toMillis());
            }
            for (final Entry<String, String> entry : httpGet.headers().entrySet()) {
                request.putHeader(entry.getKey(), entry.getValue());
            }
            return request.send()
                          .map(v -> new HttpResponse(v));
        }
        catch (final Exception e) {
            throw new HttpException("GET failed, path=" + httpGet.path(), e);
        }
    }

    /**
     * Sends a POST request, auto-selecting the {@code Content-Type} from the
     * body's runtime type, and returns a future resolving to the response.
     *
     * @param httpPost POST request to send
     * @return future resolving to the response
     * @throws HttpException when the request cannot be built
     */
    public Future<HttpResponse> send(final HttpPost httpPost) {
        try {
            final Future<HttpResponse> result;

            final String uri = toUri(httpPost.path(), null);
            final HttpRequest<Buffer> request = webClient.post(port, host, uri.toString())
                                                         .ssl(ssl);
            putHeaders(request, httpPost);
            if (timeout != null) {
                request.timeout(timeout.toMillis());
            }
            for (final Entry<String, String> entry : httpPost.headers().entrySet()) {
                request.putHeader(entry.getKey(), entry.getValue());
            }

            final Object body = httpPost.body();
            if (body instanceof JsonObject jsonObject) {
                request.headers()
                       .set(ContentType.key,
                             ContentType.jsonObject.value());
                result = request.sendJsonObject(jsonObject)
                                .map(v -> new HttpResponse(v));
            }
            else if (body instanceof JsonArray jsonArray) {
                request.headers()
                       .set(ContentType.key,
                             ContentType.jsonArray.value());
                result = request.sendBuffer(Buffer.buffer(jsonArray.encode()))
                                .map(v -> new HttpResponse(v));
            }
            else if (body instanceof String text) {
                request.headers()
                       .set(ContentType.key,
                             ContentType.text.value());
                result = request.sendBuffer(Buffer.buffer((text)))
                                .map(v -> new HttpResponse(v));
            }
            else if (body instanceof byte[] binary) {
                result = request.sendBuffer(Buffer.buffer(binary))
                                .map(v -> new HttpResponse(v));
            }
            else {
                result = request.send()
                                .map(v -> new HttpResponse(v));
            }
            return result;
        }
        catch (final Exception e) {
            throw new HttpException("POST failed, path=" + httpPost.path(), e);
        }
    }

    /*
     *
     *
     *
     */
    /**
     * Copies headers from a Yoja {@link com.easygoingapi.yoja.http.client.HttpRequest}
     * onto a Vert.x {@link HttpRequest}, and encodes the cookie map into a
     * single {@code cookie} header.
     *
     * @param request     Vert.x request to mutate
     * @param httpRequest Yoja request to read from
     */
    private static void putHeaders(final HttpRequest<Buffer> request,
                                   final com.easygoingapi.yoja.http.client.HttpRequest httpRequest) {
        for (final Entry<String, String> entry : httpRequest.httpHeader.values().entrySet()) {
            request.putHeader(entry.getKey(), entry.getValue());
        }
        if (httpRequest.hasCookie()) {
            final List<String> cookies = new ArrayList<>();
            for (final Entry<String, String> httpCookie : httpRequest.cookies().entrySet()) {
                cookies.add(ServerCookieEncoder.STRICT.encode(httpCookie.getKey(), httpCookie.getValue()));
            }
            request.putHeader("cookie", String.join(";", cookies));
        }
    }

    /**
     * Assembles the request URI by appending an encoded query string (when
     * any) to the path.
     *
     * @param path          path component
     * @param httpParameter optional query parameters
     * @return the assembled URI
     * @throws UnsupportedEncodingException never thrown today; retained to
     *         keep the signature flexible if URL-encoding is reintroduced
     */
    private static String toUri(final String path,
                                final HttpParameter httpParameter)
                                 throws UnsupportedEncodingException {
        final StringBuilder uri = new StringBuilder();
        uri.append(path);
//        uri.append(URLEncoder.encode(path, "UTF-8"));
        if (httpParameter != null) {
            uri.append("?");
            uri.append(httpParameter.parameterQuery(Format.encoded));
        }
        return uri.toString();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpClient.class.getSimpleName());
        result.append(" [");
        result.append("host=");
        result.append(host);
        result.append(", ");
        result.append("port=");
        result.append(port);
        result.append(", ");
        result.append("ssl=");
        result.append(ssl);
        if (timeout != null) {
            result.append(", ");
            result.append("timeout=");
            result.append(timeout);
        }
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder backed by the given engine.
     *
     * @param httpEngine the engine owning the underlying Vert.x web client
     * @return a new builder
     */
    public static Builder builder(final HttpEngine httpEngine) {
        return new Builder(httpEngine);
    }

    /**
     * Fluent builder for {@link HttpClient}. Defaults the host and port to
     * those configured on the supplied {@link HttpEngine} when none is set
     * explicitly.
     */
    public static class Builder {

        /** Engine the client will borrow its Vert.x {@link WebClient} from. */
        private final HttpEngine httpEngine;
        /** Client being assembled. */
        private final HttpClient httpClient = new HttpClient();

        /**
         * @param httpEngine engine the client will borrow its Vert.x client from
         */
        private Builder(final HttpEngine httpEngine) {
            super();
            this.httpEngine = httpEngine;
            httpClient.webClient = httpEngine.webClient();
        }

        /**
         * Sets the target port.
         *
         * @param port target TCP port
         * @return this builder
         */
        public Builder port(final int port) {
            httpClient.port = port;
            return this;
        }

        /**
         * Sets the target host.
         *
         * @param host target host name
         * @return this builder
         */
        public Builder host(final String host) {
            httpClient.host = host;
            return this;
        }

        /**
         * Toggles TLS for every request sent through this client.
         *
         * @param ssl {@code true} to dial over TLS
         * @return this builder
         */
        public Builder ssl(final boolean ssl) {
            httpClient.ssl = ssl;
            return this;
        }

        /**
         * Sets a per-request timeout.
         *
         * @param timeout request timeout
         * @return this builder
         */
        public Builder timeout(final Duration timeout) {
            httpClient.timeout = timeout;
            return this;
        }

        /**
         * Fills in host/port defaults from the engine when missing and
         * returns the assembled client.
         *
         * @return the assembled {@link HttpClient}
         */
        public HttpClient build() {
            if (httpClient.port == null) {
                httpClient.port = httpEngine.defaultPort();
            }
            if (httpClient.host == null) {
                httpClient.host = httpEngine.defaultHost();
            }
            return httpClient;
        }

    }

}
