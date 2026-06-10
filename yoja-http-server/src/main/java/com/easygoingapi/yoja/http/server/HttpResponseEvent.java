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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Event passed to {@code onResponse} handlers registered through
 * {@link HttpEvent#onResponse}.
 * <p>
 * Combines response-side helpers from {@link AbstractHttpResponse} with the
 * ability to read, replace or clear the outgoing body before the bytes are
 * written, and to {@link #abort()} the send entirely. Hooks see the body in
 * its raw, unserialized form (the type returned by {@link #bodyType()}).
 */
public class HttpResponseEvent extends AbstractHttpResponse {

	/** Routing context the response is being built on. */
	private final HttpRoutingContext httpRoutingContext;
	/** Snapshot of the inbound request, for handlers that need it. */
	private final HttpRequest httpRequest;

	/** Current body value; may be replaced by handlers via the {@code update*} methods. */
	private Object body;
	/** Flipped to {@code true} by {@link #abort()} to stop the send. */
	private boolean abort;

	/**
	 * Constructs a response event for the given routing context and initial body.
	 *
	 * @param httpRoutingContext routing context for the exchange
	 * @param body               the body computed so far (may be {@code null})
	 */
	protected HttpResponseEvent(final HttpRoutingContext httpRoutingContext,
			                    final Object body) {
		super(httpRoutingContext.context());
		this.httpRoutingContext = httpRoutingContext;
		this.httpRequest = new HttpRequest(context());
		this.body = body;
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
	 * Returns a read-only view over the inbound request.
	 *
	 * @return a read-only view over the inbound request
	 */
	public HttpRequest request() {
		return httpRequest;
	}

	/*
	 *
	 * BODY
	 *
	 */
	/**
	 * Returns the {@link HttpBodyType} matching the current body's runtime type.
	 *
	 * @return the {@link HttpBodyType} matching the current body's runtime type
	 */
	public HttpBodyType bodyType() {
        return HttpBodyType.typeOf(body);
    }

	/**
	 * Despite its name, returns {@code true} when the body is <em>non-empty</em>
	 * (i.e. non-null and not a zero-length string, array, JSON object or JSON
	 * array).
	 *
	 * @return {@code true} when the response will carry a non-empty payload
	 */
	public boolean hasBody() {
		boolean result = false;
        if (body == null) {
        	result = true;
        }
        else if (body instanceof byte[] value) {
        	result = value.length == 0;
        }
        else if (body instanceof String value) {
        	result = value.isEmpty();
        }
        else if (body instanceof JsonObject value) {
        	result = value.isEmpty();
        }
        else if (body instanceof JsonArray value) {
        	result = value.isEmpty();
        }
        return !result;
    }

	/**
	 * Returns the body as a {@link JsonObject} (unchecked cast).
	 *
	 * @return the body as a {@link JsonObject} (unchecked cast)
	 */
	public JsonObject bodyAsJsonObject() {
		return (JsonObject) body;
	}

	/**
	 * Returns the body as a {@link JsonArray} (unchecked cast).
	 *
	 * @return the body as a {@link JsonArray} (unchecked cast)
	 */
	public JsonArray bodyAsJsonArray() {
		return (JsonArray) body;
	}

	/**
	 * Returns the body as a {@link String} (unchecked cast).
	 *
	 * @return the body as a {@link String} (unchecked cast)
	 */
	public String bodyAsText() {
		return (String) body;
	}

	/**
	 * Returns the body as a {@code byte[]} (unchecked cast).
	 *
	 * @return the body as a {@code byte[]} (unchecked cast)
	 */
	public byte[] bodyAsBinary() {
		return (byte[]) body;
	}

	/**
	 * Maps the current JSON body to a POJO of the given class.
	 *
	 * @param clazz target type
	 * @param <C>   target type parameter
	 * @return the decoded POJO
	 */
	public <C> C body(final Class<C> clazz) {
		return ((JsonObject) body).mapTo(clazz);
	}

	/**
	 * Returns the raw current body value (router-internal use).
	 *
	 * @return the raw current body value (router-internal use)
	 */
	protected Object body() {
		return body;
	}

	/*
	 *
	 * UPDATE BODY
	 *
	 */
    /**
     * Replaces the body with the JSON serialization of {@code body} using the
     * given Jackson view, stored as a {@link JsonObject}.
     *
     * @param body POJO to serialize
     * @param view Jackson view marker class (may be {@code null})
     */
    public void updateJsonBody(final Object body,
                               final Class<?> view) {
        updateBody(JsonWriter.builder()
                             .view(view)
                             .build()
                             .writeAsJsonObject(body));
    }

    /**
     * Replaces the body with Vert.x' default mapping of {@code body} to a
     * {@link JsonObject}.
     *
     * @param body POJO to serialize
     */
    public void updateJsonBody(final Object body) {
        updateBody(JsonObject.mapFrom(body));
    }

	/**
	 * Replaces the body with the supplied {@link JsonObject}.
	 *
	 * @param data new body
	 */
	public void updateBody(final JsonObject data) {
		update(data);
	}

	/**
	 * Replaces the body with the supplied {@link JsonArray}.
	 *
	 * @param body new body
	 */
	public void updateBody(final JsonArray body) {
		update(body);
	}

	/**
	 * Replaces the body with the supplied text.
	 *
	 * @param body new body
	 */
	public void updateBody(final String body) {
		update(body);
	}

	/**
	 * Replaces the body with the supplied byte array.
	 *
	 * @param body new body
	 */
	public void updateBody(final byte[] body) {
		update(body);
	}

	/** Clears the body so that the response will be sent with no payload. */
	public void clearBody() {
		update(null);
	}

	/** Internal helper backing every {@code update*} method. */
	private void update(final Object body) {
		this.body = body;
	}

	/*
	 *
	 * ABORT
	 *
	 */
	/**
	 * Marks the send as aborted. The router will end the response with status
	 * code {@code 444} instead of writing the body.
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

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append(HttpResponseEvent.class.getSimpleName());
		result.append(" [statusCode=");
		result.append(statusCode());
		result.append(", version=");
		result.append(httpServerRequest().version().name());
		result.append(", method=");
		result.append(httpServerRequest().method().name());
		result.append(", uri=");
		result.append(httpServerRequest().absoluteURI());
		result.append("]");
		return result.toString();
	}

}
