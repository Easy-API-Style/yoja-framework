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
package com.easygoingapi.yoja.core.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Mutable collection of HTTP headers.
 * Header names are stored and looked up in lower-case to ensure case-insensitive behaviour.
 */
public class HttpHeader {

    private final Map<String, String> values = new HashMap<>();
    
    /** Creates an empty header map. */
    public HttpHeader() {
        super();
    }

    /**
     * Copy constructor — creates a new instance with the same headers as {@code httpHeader}.
     *
     * @param httpHeader the source to copy headers from
     */
    public HttpHeader(final HttpHeader httpHeader) {
        super();
        this.values.putAll(httpHeader.values);
    }
    
    /**
     * Returns the number of headers.
     *
     * @return the number of headers
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns {@code true} if no headers are present.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns a sorted copy of all headers as a name→value map.
     *
     * @return a sorted copy of the headers
     */
    public Map<String, String> values() {
        return new TreeMap<>(values);
    }

    /**
     * Returns {@code true} if a header with the given name is present (case-insensitive).
     *
     * @param name the header name
     * @return {@code true} if the header exists
     */
    public boolean has(final String name) {
        if (name != null) {
            return values.containsKey(name.toLowerCase());
        }
        return false;
    }

    /**
     * Returns the set of header names present in this collection (lower-case, sorted).
     *
     * @return a sorted set of header names
     */
    public Set<String> names() {
        return new TreeSet<>(values.keySet());
    }
    
    /**
     * Returns the value of the header with the given name, or {@code null} if absent (case-insensitive).
     *
     * @param name the header name
     * @return the header value, or {@code null}
     */
    public String value(final String name) {
        if (name != null) {
            return values.get(name.toLowerCase());
        }
        return null;
    }

    /**
     * Sets (or replaces) the header with the given name and value.
     * The name is stored in lower-case.
     *
     * @param name  the header name (must not be {@code null})
     * @param value the header value, may be {@code null}
     * @return this instance for chaining
     */
    public HttpHeader put(final String name,
                          final String value) {
        Objects.requireNonNull(name, "http name paramter non null");
        remove(name.toLowerCase());
        values.put(name.toLowerCase(), value);
        return this;
    }

    /**
     * Removes the header with the given name (case-insensitive); no-op if absent.
     *
     * @param name the header name to remove; ignored if {@code null}
     * @return this instance for chaining
     */
    public HttpHeader remove(final String name) {
        if (name != null) {
            values.remove(name.toLowerCase());
        }
        return this;
    }

    /**
     * Removes all headers from this collection.
     *
     * @return this instance for chaining
     */
    public HttpHeader clear() {
        values.clear();
        return this;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final HttpHeader other = (HttpHeader) obj;
        return Objects.equals(values, other.values);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpHeader.class.getSimpleName());
        result.append(" [values=");
        result.append(values);
        result.append("]");
        return result.toString();
    }
    
}
