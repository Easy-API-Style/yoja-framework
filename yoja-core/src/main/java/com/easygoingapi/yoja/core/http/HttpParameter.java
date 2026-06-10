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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.easygoingapi.yoja.core.YojaAppException;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.util.StringUtil;

/**
 * Mutable, ordered collection of HTTP query parameters.
 * Multiple entries with the same name are supported.
 */
public class HttpParameter {

    /**
     * A single query parameter entry holding a name and an optional value.
     *
     * @param name  the parameter name
     * @param value the parameter value, may be {@code null}
     */
    public static record Entry(String name, String value) {}

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Creates an empty parameter collection.
     */
    public HttpParameter() {
        super();
    }

    /**
     * Copy constructor — creates a new instance with the same entries as {@code httpParameter}.
     *
     * @param httpParameter the source to copy entries from
     */
    public HttpParameter(final HttpParameter httpParameter) {
        super();
        this.entries.addAll(httpParameter.entries);
    }

    /**
     * Returns the total number of entries (counting duplicate names).
     *
     * @return the number of entries
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns {@code true} if there are no entries.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns a copy of the full ordered entry list.
     *
     * @return a copy of the entry list
     */
    public List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    /**
     * Returns {@code true} if at least one entry with the given name exists.
     *
     * @param name the parameter name to look for
     * @return {@code true} if found, {@code false} otherwise
     */
    public boolean hasName(final String name) {
        if(name != null) {
            return entries.stream()
                          .filter(v -> v.name().equals(name))
                          .findAny()
                          .isPresent();
        }
        return false;
    }

    /**
     * Returns the value of the first entry matching {@code name}, or {@code null} if none exists.
     *
     * @param name the parameter name
     * @return the first matching value, or {@code null}
     */
    public String firstValue(final String name) {
        if(name != null) {
            final List<String> values = values(name);
            return !values.isEmpty() ? values.get(0) : null;
        }
        return null;
    }
    
    /**
     * Returns all values associated with the given parameter name.
     *
     * @param name the parameter name
     * @return a list of values (never {@code null}, may be empty)
     */
    public List<String> values(final String name) {
        if(name != null) {
            return new ArrayList<>(entries.stream()
                                          .filter(v -> v.name().equals(name))
                                          .map(v -> v.value())
                                          .filter(v -> v != null)
                                          .toList());
        }
        return List.of();
    }

    /**
     * Returns the distinct parameter names present in this collection.
     *
     * @return a set of parameter names
     */
    public Set<String> names() {
        return entries.stream()
                      .map(v -> v.name())
                      .collect(Collectors.toSet());
    }
    
    /**
     * Appends a value-less entry with the given name.
     *
     * @param name the parameter name (must not be blank)
     * @return this instance for chaining
     */
    public HttpParameter addEntry(final String name) {
        return addEntry(name, null);
    }

    /**
     * Appends an entry with the given name and value without removing existing entries of the same name.
     *
     * @param name  the parameter name (must not be blank)
     * @param value the parameter value, may be {@code null}
     * @return this instance for chaining
     */
    public HttpParameter addEntry(final String name,
    		                      final String value) {
        if (StringUtil.isNullOrBlank(name)) {
            throw new YojaAppException("http name paramter non blank");
        }
        entries.add(new Entry(name, value));
        return this;
    }

    /**
     * Appends one entry per value in {@code list} for the given name, without removing existing entries.
     *
     * @param name the parameter name (must not be blank)
     * @param list the values to append; ignored if {@code null}
     * @return this instance for chaining
     */
    public HttpParameter addEntries(final String name,
                                    final List<String> list) {
        if (StringUtil.isNullOrBlank(name)) {
            throw new YojaAppException("http name paramter non blank");
        }
        if (list != null) {
            for (final String value : list) {
                entries.add(new Entry(name, value));
            }
        }
        return this;
    }
    
    /**
     * Replaces all existing entries for {@code name} with a single value-less entry.
     *
     * @param name the parameter name (must not be blank)
     * @return this instance for chaining
     */
    public HttpParameter putEntry(final String name) {
        return putEntry(name, null);
    }

