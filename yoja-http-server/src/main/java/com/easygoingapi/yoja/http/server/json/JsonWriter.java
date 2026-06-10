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
package com.easygoingapi.yoja.http.server.json;

import java.util.Collection;

import com.easygoingapi.yoja.http.server.HttpServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Thin wrapper around a Jackson {@link ObjectWriter} that converts write
 * failures into {@link HttpServerException}, plus helpers to materialize the
 * result as a Vert.x {@link JsonObject} or {@link JsonArray}.
 * <p>
 * Instances are built through {@link #builder()}; the builder supports pretty
 * printing and Jackson views.
 * <p>
 * Note: this class uses its own private {@link ObjectMapper} (initialized in
 * a static field) rather than the shared one held by {@link Mapper}, so
 * customisations registered on {@code Mapper} are <em>not</em> picked up by
 * the writer.
 */
public class JsonWriter {

	/** Private Jackson mapper used for every writer built from this class. */
	private static final ObjectMapper JSON_OBJECT_MAPPER = JsonMapper.builder()
                                                                     .build();

	/** The wrapped Jackson writer. */
	private final ObjectWriter objectWriter;

	/** Private — use {@link #builder()}. */
	private JsonWriter(final ObjectWriter objectWriter) {
		super();
		this.objectWriter = objectWriter;
	}

	/**
	 * Serializes {@code value} to a JSON string.
	 *
	 * @param value the object to serialize
	 * @return the JSON string
	 * @throws HttpServerException when Jackson fails to serialize
	 */
	public String write(final Object value) {
		try {
			return objectWriter.writeValueAsString(value);
		}
		catch (final Exception e) {
			throw new HttpServerException("write json failed", e);
		}
	}

	/**
	 * Returns the JSON serialization of {@code value} wrapped in a Vert.x {@link JsonObject}.
	 *
	 * @param value the object to serialize
	 * @return the JSON serialization wrapped in a {@link JsonObject}
	 */
	public JsonObject writeAsJsonObject(final Object value) {
		return new JsonObject(write(value));
	}

	/**
	 * Serializes each element of {@code values} separately and gathers the
	 * results into a {@link JsonArray}.
	 *
	 * @param values input collection
	 * @return a JSON array where every entry is the JSON object form of an input element
	 */
	public JsonArray writeAsJsonArray(final Collection<?> values) {
		final JsonArray jsonArray = new JsonArray();
    	for (final Object v : values) {
    		jsonArray.add(writeAsJsonObject(v));
    	}
		return jsonArray;
	}

	/*
	 *
	 * STATIC
	 *
	 */
	/**
	 * Convenience one-shot write with a Jackson view.
	 *
	 * @param value the object to serialize
	 * @param view  Jackson view marker class
	 * @return the JSON string
	 */
	public static String writeValue(final Object value,
			                        final Class<?> view) {
		return builder().view(view).build().write(value);
	}

	/**
	 * Returns the JSON serialization of {@code value} as a {@link JsonObject}, applying the given view.
	 *
	 * @param value the object to serialize
	 * @param view  Jackson view marker class
	 * @return the JSON object
	 */
	public static JsonObject writeValueAsJsonObject(final Object value,
                                                    final Class<?> view) {
		return new JsonObject(writeValue(value, view));
	}

	/**
	 * Returns a {@link JsonArray} where each element is the JSON-object form of the corresponding input element.
	 *
	 * @param values input collection
	 * @param view   Jackson view marker class
	 * @return the JSON array
	 */
	public static JsonArray writeValueAsJsonArray(final Collection<?> values,
                                                  final Class<?> view) {
		return builder().view(view).build().writeAsJsonArray(values);
	}

	/**
	 * Serializes {@code value} with default settings (no view, no pretty-print).
	 *
	 * @param value the object to serialize
	 * @return the JSON string
	 */
	public static String writeValue(final Object value) {
		return builder().build().write(value);
	}

	/*
	 *
	 * BUILDER
	 *
	 */
	/**
	 * Returns a new builder.
	 *
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/** Fluent builder for {@link JsonWriter}. */
	public static class Builder {

		/** Optional Jackson view marker class. */
		private Class<?> view;
		/** When true, emits the indented pretty-printed form. */
		private boolean pretty;

		/** Private — use {@link JsonWriter#builder()}. */
		private Builder() {
			super();
		}

		/**
		 * Binds the writer to a Jackson {@code @JsonView} marker class so
		 * only fields visible to that view are emitted.
		 *
		 * @param view Jackson view marker class
		 * @return this builder
		 */
		public Builder view(final Class<?> view) {
			this.view = view;
			return this;
		}

		/**
		 * Enables or disables Jackson's default pretty printer.
		 *
		 * @param pretty {@code true} to enable pretty printing
		 * @return this builder
		 */
		public Builder pretty(final boolean pretty) {
			this.pretty = pretty;
			return this;
		}

		/**
		 * Returns the configured {@link JsonWriter}.
		 *
		 * @return the configured {@link JsonWriter}
		 */
		public JsonWriter build() {
			ObjectWriter writer = JSON_OBJECT_MAPPER.writer();
			if (pretty) {
				writer = writer.withDefaultPrettyPrinter();
			}
			if (view != null) {
				writer = writer.withView(view);
			}
			return new JsonWriter(writer);
		}

	}

}
