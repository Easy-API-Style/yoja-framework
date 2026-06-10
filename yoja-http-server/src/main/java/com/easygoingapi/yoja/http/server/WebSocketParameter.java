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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.easygoingapi.yoja.core.http.HttpEncoding;

/**
 * Parsed query string accompanying a WebSocket handshake.
 * <p>
 * Built by {@link #newInstance(String)} from the raw query (the part after
 * {@code ?} in the upgrade URL), this class preserves the original order of
 * entries and allows duplicate names. Values are URL-decoded.
 */
public class WebSocketParameter {

    /**
     * Single {@code name=value} entry. {@code value} may be {@code null} when
     * the original query contained a bare {@code name} without {@code =}.
     *
     * @param name  parameter name (URL-decoded)
     * @param value parameter value (URL-decoded), or {@code null}
     */
    public static record Entry(String name, String value) {}

    /** Parsed entries in document order. */
    private final List<Entry> entries;

    /**
     * Constructs a parameter set from pre-parsed entries (ownership transferred).
     *
     * @param entries parsed entries
     */
    protected WebSocketParameter(final List<Entry> entries) {
        super();
        this.entries = entries;
    }

    /**
     * Returns the total number of entries (including duplicates).
     *
     * @return the total number of entries (including duplicates)
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns {@code true} when no entry was parsed.
     *
     * @return {@code true} when no entry was parsed
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns the live list of parsed entries.
     *
     * @return the live list of parsed entries
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Returns all non-null values associated with the given name.
     *
     * @param name parameter name (may be {@code null})
     * @return all non-null values associated with this name; empty when none
     */
    public List<String> values(final String name) {
        if (name != null) {
            return entries.stream()
                          .filter(v -> v.name().equals(name))
                          .map(v -> v.value())
                          .filter(v -> v != null)
                          .toList();
        }
        return List.of();
    }

    /**
     * Returns {@code true} when at least one entry matches the given name.
     *
     * @param name parameter name (may be {@code null})
     * @return {@code true} when at least one entry matches this name
     */
    public boolean hasName(final String name) {
        if (name != null) {
            return entries.stream()
                          .filter(v -> v.name().equals(name))
                          .findAny()
                          .isPresent();
        }
        return false;
    }

    /**
     * Returns the first non-null value for the given name, or {@code null} when missing.
     *
     * @param name parameter name (may be {@code null})
     * @return the first non-null value for this name, or {@code null} when missing
     */
    public String firstValue(final String name) {
        if (name != null) {
            final List<String> values = values(name);
            return !values.isEmpty() ? values.get(0) : null;
        }
        return null;
    }

    /**
     * Returns the deduplicated set of parameter names present in the query.
     *
     * @return the deduplicated set of parameter names present in the query
     */
    public Set<String> names() {
        return entries.stream()
                      .map(v -> v.name())
                      .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(WebSocketParameter.class.getSimpleName());
        result.append(" [entries=");
        result.append(entries.size());
        result.append("]");
        return result.toString();
    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Parses an HTTP-style query string into a {@link WebSocketParameter}.
     * Name and value are split on {@code =} and each piece is URL-decoded;
     * entries with no {@code =} are kept as bare names (null value).
     *
     * @param httpQuery raw query (may be {@code null})
     * @return a new parameter view (possibly empty)
     */
    protected static WebSocketParameter newInstance(final String httpQuery) {
        final List<Entry> entries = new ArrayList<>();
        if (httpQuery != null) {
            final String[] splitQuery = httpQuery.split("&");
            for (final String parameter : splitQuery) {
                if (parameter.contains("=")) {
                    final String[] splitParameter = parameter.split("=");
                    final String name = HttpEncoding.urlDecode(splitParameter[0]);
                    final String value = HttpEncoding.urlDecode(splitParameter[1]);
                    entries.add(new Entry(name, value));
                }
                else {
                    final String name = HttpEncoding.urlDecode(parameter);
                    entries.add(new Entry(name, null));
                }
            }
        }
        return new WebSocketParameter(entries);
    }

}