    /**
     * Replaces all existing entries for {@code name} with a single entry holding {@code value}.
     *
     * @param name  the parameter name (must not be blank)
     * @param value the parameter value, may be {@code null}
     * @return this instance for chaining
     */
    public HttpParameter putEntry(final String name,
                                  final String value) {
        if (StringUtil.isNullOrBlank(name)) {
            throw new YojaAppException("http name paramter non blank");
        }
        removeEntries(name);
        entries.add(new Entry(name, value));
        return this;
    }

    /**
     * Replaces all existing entries for {@code name} with one entry per value in {@code list}.
     * If {@code list} is {@code null}, a single value-less entry is added.
     *
     * @param name the parameter name (must not be blank)
     * @param list the replacement values, or {@code null}
     * @return this instance for chaining
     */
    public HttpParameter putEntries(final String name,
                                    final List<String> list) {
        if (StringUtil.isNullOrBlank(name)) {
            throw new YojaAppException("http name paramter non blank");
        }
        removeEntries(name);
        if (list != null) {
            for (final String value : list) {
                entries.add(new Entry(name, value));
            }
        }
        else {
            putEntry(name);
        }
        return this;
    }

    /**
     * Removes all entries whose name equals {@code name}.
     *
     * @param name the parameter name to remove; ignored if {@code null}
     * @return this instance for chaining
     */
    public HttpParameter removeEntries(final String name) {
        if (name != null) {
            final Iterator<Entry> iterator = entries.iterator();
            while (iterator.hasNext()) {
                final Entry parameter = iterator.next();
                if (parameter.name().equals(name)) {
                    iterator.remove();
                }
            }
        }
        return this;
    }

    /**
     * Removes all entries from this collection.
     *
     * @return this instance for chaining
     */
    public HttpParameter clear() {
        entries.clear();
        return this;
    }
    
    /**
     * Serialises all entries to a query string (e.g. {@code "foo=bar&baz=1"}).
     *
     * @param format whether names and values should be URL-encoded or left decoded
     * @return the query string, never {@code null}
     */
    public String parameterQuery(final Format format) {
        final List<String> query = new ArrayList<>();
        for (final Entry entry : entries) {
            final StringBuilder param = new StringBuilder();
            if (Format.encoded == format) {
                param.append(HttpEncoding.urlEncode(entry.name()));
            }
            else {
                param.append(entry.name());
            }
            if (entry.value() != null) {
                param.append("=");
                if (Format.encoded == format) {
                    param.append(HttpEncoding.urlEncode(entry.value()));
                }
                else {
                    param.append(entry.value());
                }
            }
            query.add(param.toString());
        }
        return String.join("&", query);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpParameter.class.getSimpleName());
        result.append(" [");
        if (entries != null) {
            result.append("parameters=");
            result.append(entries);
        }
        result.append("]");
        return result.toString();
    }

    /*
     * 
     * STATIC
     * 
     */
    /**
     * Parses a raw query string into an {@link HttpParameter} instance.
     * Values are URL-decoded; fragments (after {@code #}) are ignored.
     *
     * @param parameterQuery the raw query string, may be {@code null}
     * @return a new {@link HttpParameter} instance (never {@code null})
     */
    public static HttpParameter parse(final String parameterQuery) {
        final HttpParameter result = new HttpParameter();
        if (parameterQuery != null) {
            final String parameters = parameterQuery.contains("#") 
                                          ? parameterQuery.split("#")[0] 
                                          : parameterQuery;
            final String[] splitParameters = parameters.split("&");
            for (final String parameter : splitParameters) {
                final String[] splitParameter = parameter.split("=");
                if (splitParameter.length > 1) {
                    final String name = HttpEncoding.urlDecode(splitParameter[0]);
                    if (!StringUtil.isNullOrBlank(name)) {
                        result.addEntry(name, 
                                        HttpEncoding.urlDecode(splitParameter[1]));
                    }
                }
                else if (splitParameter.length == 1) {
                    final String name = HttpEncoding.urlDecode(splitParameter[0]);
                    if (!StringUtil.isNullOrBlank(name)) {
                        result.addEntry(name);
                    }
                }
            }
        }
        return result;
    }
    
}
