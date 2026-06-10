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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.easygoingapi.yoja.core.http.HttpHeader;
import com.easygoingapi.yoja.core.util.PathUtil;

/**
 * Abstract base shared by {@link HttpGet} and {@link HttpPost}: holds the
 * normalized request path, the cookie map and the {@link HttpHeader} value
 * object.
 * <p>
 * Subclasses add their method-specific fields (query parameters for GET, a
 * body for POST). The nested {@link Builder} is generic on the concrete
 * builder type so subclasses inherit {@link Builder#putHeader} and
 * {@link Builder#putCookie} while still returning their own builder type.
 */
public abstract class HttpRequest {

    /** Normalized request path (forward-slash form). */
    protected final String path;
    /** Cookie name → value map; never {@code null}. */
    protected final Map<String, String> httpCookies;
    /** Header value object; never {@code null}. */
    protected final HttpHeader httpHeader;

    /**
     * Constructs a request for the given path, cookies and headers.
     *
     * @param path        request path (normalized via {@link PathUtil#formatPath(String)})
     * @param httpCookies cookies to attach
     * @param httpHeader  headers to attach
     */
    protected HttpRequest(final String path,
                          final Map<String, String> httpCookies,
                          final HttpHeader httpHeader) {
        super();
        this.path = PathUtil.formatPath(path);
        this.httpCookies = httpCookies;
        this.httpHeader = httpHeader;
    }

    /**
     * Returns the normalized request path.
     *
     * @return the normalized request path
     */
    public String path() {
        return path;
    }

    /*
     *
     * COOKIE
     *
     */
    /**
     * Returns {@code true} when at least one cookie is attached.
     *
     * @return {@code true} when at least one cookie is attached
     */
    public boolean hasCookie() {
        return !httpCookies.isEmpty();
    }

    /**
     * Returns a sorted snapshot of the cookie map.
     *
     * @return a sorted snapshot of the cookie map
     */
    public Map<String, String> cookies() {
        return new TreeMap<>(httpCookies);
    }

    /**
     * Returns the value of the cookie with the given name, or {@code null} when missing.
     *
     * @param name cookie name
     * @return the cookie value, or {@code null} when missing
     */
    public String cookie(final String name) {
        return httpCookies.get(name);
    }

    /*
     *
     * HEADER
     *
     */
    /**
     * Returns {@code true} when at least one header is set.
     *
     * @return {@code true} when at least one header is set
     */
    public boolean hasHeader() {
        return !httpHeader.isEmpty();
    }

    /**
     * Returns the number of headers set.
     *
     * @return the number of headers set
     */
    public int headerSize() {
        return httpHeader.size();
    }

    /**
     * Returns a snapshot of headers as a name→value map.
     *
     * @return a snapshot of headers as a name→value map
     */
    public Map<String, String> headers() {
        return httpHeader.values();
    }

    /**
     * Returns {@code true} when a header with the given name is set.
     *
     * @param name header name
     * @return {@code true} when a header with this name is set
     */
    public boolean hasHeader(final String name) {
        return httpHeader.has(name);
    }

    /**
     * Returns the set of configured header names.
     *
     * @return the set of configured header names
     */
    public Set<String> headerNames() {
        return httpHeader.names();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Factory used by subclasses to obtain a builder for their concrete type.
     *
     * @param path request path
     * @param <A>  concrete builder type
     * @return a new builder
     */
    protected static <A> Builder<A> builder(final String path) {
        return new Builder<>(path);
    }

    /**
     * Generic builder backing {@link HttpGet.Builder} and {@link HttpPost.Builder}.
     * <p>
     * The type parameter {@code A} is the concrete builder subclass, used as
     * the return type of {@link #putHeader} and {@link #putCookie} so chained
     * calls stay typed against the subclass.
     *
     * @param <A> concrete builder type
     */
    public static class Builder<A> {

        /** Path of the request being assembled. */
        protected final String path;
        /** Headers being accumulated. */
        protected final HttpHeader httpHeader = new HttpHeader();
        /** Cookies being accumulated. */
        protected final Map<String, String> httpCookies = new HashMap<>();

        /**
         * Constructs a builder for a request on the given path.
         *
         * @param path request path
         */
        protected Builder(final String path) {
            super();
            this.path = path;
        }

        /**
         * Sets a header; {@code null} value removes the header instead.
         * No-op when {@code name} is {@code null}.
         *
         * @param name  header name
         * @param value header value (or {@code null} to remove)
         * @return this builder
         */
        @SuppressWarnings("unchecked")
		public A putHeader(final String name,
                           final String value) {
            if (name != null) {
                if (value != null) {
                    httpHeader.put(name, value);
                }
                else {
                    httpHeader.remove(name);
                }
            }
            return (A) this;
        }

        /**
         * Sets a cookie; {@code null} value removes the cookie instead.
         * No-op when {@code name} is {@code null}.
         *
         * @param name  cookie name
         * @param value cookie value (or {@code null} to remove)
         * @return this builder
         */
        @SuppressWarnings("unchecked")
		public A putCookie(final String name,
                           final String value) {
            if (name != null) {
                if (value != null) {
                    httpCookies.put(name, value);
                }
                else {
                    httpCookies.remove(name);
                }
            }
            return (A) this;
        }

    }

}
