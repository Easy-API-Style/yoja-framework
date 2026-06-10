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
 * {@link ReverseProxyResult} carrying the {@link ReverseProxyRule} that
 * produced the resolution.
 * <p>
 * The switcher emits this subclass whenever the URL was resolved through the
 * configured rule table; the fallback resolver (the {@code elseRule} function)
 * produces a plain {@link ReverseProxyResult} instead.
 */
public class ReverseProxyRuleResult extends ReverseProxyResult {

    /** The rule whose {@code from} matched the inbound URL. */
    private final ReverseProxyRule reverseProxyRule;

    /**
     * Constructs a rule-based resolution result.
     *
     * @param fromUrl          URL the request was received on
     * @param toUrl            resolved target URL
     * @param reverseProxyRule rule that produced the resolution
     */
    protected ReverseProxyRuleResult(final HttpUrl fromUrl,
                                     final HttpUrl toUrl,
                                     final ReverseProxyRule reverseProxyRule) {
        super(fromUrl, toUrl);
        this.reverseProxyRule = reverseProxyRule;
    }

    /**
     * Returns the rule that produced the resolution.
     *
     * @return the rule that produced the resolution
     */
    public ReverseProxyRule reverseProxyRule() {
        return reverseProxyRule;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(ReverseProxyRuleResult.class.getSimpleName());
        result.append(" [resolved=");
        result.append(isResolved());
        result.append(", fromUrl=");
        result.append(fromUrl());
        result.append(isResolved());
        if (isResolved()) {
             result.append(", toUrl=");
             result.append(toUrl());
        }
        result.append(", reverseProxyRule=");
        result.append(reverseProxyRule);
        result.append("]");
        return result.toString();
    }

}
