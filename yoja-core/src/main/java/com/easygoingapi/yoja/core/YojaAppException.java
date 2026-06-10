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
package com.easygoingapi.yoja.core;

/**
 * Unchecked exception thrown by the Yoja framework when an operation fails.
 *
 * <p>This exception sits at the root of the framework's exception hierarchy:
 * every subsystem (HTTP server, HTTP client, reverse proxy, Selenium, …)
 * defines its own unchecked exception that either extends or wraps
 * {@code YojaAppException}. Because it extends {@link RuntimeException},
 * callers are never forced to declare or catch it — it propagates freely
 * through reactive callbacks and worker tasks.
 *
 * <p>Typical usage at a system boundary:
 * <pre>{@code
 * try {
 *     YojaApp.start();
 * }
 * catch (final YojaAppException e) {
 *     LOGGER.error("yoja startup failed: {}", e.getMessage(), e);
 * }
 * }</pre>
 *
 * <p>Throwing it manually, with or without an underlying cause:
 * <pre>{@code
 * throw new YojaAppException("invalid config");
 * throw new YojaAppException("worker submit failed", ioException);
 * }</pre>
 */
public class YojaAppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception with the given message and an underlying cause.
     *
     * @param message a short, human-readable description of the failure
     * @param cause   the underlying exception that triggered this one
     *                (may be {@code null} if there is no nested cause)
     */
    public YojaAppException(final String message,
                            final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with the given message and no underlying cause.
     *
     * @param message a short, human-readable description of the failure
     */
    public YojaAppException(final String message) {
        super(message);
    }

    /**
     * Returns a string representation in the form
     * {@code "YojaAppException[message=<message>] "} (note the trailing
     * space — preserved for backwards-compatible log-line parsing).
     *
     * @return a debugging-friendly description, never {@code null}
     */
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
