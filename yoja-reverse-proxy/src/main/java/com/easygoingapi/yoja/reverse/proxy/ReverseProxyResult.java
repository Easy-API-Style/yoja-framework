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
package com.easygoingapi.yoja.reverse.proxy;

import com.easygoingapi.yoja.core.http.HttpUrl;

/**
 * Outcome of resolving an inbound URL against the reverse-proxy switcher.
 * <p>
 * Carries the URL the request arrived on ({@link #fromUrl()}) and, when a
 * match was found, the target URL the request should be forwarded to
 * ({@link #toUrl()}). When no rule matches and the optional fallback resolver
 * yields nothing either, {@code toUrl} is {@code null} and
 * {@link #isResolved()} returns {@code false}.
 * <p>
 * Subclass {@link ReverseProxyRuleResult} adds the {@link ReverseProxyRule}
 * that produced a resolution. Plain {@code ReverseProxyResult} instances are
 * produced either by the fallback resolver or to signal a miss.
 */
public class ReverseProxyResult {

    /** URL the request was received on. */
    private final HttpUrl fromUrl;
    /** Target URL to forward to, or {@code null} when no match was found. */
    private final HttpUrl toUrl;

    /**
     * Constructs a resolution result for the given from/to URL pair.
     *
     * @param fromUrl URL the request was received on
     * @param toUrl   target URL, or {@code null} when no resolution succeeded
     */
    protected ReverseProxyResult(final HttpUrl fromUrl,
                                 final HttpUrl toUrl) {
        super();
        this.fromUrl = fromUrl;
        this.toUrl = toUrl;
    }

    /**
     * Returns {@code true} when a target URL was produced (rule or fallback).
     *
     * @return {@code true} when a target URL was produced (rule or fallback)
     */
    public boolean isResolved() {
        return toUrl != null;
    }

    /**
     * Returns the URL the request was received on.
     *
     * @return the URL the request was received on
     */
    public HttpUrl fromUrl() {
        return fromUrl;
    }

    /**
     * Returns the target URL, or {@code null} when the resolution failed.
     *
     * @return the target URL, or {@code null} when the resolution failed
     */
    public HttpUrl toUrl() {
        return toUrl;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(ReverseProxyResult.class.getSimpleName());
        result.append(" [resolved=");
        result.append(isResolved());
        result.append(", fromUrl=");
        result.append(fromUrl);
        if (isResolved()) {
             result.append(", toUrl=");
             result.append(toUrl);
        }
        result.append(", resolved=");
        result.append(isResolved());
        result.append("]");
        return result.toString();
    }

}
