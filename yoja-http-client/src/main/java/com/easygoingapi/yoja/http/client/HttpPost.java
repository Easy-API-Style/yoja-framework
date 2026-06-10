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

import java.util.Map;

import com.easygoingapi.yoja.core.http.HttpHeader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * POST request: a path, optional headers and cookies (inherited from
 * {@link HttpRequest}), plus a body whose runtime type drives the
 * {@code Content-Type} chosen by {@link HttpClient#send(HttpPost)}.
 * <p>
 * Supported body types are {@link JsonObject}, {@link JsonArray},
 * {@link String}, {@code byte[]} and {@code null} (empty body). Build through
 * {@link #builder(String)} for a full fluent API; the static {@code of}
 * factories are shortcuts for the common cases.
 */
public class HttpPost extends HttpRequest {

    /** Body to send; the concrete runtime type drives the Content-Type. */
    private final Object body;

    /** Private — instances are produced through {@link #builder(String)} or the {@code of} factories. */
    private HttpPost(final String path,
                     final Map<String, String> httpCookies,
                     final HttpHeader httpHeader,
                     final Object body) {
        super(path, httpCookies, httpHeader);
        this.body = body;
    }

    /**
     * Returns the body as a {@link JsonObject} (unchecked cast).
     *
     * @return the body as a {@link JsonObject} (unchecked cast)
     */
    public JsonObject bodyAsJsonObject() {
        return (JsonObject) body;
    }

    /**
     * Returns the body as a {@link JsonArray} (unchecked cast).
     *
     * @return the body as a {@link JsonArray} (unchecked cast)
     */
    public JsonArray bodyAsJsonArray() {
        return (JsonArray) body;
    }

    /**
     * Returns the body as a {@link String} (unchecked cast).
     *
     * @return the body as a {@link String} (unchecked cast)
     */
    public String bodyAsText() {
        return (String) body;
    }

    /**
     * Returns the body as a {@code byte[]} (unchecked cast).
     *
     * @return the body as a {@code byte[]} (unchecked cast)
     */
    public byte[] bodyAsBinary() {
        return (byte[]) body;
    }

    /**
     * Maps the JSON body to a POJO of the given class.
     *
     * @param clazz target type
     * @param <C>   target type parameter
     * @return the decoded POJO
     */
    public <C> C body(final Class<C> clazz) {
        return ((JsonObject) body).mapTo(clazz);
    }

    /**
     * Returns the raw body value (client-internal access).
     *
     * @return the raw body value (client-internal access)
     */
    protected Object body() {
        return body;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpPost.class.getSimpleName());
        result.append(" [");
        result.append("path=");
        result.append(path);
        result.append(", ");
        result.append("cookies=");
        result.append(httpCookies.size());
        result.append(", ");
        result.append("header=");
        result.append(headerSize());
        if (body != null) {
            result.append("body=");
            result.append(body);
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
     * Returns a new builder for a POST request to the given path.
     *
     * @param path request path
     * @return a new builder
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(final String path) {
        return new Builder(path);
    }

    /** Fluent builder for {@link HttpPost}. */
    public static class Builder extends HttpRequest.Builder<Builder> {

        /** Body being assembled. */
        private Object body;

        /**
         * @param path request path
         */
        private Builder(final String path) {
            super(path);
        }

        /**
         * Sets a {@link JsonObject} body.
         *
         * @param body payload
         * @return this builder
         */
        public Builder body(final JsonObject body) {
            this.body = body;
            return this;
        }

        /**
         * Sets a {@link JsonArray} body.
         *
         * @param body payload
         * @return this builder
         */
        public Builder body(final JsonArray body) {
            this.body = body;
            return this;
        }

        /**
         * Sets a textual body.
         *
         * @param body payload
         * @return this builder
         */
        public Builder body(final String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets a binary body.
         *
         * @param body payload
         * @return this builder
         */
        public Builder body(final byte[] body) {
            this.body = body;
            return this;
        }

        /**
         * Returns the assembled {@link HttpPost}.
         *
         * @return the assembled {@link HttpPost}
         */
        public HttpPost build() {
            return new HttpPost(path, httpCookies, httpHeader, body);
        }

    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Convenience factory for a POST with no body.
     *
     * @param path request path
     * @return the assembled POST request
     */
    public static HttpPost of(final String path) {
        return HttpPost.builder(path).build();
    }

    /**
     * Convenience factory for a POST with a {@link JsonObject} body.
     *
     * @param path request path
     * @param body payload
     * @return the assembled POST request
     */
    public static HttpPost of(final String path,
                              final JsonObject body) {
        return HttpPost.builder(path)
                       .body(body)
                       .build();
    }

    /**
     * Convenience factory for a POST with a {@link JsonArray} body.
     *
     * @param path request path
     * @param body payload
     * @return the assembled POST request
     */
    public static HttpPost of(final String path,
                              final JsonArray body) {
        return HttpPost.builder(path)
                       .body(body)
                       .build();
    }

    /**
     * Convenience factory for a POST with a textual body.
     *
     * @param path request path
     * @param body payload
     * @return the assembled POST request
     */
    public static HttpPost of(final String path,
                              final String body) {
        return HttpPost.builder(path)
                       .body(body)
                       .build();
    }

    /**
     * Convenience factory for a POST with a binary body.
     *
     * @param path request path
     * @param body payload
     * @return the assembled POST request
     */
    public static HttpPost of(final String path,
                              final byte[] body) {
        return HttpPost.builder(path)
                       .body(body)
                       .build();
    }

}
