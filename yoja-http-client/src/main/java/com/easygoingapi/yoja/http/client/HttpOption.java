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

import java.time.Duration;

/**
 * Two-knob option bundle (TLS flag + optional timeout) that complements the
 * Vert.x-level engine settings.
 * <p>
 * Defaults to SSL on and no client-side timeout (delegating to the engine's
 * own default). Build instances through {@link #builder()}.
 */
public class HttpOption {

    /** Package-visible; instances are produced through {@link #builder()}. */
    HttpOption() {
        super();
    }

    /** Whether the request should be sent over TLS; defaults to {@code true}. */
    private boolean ssl = true;
    /** Per-request timeout; {@code null} means "use the engine default". */
    private Duration timeout;

    /**
     * Returns whether TLS is enabled.
     *
     * @return whether TLS is enabled
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * Returns the per-request timeout, or {@code null} for the engine default.
     *
     * @return the per-request timeout, or {@code null} for the engine default
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Returns a new builder with default settings (TLS on, no timeout).
     *
     * @return a new builder with default settings (TLS on, no timeout)
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link HttpOption}. */
    public static class Builder {

        /** Option being assembled. */
        private HttpOption httpOption = new HttpOption();

        /** Private — use {@link HttpOption#builder()}. */
        private Builder() {
            super();
        }

        /**
         * Toggles TLS for the request.
         *
         * @param ssl {@code true} to send over TLS
         * @return this builder
         */
        public Builder ssl(final boolean ssl) {
            httpOption.ssl = ssl;
            return this;
        }

        /**
         * Sets the per-request timeout.
         *
         * @param timeout request timeout
         * @return this builder
         */
        public Builder timeout(final Duration timeout) {
            httpOption.timeout = timeout;
            return this;
        }

        /**
         * Clears the per-request timeout (engine default applies again).
         *
         * @return this builder
         */
        public Builder noTimeout() {
            httpOption.timeout = null;
            return this;
        }

        /**
         * Returns the assembled {@link HttpOption}.
         *
         * @return the assembled {@link HttpOption}
         */
        public HttpOption build() {
            return httpOption;
        }

    }
}
