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

import java.util.Collection;

import com.google.common.base.Throwables;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

/**
 * Package-private helpers that send canonical HTTP responses (200/400/403/404/500)
 * on a Vert.x Web {@link RoutingContext}.
 * <p>
 * Used by {@link ReverseProxySwitcher}'s admin endpoints to keep response
 * generation uniform: every helper sets the status code, an appropriate
 * {@code Content-Type} header when a body is sent, and writes the body in a
 * single {@code end(...)} call.
 */
public class StatusCode {

    /** Not instantiable; only static helpers are exposed. */
    private StatusCode() {
        super();
    }

    /**
     * Sends a JSON-array response with {@code 200 OK}. Each entry of
     * {@code values} is encoded with Vert.x' default {@link JsonArray#encode()}.
     *
     * @param routingContext routing context whose response will be written
     * @param values         items to serialize into the array
     */
    protected static void jsonArray(final RoutingContext routingContext,
                                    final Collection<?> values) {
        routingContext.response().setStatusCode(200);
        routingContext.response().putHeader("Content-Type", "application/array-json");
        routingContext.response().end(JsonArray.of(values.toArray()).encode());
    }

    /**
     * Ends the response with {@code 200 OK} and no body.
     *
     * @param routingContext routing context to respond on
     */
    protected static void ok(final RoutingContext routingContext) {
        routingContext.response().setStatusCode(200);
        routingContext.response().end();
    }

    /**
     * Ends the response with {@code 400 Bad Request} and the supplied
     * {@code text/plain} message.
     *
     * @param routingContext routing context to respond on
     * @param message        explanation written as plain text
     */
    protected static void badRequest(final RoutingContext routingContext,
                                  final String message) {
        routingContext.response().setStatusCode(400);
        routingContext.response().putHeader("Content-Type", "text/plain");
        routingContext.response().end(message);
    }

    /**
     * Ends the response with {@code 403 Forbidden} and the supplied
     * {@code text/plain} message.
     *
     * @param routingContext routing context to respond on
     * @param message        explanation written as plain text
     */
    protected static void forbidden(final RoutingContext routingContext,
                                    final String message) {
        routingContext.response().setStatusCode(403);
        routingContext.response().putHeader("Content-Type", "text/plain");
        routingContext.response().end(message);
    }

    /**
     * Ends the response with {@code 500 Internal Server Error} and the supplied
     * {@code text/plain} message.
     *
     * @param routingContext routing context to respond on
     * @param message        explanation written as plain text
     */
    protected static void serverError(final RoutingContext routingContext,
                                   final String message) {
        routingContext.response().setStatusCode(500);
        routingContext.response().putHeader("Content-Type", "text/plain");
        routingContext.response().end(message);
    }

    /**
     * Ends the response with {@code 500 Internal Server Error} and a textual
     * stack trace of {@code throwable} as body.
     *
     * @param routingContext routing context to respond on
     * @param throwable      cause to dump
     */
    protected static void serverError(final RoutingContext routingContext,
                                      final Throwable throwable) {
        routingContext.response().setStatusCode(500);
        routingContext.response().putHeader("Content-Type", "text/plain");
        routingContext.response().end(Throwables.getStackTraceAsString(throwable));
    }

    /**
     * Ends the response with {@code 404 Not Found} and a textual stack trace of
     * {@code throwable} as body.
     *
     * @param routingContext routing context to respond on
     * @param throwable      cause to dump
     */
    protected static void notFound(final RoutingContext routingContext,
                                   final Throwable throwable) {
        routingContext.response().setStatusCode(404);
        routingContext.response().putHeader("Content-Type", "text/plain");
        routingContext.response().end(Throwables.getStackTraceAsString(throwable));
    }

    /**
     * Ends the response with {@code 404 Not Found} and no body.
     *
     * @param routingContext routing context to respond on
     */
    protected static void notFound(final RoutingContext routingContext) {
        routingContext.response().setStatusCode(404);
        routingContext.response().end();
    }

}
