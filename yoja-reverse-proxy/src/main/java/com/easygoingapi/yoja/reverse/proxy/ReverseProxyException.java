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

import com.easygoingapi.yoja.core.YojaAppException;

/**
 * Unchecked exception raised by the reverse-proxy module for misconfigured
 * rules (invalid paths, invalid ports), malformed URLs encountered during
 * resolution, and admin-API misuse. Extends the framework-wide
 * {@link YojaAppException}.
 */
public class ReverseProxyException extends YojaAppException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the given message and cause.
     *
     * @param message human-readable error description
     * @param cause   underlying cause
     */
    public ReverseProxyException(final String message,
                                 final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the given message and no cause.
     *
     * @param message human-readable error description
     */
    public ReverseProxyException(final String message) {
        super(message);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append("[message=");
        result.append(getMessage());
        result.append("] ");
        return result.toString();
    }

}
