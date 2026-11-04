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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.easygoingapi.yoja.core.http.HttpEncoding;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.MultiMap;

/**
 * Parsed query string accompanying a WebSocket handshake, with the headers and
 * cookies of its upgrade request.
 * <p>
 * Built by {@link #newInstance(String, MultiMap)} from the raw query (the part
 * after {@code ?} in the upgrade URL), this class preserves the original order
 * of entries and allows duplicate names. Values are URL-decoded.
 * <p>
 * The headers and cookies let an acceptance predicate know who connects -- a
 * session cookie, an {@code Origin} -- see {@link WebSocket#asyncAccept}.
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

    /** Headers of the upgrade request (case-insensitive names); empty when unknown. */
    private final MultiMap headers;

    /** Cookies of the upgrade request, by name (the first one of a name kept). */
    private final Map<String, String> cookies;

    /**
     * Constructs a parameter set from pre-parsed entries (ownership transferred).
     *
     * @param entries parsed entries
     */
    protected WebSocketParameter(final List<Entry> entries) {
        this(entries, null);
    }

    /**
     * Constructs a parameter set from pre-parsed entries (ownership transferred)
     * and the headers of the upgrade request.
     *
     * @param entries parsed entries
     * @param headers headers of the upgrade request, or {@code null}
     */
    protected WebSocketParameter(final List<Entry> entries,
                                 final MultiMap headers) {
        super();
        this.entries = entries;
        this.headers = headers != null ? headers : MultiMap.caseInsensitiveMultiMap();
        this.cookies = parseCookies(this.headers.getAll("Cookie"));
    }

    /**
     * Returns the first value of a header of the upgrade request.
     *
     * @param name header name, case-insensitive (may be {@code null})
     * @return its first value, or {@code null} when missing
     */
    public String header(final String name) {
        return name != null ? headers.get(name) : null;
    }

    /**
     * Returns every value of a header of the upgrade request.
     *
     * @param name header name, case-insensitive (may be {@code null})
     * @return its values; empty when none
     */
    public List<String> headers(final String name) {
        return name != null ? List.copyOf(headers.getAll(name)) : List.of();
    }

    /**
     * Returns the value of a cookie sent with the upgrade request -- a session
     * cookie, to know who connects.
     *
     * @param name cookie name (may be {@code null})
     * @return its value, or {@code null} when not sent
     */
    public String cookie(final String name) {
        return name != null ? cookies.get(name) : null;
    }

    /**
     * Returns the names of the cookies sent with the upgrade request.
     *
     * @return the names of the cookies sent with the upgrade request
     */
    public Set<String> cookieNames() {
        return Set.copyOf(cookies.keySet());
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
        return newInstance(httpQuery, null);
    }

    /**
     * Parses an HTTP-style query string into a {@link WebSocketParameter},
     * with the headers -- and so the cookies -- of the upgrade request.
     *
     * @param httpQuery raw query (may be {@code null})
     * @param headers   headers of the upgrade request (may be {@code null})
     * @return a new parameter view (possibly empty)
     */
    protected static WebSocketParameter newInstance(final String httpQuery,
                                                    final MultiMap headers) {
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
        return new WebSocketParameter(entries, headers);
    }

    /**
     * @param cookieHeaders the {@code Cookie} headers of a request
     * @return its cookies by name -- a malformed one left out, the first of a name kept
     */
    private static Map<String, String> parseCookies(final List<String> cookieHeaders) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final String cookieHeader : cookieHeaders) {
            for (final Cookie cookie : ServerCookieDecoder.LAX.decodeAll(cookieHeader)) {
                result.putIfAbsent(cookie.name(), cookie.value());
            }
        }
        return result;
    }

}
