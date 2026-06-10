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
import java.util.TreeMap;

import com.easygoingapi.yoja.core.http.HttpEncoding;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.core.http.HttpParameter;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Read-only view over an inbound HTTP request, exposed to user handlers via
 * {@link HttpRouting#request()}.
 * <p>
 * The class wraps Vert.x' {@link HttpServerRequest} and Web's
 * {@link RoutingContext} and offers convenience accessors for the URL,
 * headers, query parameters, cookies and body, including typed body
 * conversions to JSON or a POJO.
 * <p>
 * Cookies are snapshotted at construction time into a {@link TreeMap} so the
 * accessors do not iterate the underlying request on each call.
 */
public class HttpRequest {

    /** The underlying Vert.x Web routing context (kept for body and query helpers). */
    private final RoutingContext routingContext;
    /** Convenience handle to the routing context's request. */
    private final HttpServerRequest httpServerRequest;
    /** Cookie snapshot built once at construction time, keyed by cookie name. */
    private final Map<String, String> httpCookies = new TreeMap<>();

    /**
     * Builds a request view around the given routing context and snapshots its
     * cookies. Constructed internally by {@link HttpRouting}.
     *
     * @param routingContext the Vert.x Web routing context
     */
	protected HttpRequest(final RoutingContext routingContext) {
        super();
        this.routingContext = routingContext;
        this.httpServerRequest = routingContext.request();

        for (final Cookie cookie : httpServerRequest.cookies()) {
            httpCookies.put(cookie.getName(), cookie.getValue());
        }
    }

    /**
     * Returns the HTTP protocol version negotiated for this request.
     *
     * @return the HTTP protocol version negotiated for this request
     */
    public HttpVersion version() {
        return httpServerRequest.version();
    }

    /**
     * Returns the HTTP method as a Yoja {@link HttpMethod}.
     *
     * @return the HTTP method (GET, POST, ...) as a Yoja {@link HttpMethod}
     */
    public HttpMethod method() {
        return HttpMethod.valueOf(httpServerRequest.method().name());
    }

    /**
     * Returns {@code true} if the request was received over TLS.
     *
     * @return {@code true} if the request was received over TLS
     */
    public boolean ssl() {
        return httpServerRequest.isSSL();
    }

    /**
     * Returns the {@code Host} header value (authority host).
     *
     * @return the {@code Host} header value (authority host)
     */
    public String host() {
        return httpServerRequest.authority().host();
    }

    /**
     * Returns the URL-decoded request path (without the query string).
     *
     * @return the URL-decoded request path (without the query string)
     */
    public String path() {
        return HttpEncoding.urlDecode(httpServerRequest.path());
    }

    /*
     *
     * HEADER
     *
     */
    /**
     * Returns {@code true} if at least one request header is present.
     *
     * @return {@code true} if at least one request header is present
     */
    public boolean hasHeader() {
        return !httpServerRequest.headers().isEmpty();
    }

    /**
     * Returns {@code true} if a request header with the given name exists (case-insensitive).
     *
     * @param name header name (case-insensitive per HTTP); may be {@code null}
     * @return {@code true} if a header with that name exists, {@code false}
     *         when {@code name} is {@code null} or absent
     */
    public boolean hasHeader(final String name) {
        if (name != null) {
            return httpServerRequest.headers().contains(name);
        }
        return false;
    }

    /**
     * Returns the set of header names attached to the request.
     *
     * @return the set of header names attached to the request
     */
    public Set<String> headerNames() {
        return httpServerRequest.headers().names();
    }

    /**
     * Returns the first value for the given request header, or {@code null} when missing.
     *
     * @param name header name (may be {@code null})
     * @return the first value for the header, or {@code null} when missing or
     *         when {@code name} is {@code null}
     */
    public String header(final String name) {
        if (name != null) {
            return httpServerRequest.headers().get(name);
        }
        return null;
    }

    /*
     *
     * PARAMETER
     *
     */
    /**
     * Returns {@code true} when any query/form parameter is present.
     *
     * @return {@code true} when any query/form parameter is present
     */
    public boolean hasParameter() {
        return !httpServerRequest.params().isEmpty();
    }

    /**
     * Returns {@code true} if at least one parameter with the given name exists.
     *
     * @param name parameter name (may be {@code null})
     * @return {@code true} if at least one parameter with that name exists
     */
    public boolean hasParameter(final String name) {
        if (name != null) {
            return httpServerRequest.params().contains(name);
        }
        return false;
    }

    /**
     * Returns the set of parameter names supplied with the request.
     *
     * @return the set of parameter names supplied with the request
     */
    public Set<String> parameterNames() {
        return httpServerRequest.params().names();
    }

    /**
     * Returns all parameters as a flat list of name/value entries, preserving duplicates.
     *
     * @return all parameters as a flat list of name/value entries, preserving
     *         duplicates
     */
    public List<HttpParameter.Entry> parameters() {
        return httpServerRequest.params()
                                .entries()
                                .stream()
                                .map(v -> new HttpParameter.Entry(v.getKey(), v.getValue()))
                                .toList();
    }

    /**
     * Returns the first parameter value for the given name, or {@code null} when absent.
     *
     * @param name parameter name (may be {@code null})
     * @return the first parameter value, or {@code null} when absent
     */
    public String firstParameter(final String name) {
        if (name != null) {
            return routingContext.request().getParam(name);
        }
        return null;
    }

    /**
     * Returns every value matching the given parameter name, or an empty list when none.
     *
     * @param name parameter name (may be {@code null})
     * @return every value matching the name, or an empty list when none
     */
    public List<String> parameters(final String name) {
        if (name != null) {
            return routingContext.queryParam(name);
        }
        return List.of();
    }

    /*
     *
     * COOKIE
     *
     */
    /**
     * Returns {@code true} when the request carries at least one cookie.
     *
     * @return {@code true} when the request carries at least one cookie
     */
    public boolean hasCookie() {
        return !httpCookies.isEmpty();
    }

    /**
     * Returns {@code true} when a cookie with the given name is present.
     *
     * @param name cookie name
     * @return {@code true} when a cookie with that name is present
     */
    public boolean hasCookie(final String name) {
        return cookie(name) != null;
    }

    /**
     * Returns the value of the cookie with the given name, or {@code null} when missing.
     *
     * @param name cookie name (may be {@code null})
     * @return the cookie value, or {@code null} when missing
     */
    public String cookie(final String name) {
        if (name != null) {
            return httpCookies.get(name);
        }
        return null;
    }

    /**
     * Returns a defensive copy of the cookie map keyed by cookie name.
     *
     * @return a defensive copy of the cookie map keyed by cookie name
     */
    public Map<String, String> cookies() {
        return new TreeMap<>(httpCookies);
    }

    /*
     *
     * BODY
     *
     */
    /**
     * Returns {@code true} when the request body buffer is empty.
     *
     * @return {@code true} when the request body buffer is empty
     */
    public boolean isEmptyBody() {
        return routingContext.body().isEmpty();
    }

    /**
     * Returns the body decoded as a {@link JsonObject}.
     *
     * @return the body decoded as a {@link JsonObject}
     */
    public JsonObject bodyAsJsonObject() {
        return routingContext.body().asJsonObject();
    }

    /**
     * Returns the body decoded as a {@link JsonArray}.
     *
     * @return the body decoded as a {@link JsonArray}
     */
    public JsonArray bodyAsJsonArray() {
        return routingContext.body().asJsonArray();
    }

    /**
     * Returns the body decoded as a UTF-8 string.
     *
     * @return the body decoded as a UTF-8 string
     */
    public String bodyAsText() {
        return routingContext.body().asString();
    }

    /**
     * Returns the body as raw bytes.
     *
     * @return the body as raw bytes
     */
    public byte[] bodyAsByteArray() {
        return routingContext.body().buffer().getBytes();
    }

    /**
     * Maps the JSON body to a POJO of the supplied class via Vert.x' Jackson
     * binding.
     *
     * @param clazz target type (may be {@code null}, which returns {@code null})
     * @param <C>   target type parameter
     * @return the decoded POJO, or {@code null} when {@code clazz} is null
     */
    public <C> C body(final Class<C> clazz) {
        if (clazz != null) {
            return routingContext.body()
                                 .asPojo(clazz);
        }
        return null;
    }

    /*
     *
     * protected
     *
     */
    /**
     * Returns the absolute request URI; used by {@link #toString()}.
     *
     * @return the absolute request URI
     */
    protected String uri() {
        return httpServerRequest.absoluteURI();
    }

    /**
     * Returns the wrapped Vert.x Web routing context.
     *
     * @return the wrapped Vert.x Web routing context
     */
    protected RoutingContext context() {
        return routingContext;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpRequest.class.getSimpleName());
        result.append(" [version=");
        result.append(version());
        result.append(", method=");
        result.append(method());
        result.append(", uri=");
        result.append(uri());
        result.append("]");
        return result.toString();
    }

}