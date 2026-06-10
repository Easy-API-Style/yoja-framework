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

import io.vertx.ext.web.RoutingContext;

/**
 * Concrete context handed to service handlers (the {@code Handler<HttpRouting>}
 * values registered through {@link WebService}).
 * <p>
 * Extends {@link HttpRoutingContext} by also exposing the request and response
 * objects: every routed handler sees a single {@code HttpRouting} instance for
 * the whole exchange.
 */
public class HttpRouting extends HttpRoutingContext {

	/** Request view built from the routing context. */
	private final HttpRequest httpRequest;
	/** Response view bound to the routing context. */
	private final HttpResponse httpResponse;

	/**
	 * Constructs a routing handle combining request and response views.
	 *
	 * @param webServiceType  whether the matched entry is a service or a resource
	 * @param routingContext  the underlying Vert.x routing context
	 * @param httpRouter      the owning router
	 */
	protected HttpRouting(final WebService.Type webServiceType,
			              final RoutingContext routingContext,
			              final HttpRouter httpRouter) {
		super(webServiceType, routingContext, httpRouter);
		this.httpRequest = new HttpRequest(routingContext);
		this.httpResponse = new HttpResponse(this);
	}

	/*
     *
     *
     *
     */
    /**
     * Hands the exchange over to the next handler in the chain (Vert.x'
     * {@code RoutingContext.next()}).
     */
    public void nextHandler() {
		context().next();
	}

    /*
     *
     *
     *
     */
	/**
	 * Returns a read-only view over the inbound request.
	 *
	 * @return a read-only view over the inbound request
	 */
	public HttpRequest request() {
		return httpRequest;
	}

	/**
	 * Returns the response object used to write the reply.
	 *
	 * @return the response object used to write the reply
	 */
	public HttpResponse response() {
		return httpResponse;
	}

}
