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

import com.easygoingapi.yoja.http.server.json.JsonWriter;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Outbound HTTP response surfaced to user handlers through
 * {@link HttpRouting#response()}.
 * <p>
 * Each {@code send*} overload triggers the {@link HttpEvent#onResponse}
 * pipeline before writing the bytes: response hooks can mutate or replace the
 * payload, or call {@link HttpResponseEvent#abort()} to short-circuit the send
 * (in which case the response is closed with status {@code 444}).
 */
public class HttpResponse extends AbstractHttpResponse {

	/** Routing context that originated this response. */
	private final HttpRoutingContext httpRoutingContext;

	/**
	 * Constructs a response bound to the given routing context.
	 *
	 * @param httpRoutingContext routing context for the current exchange
	 */
    protected HttpResponse(final HttpRoutingContext httpRoutingContext) {
        super(httpRoutingContext.context());
        this.httpRoutingContext = httpRoutingContext;
    }

    /*
     *
     * END BODY
     *
     */
    /**
     * Serializes {@code body} as JSON using the supplied Jackson view, then
     * sends it as a {@link JsonObject}.
     *
     * @param body POJO to serialize
     * @param view Jackson {@code @JsonView} marker class (may be {@code null})
     * @return a future completing when the response is flushed
     */
    public Future<Void> sendJson(final Object body,
                                 final Class<?> view) {
        return sendBody(JsonWriter.builder()
                                  .view(view)
                                  .build()
                                  .writeAsJsonObject(body));
    }

    /**
     * Maps {@code body} to a {@link JsonObject} using Vert.x' default mapping
     * and sends it.
     *
     * @param body POJO to serialize
     * @return a future completing when the response is flushed
     */
    public Future<Void> sendJson(final Object body) {
        return sendBody(JsonObject.mapFrom(body));
    }

    /**
     * Sends a {@link JsonObject} body (Content-Type defaults to
     * {@code application/json}).
     *
     * @param body payload to send
     * @return a future completing when the response is flushed
     */
    public Future<Void> send(final JsonObject body) {
        return sendBody(body);
    }

    /**
     * Sends a {@link JsonArray} body (Content-Type defaults to
     * {@code application/json}).
     *
     * @param body payload to send
     * @return a future completing when the response is flushed
     */
    public Future<Void> send(final JsonArray body) {
        return sendBody(body);
    }

    /**
     * Sends a textual body (Content-Type defaults to {@code text/plain}).
     *
     * @param body payload to send
     * @return a future completing when the response is flushed
     */
    public Future<Void> send(final String body) {
        return sendBody(body);
    }

    /**
     * Sends a binary body. The caller is responsible for the content type.
     *
     * @param body payload to send
     * @return a future completing when the response is flushed
     */
    public Future<Void> send(final byte[] body) {
        return sendBody(body);
    }

    /**
     * Ends the response with an empty body.
     *
     * @return a future completing when the response is flushed
     */
    public Future<Void> send() {
        return sendBody(null);
    }

    /**
     * Returns {@code true} when the response has already been ended.
     *
     * @return {@code true} when the response has already been ended
     */
    public boolean sent() {
        return httpServerResponse().ended();
    }

    /**
     * Common send path used by every {@code send} overload.
     * <p>
     * Wraps the body in an {@link HttpResponseEvent}, runs every registered
     * {@code onResponse} hook (each of which may mutate or replace the body),
     * and finally writes the bytes through
     * {@link AbstractHttpResponse#sendBodyWithContentType(Object)}. If a hook
     * called {@link HttpResponseEvent#abort()}, the response is closed with
     * HTTP {@code 444} and {@code "abort sending"} as body.
     *
     * @param body raw body, may be {@code null}
     * @return a future completing when the response is flushed, or a failed
     *         future when the response had already been ended
     */
    private Future<Void> sendBody(final Object body) {
    	final HttpResponseEvent httpResponseEvent = new HttpResponseEvent(httpRoutingContext, body);
    	Object _body = httpResponseEvent.body();
		for (final Handler<HttpResponseEvent> action : httpRoutingContext.router()
				                                                         .getHttpEvent()
				                                                         .getOnResponseActions()) {
			action.handle(httpResponseEvent);
			_body = httpResponseEvent.body();
		}
		Future<Void> result;
		if (!httpResponseEvent.aborted()) {
			if (!sent()) {
				result = sendBodyWithContentType(_body);
			}
			else {
				result = Future.failedFuture("already sent");
			}
		}
		else {
			statusCode(444);
			result = httpServerResponse().end("abort sending");
		}
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpResponse.class.getSimpleName());
        result.append(" [version=");
        result.append(httpServerRequest().version().name());
        result.append(", method=");
        result.append(httpServerRequest().method().name());
        result.append(", uri=");
        result.append(httpServerRequest().absoluteURI());
        result.append("]");
        return result.toString();
    }

}
