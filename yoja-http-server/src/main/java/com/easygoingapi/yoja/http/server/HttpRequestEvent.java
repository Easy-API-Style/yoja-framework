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

/**
 * Event passed to {@code onRequest} handlers registered through
 * {@link HttpEvent#onRequest}.
 * <p>
 * Extends {@link HttpRequest} with the ability to short-circuit the exchange:
 * when {@link #abort()} is called, the router stops dispatching further
 * handlers and returns HTTP status {@code 444} to the client.
 */
public class HttpRequestEvent extends HttpRequest {

	/** Routing context that produced this event. */
	private final HttpRoutingContext httpRoutingContext;
	/** Flag flipped to {@code true} by {@link #abort()}. */
	private boolean abort;

	/**
	 * Wraps the supplied routing context as a mutable pre-dispatch event.
	 *
	 * @param httpRoutingContext routing context for the inbound request
	 */
	protected HttpRequestEvent(final HttpRoutingContext httpRoutingContext) {
		super(httpRoutingContext.context());
		this.httpRoutingContext = httpRoutingContext;
	}

	/**
	 * Returns the originating {@link HttpRoutingContext}.
	 *
	 * @return the originating {@link HttpRoutingContext}
	 */
	public HttpRoutingContext routingContext() {
		return httpRoutingContext;
	}

	/**
	 * Marks the exchange to be aborted. The router will end the response with
	 * status code {@code 444} (no response) instead of invoking the matched
	 * service handlers.
	 */
	public void abort() {
		abort = true;
	}

	/**
	 * Returns {@code true} when {@link #abort()} has been called.
	 *
	 * @return {@code true} when {@link #abort()} has been called
	 */
	public boolean aborted() {
		return abort;
	}

}
