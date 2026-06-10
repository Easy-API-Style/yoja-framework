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

import io.vertx.core.Handler;

/**
 * Registry of cross-cutting handlers invoked around every HTTP exchange routed
 * by an {@link HttpRouter}.
 * <p>
 * Two hook points are exposed:
 * <ul>
 *   <li>{@code onRequest} handlers run before the matched
 *       {@link WebService} handlers and may abort the exchange via
 *       {@link HttpRequestEvent#abort()};</li>
 *   <li>{@code onResponse} handlers run before the body is written and may
 *       mutate the payload or abort the send via
 *       {@link HttpResponseEvent#abort()}.</li>
 * </ul>
 * Instances are created and populated by {@link HttpRouter.Builder} and are
 * not meant to be constructed directly by user code.
 */
public class HttpEvent {

    /** Handlers invoked in registration order on every incoming request. */
    private final List<Handler<HttpRequestEvent>> onRequestActions = new ArrayList<>();
    /** Handlers invoked in registration order before every outgoing response. */
    private final List<Handler<HttpResponseEvent>> onResponseActions = new ArrayList<>();

    /** Package-visible constructor; instances are produced by the router builder. */
    protected HttpEvent() {
        super();
    }

    /**
     * Returns the live list of request handlers (router-internal access).
     *
     * @return the live list of request handlers (router-internal access)
     */
    protected List<Handler<HttpRequestEvent>> getOnRequestActions() {
		return onRequestActions;
	}

    /**
     * Returns the live list of response handlers (router-internal access).
     *
     * @return the live list of response handlers (router-internal access)
     */
    protected List<Handler<HttpResponseEvent>> getOnResponseActions() {
		return onResponseActions;
	}

    /**
     * Appends a request handler that will run before any matched
     * {@link WebService} handler.
     *
     * @param action handler to register (must not be {@code null})
     */
    protected void onRequest(final Handler<HttpRequestEvent> action) {
        onRequestActions.add(action);
    }

    /**
     * Appends a response handler that will run before the body is written.
     *
     * @param action handler to register (must not be {@code null})
     */
    protected void onResponse(final Handler<HttpResponseEvent> action) {
        onResponseActions.add(action);
    }

	@Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpEvent.class.getSimpleName());
        result.append(" [onRequestActions=");
        result.append(onRequestActions.size());
        result.append(", onResponseActions=");
        result.append(onResponseActions.size());
        result.append("]");
        return result.toString();
    }

}