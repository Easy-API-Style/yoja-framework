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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.easygoingapi.yoja.core.http.HttpCookie;
import com.google.common.base.Objects;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Yoja-flavoured view over a Vert.x web client {@link io.vertx.ext.web.client.HttpResponse}.
 * <p>
 * Wraps the underlying Vert.x response and converts every {@code Set-Cookie}
 * header into a Yoja {@link HttpCookie} at construction time (the decoded set
 * is then queryable via {@link #cookies(String)} and {@link #cookie(String, String, String)}).
 * Body accessors return the most common shapes ({@link JsonObject},
 * {@link JsonArray}, {@link String}, {@code byte[]}) or map the body to a POJO
 * via {@link #body(Class)}.
 */
public class HttpResponse {

    /** Wrapped Vert.x web-client response. */
    private final io.vertx.ext.web.client.HttpResponse<Buffer> httpResponse;
    /** Cookies decoded from the response's {@code Set-Cookie} headers. */
    private final Set<HttpCookie> httpCookies = new HashSet<>();

    /**
     * Wraps the given Vert.x response and eagerly decodes its cookies.
     *
     * @param httpResponse Vert.x response to wrap; cookies are eagerly decoded
     */
    protected HttpResponse(final io.vertx.ext.web.client.HttpResponse<Buffer> httpResponse) {
        super();
        this.httpResponse = httpResponse;
        for (final String encodedCookie : httpResponse.cookies()) {
            final Cookie cookie = ClientCookieDecoder.STRICT.decode(encodedCookie);
            final HttpCookie httpCookie = HttpCookie.builder(cookie.name(), cookie.value())
                                                    .path(cookie.path())
                                                    .domain(cookie.domain())
                                                    .maxAge(cookie.maxAge())
                                                    .httpOnly(cookie.isHttpOnly())
                                                    .secure(cookie.isSecure())
                                                    .build();
            httpCookies.add(httpCookie);
        }
    }

    /**
     * Returns the HTTP protocol version of the response.
     *
     * @return the HTTP protocol version of the response
     */
    public HttpVersion version() {
        return httpResponse.version();
    }

    /*
     *
     * STATUS
     *
     */
    /**
     * Returns the response status code.
     *
     * @return the response status code
     */
    public int statusCode() {
        return httpResponse.statusCode();
    }

    /**
     * Returns the response status reason phrase.
     *
     * @return the response status reason phrase
     */
    public String statusMessage() {
        return httpResponse.statusMessage();
    }

    /*
     *
     * HEADER
     *
     */
    /**
     * Returns {@code true} when at least one response header is set.
     *
     * @return {@code true} when at least one response header is set
     */
    public boolean hasHeader() {
        return !httpResponse.headers().isEmpty();
    }

    /**
     * Returns {@code true} when a header with the given name is set (case-insensitive).
     *
     * @param name header name (case-insensitive per HTTP)
     * @return {@code true} when a header with that name is set
     */
    public boolean hasHeader(final String name) {
        return httpResponse.headers().contains(name);
    }

    /**
     * Returns the set of response header names.
     *
     * @return the set of response header names
     */
    public Set<String> headerNames() {
        return httpResponse.headers().names();
    }

    /**
     * Returns the first header value for the given name, or {@code null} when missing.
     *
     * @param name header name
     * @return the first header value for that name, or {@code null} when missing
     */
    public String header(final String name) {
        return httpResponse.headers().get(name);
    }

    /*
     *
     * COOKIE
     *
     */
    /**
     * Returns {@code true} when at least one cookie was decoded.
     *
     * @return {@code true} when at least one cookie was decoded
     */
    public boolean hasCookie() {
        return !httpCookies.isEmpty();
    }

    /**
     * Returns {@code true} when at least one decoded cookie matches the given name.
     *
     * @param name cookie name
     * @return {@code true} when at least one decoded cookie matches that name
     */
    public boolean hasCookie(final String name) {
        return !cookies(name).isEmpty();
    }

    /**
     * Returns every decoded cookie matching the given name, in natural order.
     *
     * @param name cookie name (no-op when {@code null})
     * @return every decoded cookie matching that name, in natural order
     */
    public Set<HttpCookie> cookies(final String name) {
        final Set<HttpCookie> result = new TreeSet<>();
        if (name != null) {
            for (final HttpCookie httpCookie : httpCookies) {
                if (name.equals(httpCookie.getName())) {
                    result.add(httpCookie);
                }
            }
        }
        return result;
    }

    /**
     * Looks up a cookie by its full identity (name, domain, path).
     *
     * @param name   cookie name
     * @param domain cookie domain (may be {@code null})
     * @param path   cookie path (may be {@code null})
     * @return the matching cookie, or {@code null} when not found
     */
    public HttpCookie cookie(final String name,
                             final String domain,
                             final String path) {
        HttpCookie result = null;
        for (final HttpCookie httpCookie : httpCookies) {
            if (name.equals(httpCookie.getName())
                    && Objects.equal(domain, httpCookie.getDomain())
                    && Objects.equal(path, httpCookie.getPath())) {
                result = httpCookie;
                break;
            }
        }
        return result;
    }

    /**
     * Returns a snapshot of every decoded cookie.
     *
     * @return a snapshot of every decoded cookie
     */
    public Set<HttpCookie> cookies() {
        return new TreeSet<>(httpCookies);
    }

    /*
     *
     * BODY
     *
     */
    /**
     * Returns the body parsed as a {@link JsonObject}.
     *
     * @return the body parsed as a {@link JsonObject}
     */
    public JsonObject bodyAsJsonObject() {
        return new JsonObject(httpResponse.bodyAsBuffer());
    }

    /**
     * Returns the body parsed as a {@link JsonArray}.
     *
     * @return the body parsed as a {@link JsonArray}
     */
    public JsonArray bodyAsJsonArray() {
        return httpResponse.bodyAsJsonArray();
    }

    /**
     * Returns the body decoded as text.
     *
     * @return the body decoded as text
     */
    public String bodyAsText() {
        return httpResponse.bodyAsString();
    }

    /**
     * Returns the body as raw bytes.
     *
     * @return the body as raw bytes
     */
    public byte[] bodyAsBinary() {
        return httpResponse.bodyAsBuffer().getBytes();
    }

    /**
     * Maps the JSON body to a POJO of the given class.
     *
     * @param clazz target type
     * @param <C>   target type parameter
     * @return the decoded POJO
     */
    public <C> C body(final Class<C> clazz) {
        return bodyAsJsonObject().mapTo(clazz);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpResponse.class.getSimpleName());
        result.append(" [statusCode=");
        result.append(statusCode());
        result.append("]");
        return result.toString();
    }

}
