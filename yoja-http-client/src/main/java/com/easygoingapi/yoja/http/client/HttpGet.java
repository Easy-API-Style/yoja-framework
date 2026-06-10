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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.easygoingapi.yoja.core.http.HttpHeader;
import com.easygoingapi.yoja.core.http.HttpParameter;
import com.easygoingapi.yoja.core.http.HttpParameter.Entry;

/**
 * GET request: a path, optional headers and cookies (inherited from
 * {@link HttpRequest}), plus a value-object {@link HttpParameter} carrying
 * the query string.
 * <p>
 * Build through {@link #builder(String)} for a full fluent API; the static
 * {@link #of(String)} and {@link #of(String, String)} factories are
 * shortcuts for the no-parameter and pre-encoded-query cases.
 */
public class HttpGet extends HttpRequest {

    /** Query parameters attached to the request. */
    private final HttpParameter httpParameter;

    /** Private — instances are produced through {@link #builder(String)} or the {@code of} factories. */
    private HttpGet(final String path,
                    final Map<String, String> httpCookies,
                    final HttpHeader httpHeader,
                    final HttpParameter httpParameter) {
        super(path, httpCookies, httpHeader);
        this.httpParameter = httpParameter;
    }

    /**
     * Returns the query-parameter value object (client-internal access).
     *
     * @return the query-parameter value object (client-internal access)
     */
    protected HttpParameter httpParameter() {
        return httpParameter;
    }

    /**
     * Returns the number of parameter entries (including duplicates).
     *
     * @return the number of parameter entries (including duplicates)
     */
    public int parameterSize() {
        return httpParameter.size();
    }

    /**
     * Returns a copy of the full entry list (name/value pairs, in order).
     *
     * @return a copy of the full entry list (name/value pairs, in order)
     */
    public List<Entry> parameters() {
        return new ArrayList<>(httpParameter.entries());
    }

    /**
     * Returns every value associated with the given parameter name.
     *
     * @param name parameter name
     * @return every value associated with {@code name}; empty when none
     */
    public List<String> parameters(final String name) {
        return new ArrayList<>(httpParameter.values(name));
    }

    /**
     * Returns {@code true} when the request carries at least one entry with the given name.
     *
     * @param name parameter name
     * @return {@code true} when the request carries at least one entry with that name
     */
    public boolean hasParameter(final String name) {
        return httpParameter.hasName(name);
    }

    /**
     * Returns the first value for the given parameter name, or {@code null} when missing.
     *
     * @param name parameter name
     * @return the first value for {@code name}, or {@code null} when missing
     */
    public String firstParameter(final String name) {
        return httpParameter.firstValue(name);
    }

    /**
     * Returns the sorted set of parameter names present in the request.
     *
     * @return the sorted set of parameter names present in the request
     */
    public Set<String> parameterNames() {
        return new TreeSet<>(httpParameter.names());
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpGet.class.getSimpleName());
        result.append(" [");
        result.append("path=");
        result.append(path);
        result.append(", ");
        result.append("cookies=");
        result.append(httpCookies.size());
        result.append(", ");
        result.append("httpHeader=");
        result.append(httpHeader.size());
        result.append(", ");
        result.append("httpParameter=");
        result.append(httpParameter.size());
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder for a GET request to the given path.
     *
     * @param path request path
     * @return a new builder
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(final String path) {
        return new Builder(path);
    }

    /** Fluent builder for {@link HttpGet}. */
    public static class Builder extends HttpRequest.Builder<Builder> {

        /** Query parameters being accumulated. */
        private final HttpParameter httpParameter = new HttpParameter();

        /**
         * @param path request path
         */
        private Builder(final String path) {
            super(path);
        }

        /**
         * Appends a bare parameter (name only, no value).
         *
         * @param name parameter name
         * @return this builder
         */
        public Builder addParameter(final String name) {
            httpParameter.addEntry(name);
            return this;
        }

        /**
         * Appends a name/value parameter; duplicates of the same name are kept.
         *
         * @param name  parameter name
         * @param value parameter value
         * @return this builder
         */
        public Builder addParameter(final String name,
                                    final String value) {
            httpParameter.addEntry(name, value);
            return this;
        }

        /**
         * Appends every value as a separate entry under the same name.
         *
         * @param name   parameter name
         * @param values parameter values
         * @return this builder
         */
        public Builder addParameter(final String name,
                                    final List<String> values) {
            httpParameter.addEntries(name, values);
            return this;
        }

        /**
         * Replaces (or sets) every existing entry sharing the name with a
         * single bare entry.
         *
         * @param name parameter name
         * @return this builder
         */
        public Builder putParameter(final String name) {
            httpParameter.putEntry(name);
            return this;
        }

        /**
         * Replaces (or sets) every existing entry sharing the name with a
         * single name/value entry.
         *
         * @param name  parameter name
         * @param value parameter value
         * @return this builder
         */
        public Builder putParameter(final String name,
                                    final String value) {
            httpParameter.putEntry(name, value);
            return this;
        }

        /**
         * Replaces (or sets) every existing entry sharing the name with the
         * supplied list of values.
         *
         * @param name   parameter name
         * @param values parameter values
         * @return this builder
         */
        public Builder putParameters(final String name,
                                     final List<String> values) {
            httpParameter.putEntries(name, values);
            return this;
        }

        /**
         * Returns the assembled {@link HttpGet}.
         *
         * @return the assembled {@link HttpGet}
         */
        public HttpGet build() {
            return new HttpGet(path,
                               httpCookies,
                               httpHeader,
                               httpParameter);
        }

    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Convenience factory for a parameterless GET.
     *
     * @param path request path
     * @return the assembled GET request
     */
    public static HttpGet of(final String path) {
        return HttpGet.builder(path).build();
    }

    /**
     * Convenience factory taking a raw query string (parsed via
     * {@link HttpParameter#parse(String)}).
     *
     * @param path           request path
     * @param parameterQuery raw query string
     * @return the assembled GET request
     */
    public static HttpGet of(final String path,
                             final String parameterQuery) {
        return new HttpGet(path,
                           Map.of(),
                           new HttpHeader(),
                           HttpParameter.parse(parameterQuery));
    }

}
