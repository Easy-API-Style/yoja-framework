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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Classifies the runtime shape of an HTTP body so callers can branch on the
 * concrete payload without using {@code instanceof} chains.
 * <p>
 * The classification is derived from the Java runtime type of the body
 * reference via {@link #typeOf(Object)}.
 */
public enum HttpBodyType {
	/** No body (the reference is {@code null}). */
	None,
    /** A textual body carried as a {@link String}. */
    Text,
    /** A JSON object body carried as a Vert.x {@link JsonObject}. */
    JsonObject,
    /** A JSON array body carried as a Vert.x {@link JsonArray}. */
    JsonArray,
    /** A binary body carried as a {@code byte[]}. */
    binary;

    /**
     * Returns the body type matching the runtime class of {@code body}, or
     * {@code null} when the value does not match any of the supported shapes.
     *
     * @param body the raw body value (may be {@code null})
     * @return the corresponding {@link HttpBodyType}, {@link #None} when the
     *         body is {@code null}, or {@code null} for unsupported types
     */
    protected static HttpBodyType typeOf(final Object body) {
    	HttpBodyType result = null;
        if (body == null) {
        	result = None;
        }
        else if (body instanceof JsonObject) {
        	result = JsonObject;
        }
        else if (body instanceof JsonArray) {
        	result = JsonArray;
        }
        else if (body instanceof String) {
        	result = Text;
        }
        else if (body instanceof byte[]) {
        	result = binary;
        }
        return result;
    }

}