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

import java.util.Set;
import java.util.TreeSet;

import com.easygoingapi.yoja.core.http.ContentType;
import com.easygoingapi.yoja.core.http.HttpCookie;
import com.easygoingapi.yoja.core.util.JavaReflectUtil;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.CookieJar;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Common implementation shared by {@link HttpResponse} (used by user handlers
 * to write the response) and {@link HttpResponseEvent} (used by response hooks
 * to mutate the outgoing payload).
 * <p>
 * Provides accessors and mutators for headers, cookies and status code, plus a
 * dispatch helper, {@link #sendBodyWithContentType(Object)}, that auto-selects
 * a {@code Content-Type} based on the body's runtime type and writes it.
 * <p>
 * The class reaches into Vert.x' {@link HttpServerResponse} internals through
 * reflection ({@code CookieJar}) to offer richer cookie queries than the
 * public API exposes.
 */
public abstract class AbstractHttpResponse {

    /** Underlying Vert.x Web routing context. */
    private final RoutingContext routingContext;
    /** Convenience handle to the response side of the routing context. */
    private final HttpServerResponse httpServerResponse;
    /** Cookie jar pulled by reflection from the Vert.x response. */
    private final CookieJar cookieJar;

    /**
     * Constructs a response wrapper around the given routing context.
     *
     * @param routingContext routing context that owns the response being built
     */
    protected AbstractHttpResponse(final RoutingContext routingContext) {
        super();
        this.routingContext = routingContext;
        this.httpServerResponse = routingContext.response();
        this.cookieJar = JavaReflectUtil.getFieldValue(this.httpServerResponse, "cookies");
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
        return !httpServerResponse.headers().isEmpty();
    }

    /**
     * Returns {@code true} when a response header with the given name is already set.
     *
     * @param name header name (may be {@code null})
     * @return {@code true} when a header with this name is already set
     */
    public boolean hasHeader(final String name) {
        boolean result = false;
        if (name != null) {
            result = httpServerResponse.headers()
                                       .contains(name);
        }
        return result;
    }

    /**
     * Returns a sorted snapshot of response header names.
     *
     * @return a sorted snapshot of response header names
     */
    public Set<String> headerNames() {
        return new TreeSet<>(httpServerResponse.headers()
                                               .names());
    }

    /**
     * Returns the first response header value for the given name, or {@code null} when missing.
     *
     * @param name header name (may be {@code null})
     * @return the first header value for that name, or {@code null} when
     *         missing
     */
    public String header(final String name) {
        if (name != null) {
            return httpServerResponse.headers().get(name);
        }
        return null;
    }

    /**
     * Sets a response header value. Silently ignored when {@code name} is
     * {@code null}.
     *
     * @param name  header name
     * @param value header value
     */
    public void putHeader(final String name,
                          final String value) {
        if (name != null) {
            httpServerResponse.putHeader(name, value);
        }
    }

    /*
     *
     * COOKIE
     *
     */
    /**
     * Converts a Vert.x {@link Cookie} to a Yoja {@link HttpCookie}.
     *
     * @param cookie source Vert.x cookie
     * @return an equivalent {@link HttpCookie}
     */
    private static HttpCookie from(final Cookie cookie) {
        return HttpCookie.builder(cookie.getName(),
                                  cookie.getValue())
                         .domain(cookie.getDomain())
                         .path(cookie.getPath())
                         .maxAge(cookie.getMaxAge())
                         .httpOnly(cookie.isHttpOnly())
                         .secure(cookie.isSecure())
                         .sameSite(cookie.getSameSite())
                         .build();
    }

    /**
     * Converts a Yoja {@link HttpCookie} to a Vert.x {@link Cookie}.
     *
     * @param httpCookie source Yoja cookie
     * @return an equivalent Vert.x {@link Cookie}
     */
    private static Cookie from(final HttpCookie httpCookie) {
        final Cookie cookie = Cookie.cookie(httpCookie.getName(),
                                            httpCookie.getValue());
        cookie.setDomain(httpCookie.getDomain());
        cookie.setPath(httpCookie.getPath());
        cookie.setMaxAge(httpCookie.getMaxAge());
        cookie.setHttpOnly(httpCookie.isHttpOnly());
        cookie.setSecure(httpCookie.isSecure());
        cookie.setSameSite(httpCookie.getSameSite());
        return cookie;
    }

    /**
     * Returns {@code true} when any cookie is staged on the response.
     *
     * @return {@code true} when any cookie is staged on the response
     */
    public boolean hasCookie() {
        return !cookies().isEmpty();
    }

    /**
     * Returns {@code true} when at least one cookie with the given name is staged.
     *
     * @param name cookie name
     * @return {@code true} when at least one cookie with that name is staged
     */
    public boolean hasCookie(final String name) {
        return !cookies(name).isEmpty();
    }

    /**
     * Returns every staged cookie whose name matches, in natural order.
     *
     * @param name cookie name (may be {@code null})
     * @return every cookie whose name matches, ordered by natural order; empty
     *         set when none or when {@code name} is {@code null}
     */
    public Set<HttpCookie> cookies(final String name) {
        if (name != null) {
            final Set<HttpCookie> result = new TreeSet<>();
            cookieJar.forEach(c -> {
                if (name.equals(c.getName())) {
                    result.add(from((Cookie) c));
                }
            });
            return result;
        }
        return Set.of();
    }

    /**
     * Looks up a single cookie by its full identity (name, domain, path).
     *
     * @param name   cookie name (may be {@code null})
     * @param domain cookie domain (may be {@code null})
     * @param path   cookie path (may be {@code null})
     * @return the matching cookie, or {@code null} when not found
     */
    public HttpCookie cookie(final String name,
                             final String domain,
                             final String path) {
        if (name != null) {
            final Cookie cookie = (Cookie) cookieJar.get(name, domain, path);
            return cookie != null ? from(cookie) : null;
        }
        return null;
    }

    /**
     * Returns a snapshot of every cookie currently staged on the response.
     *
     * @return a snapshot of every cookie currently staged on the response
     */
    public Set<HttpCookie> cookies() {
        final Set<HttpCookie> result = new TreeSet<>();
        cookieJar.forEach(c -> result.add(from((Cookie) c)));
        return result;
    }

    /**
     * Adds a cookie to the response. When the cookie value is {@code null} the
     * matching cookie is removed instead (which the browser translates into a
     * deletion).
     *
     * @param httpCookie cookie to add (may be {@code null}, in which case the
     *                   call is a no-op)
     */
    public void addCookie(final HttpCookie httpCookie) {
        if (httpCookie != null) {
            if (httpCookie.getValue() != null) {
                httpServerResponse.addCookie(from(httpCookie));
            }
            else {
                removeCookie(httpCookie.getName(),
                             httpCookie.getDomain(),
                             httpCookie.getPath());
            }
        }
    }

    /**
     * Removes every cookie matching the given name (any domain/path).
     *
     * @param name cookie name (no-op when {@code null})
     */
    public void removeCookies(final String name) {
        if (name != null) {
            httpServerResponse.removeCookies(name);
        }
    }

    /**
     * Removes a specific cookie identified by name, domain and path.
     *
     * @param name   cookie name (no-op when {@code null})
     * @param domain cookie domain
     * @param path   cookie path
     */
    public void removeCookie(final String name,
                             final String domain,
                             final String path) {
        if (name != null) {
            synchronized (cookieJar) {
                httpServerResponse.removeCookie(name, domain, path);
            }
        }
    }

    /*
     *
     * STATUS
     *
     */
    /**
     * Returns the current HTTP response status code.
     *
     * @return the current HTTP response status code
     */
    public int statusCode() {
        return httpServerResponse.getStatusCode();
    }

    /**
     * Overrides the response status code (default is 200).
     *
     * @param statusCode the status code to send back
     */
    public void statusCode(final int statusCode) {
        httpServerResponse.setStatusCode(statusCode);
    }

    /*
     *
     * PROTECTED
     *
     */
    /**
     * Returns the wrapped Vert.x Web routing context.
     *
     * @return the wrapped Vert.x Web routing context
     */
    protected RoutingContext context() {
        return routingContext;
    }

    /**
     * Returns the underlying Vert.x server request.
     *
     * @return the underlying Vert.x server request
     */
    protected HttpServerRequest httpServerRequest() {
        return routingContext.request();
    }

    /**
     * Returns the underlying Vert.x server response.
     *
     * @return the underlying Vert.x server response
     */
    protected HttpServerResponse httpServerResponse() {
        return routingContext.response();
    }

    /**
     * Writes the given body to the response, auto-selecting a default
     * {@code Content-Type} when none has been set:
     * {@code application/json} for {@link JsonObject} / {@link JsonArray},
     * {@code text/plain} for a {@link String}. {@code byte[]} payloads carry
     * no implicit content type. A default {@code Cache-Control: no-store} is
     * also set when missing. A {@code null} body ends the response without a
     * payload.
     *
     * @param body the payload (any of {@code JsonObject}, {@code JsonArray},
     *             {@code String}, {@code byte[]} or {@code null})
     * @return a future completing when the bytes have been flushed
     */
    protected Future<Void> sendBodyWithContentType(final Object body) {
		final MultiMap headers = httpServerResponse.headers();
		if (body != null && !headers.contains("Cache-Control")) {
			httpServerResponse.putHeader("Cache-Control", "no-store");
		}
		final Future<Void> result;
		if (body instanceof JsonObject jsonObject) {
			if (!headers.contains(ContentType.key)) {
				httpServerResponse.putHeader(ContentType.key,
						                     ContentType.jsonObject.value());
			}
			result = httpServerResponse.end(jsonObject.encode());
		}
		else if (body instanceof JsonArray jsonArray) {
			if (!headers.contains(ContentType.key)) {
				httpServerResponse.putHeader(ContentType.key,
						                     ContentType.jsonArray.value());
			}
			result = httpServerResponse.end(jsonArray.encode());
		}
		else if (body instanceof String string) {
			if (!headers.contains(ContentType.key)) {
				httpServerResponse.putHeader(ContentType.key,
						                     ContentType.text.value());
			}
			result = httpServerResponse.end(string);
		}
		else if (body instanceof byte[] binary) {
			result = httpServerResponse.end(Buffer.buffer(binary));
		}
		else {
			result = httpServerResponse.end();
		}
		return result;
	}

}
