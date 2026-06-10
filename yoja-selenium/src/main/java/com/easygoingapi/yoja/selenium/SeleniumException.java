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
package com.easygoingapi.yoja.selenium;

/**
 * Unchecked exception raised by the Selenium module for WebDriver failures,
 * script-execution errors and HTTP-server-context lifecycle issues.
 * <p>
 * Unlike the other Yoja exceptions, this one extends {@link RuntimeException}
 * directly (not the framework-wide {@code YojaAppException}) because the
 * Selenium module is meant for tests and may be used without the rest of the
 * framework's runtime.
 */
public class SeleniumException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** No-arg constructor; used when neither a message nor a cause is available. */
    public SeleniumException() {
        super();
    }

    /**
     * Constructs a new exception with the given message and cause.
     *
     * @param message human-readable error description
     * @param cause   underlying cause
     */
    public SeleniumException(final String message,
                             final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the given message and no cause.
     *
     * @param message human-readable error description
     */
    public SeleniumException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception wrapping the given cause.
     *
     * @param cause underlying cause (used when no message adds context)
     */
    public SeleniumException(final Throwable cause) {
        super(cause);
    }

}
