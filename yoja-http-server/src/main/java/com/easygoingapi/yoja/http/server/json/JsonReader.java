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

import com.easygoingapi.yoja.http.server.HttpServerException;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Thin wrapper around a Jackson {@link ObjectReader} that converts read
 * failures into {@link HttpServerException}.
 * <p>
 * Instances are built through {@link #builder()} and optionally bound to a
 * Jackson {@code @JsonView} via {@link Builder#view(Class)}.
 */
public class JsonReader {

	/** The wrapped Jackson reader. */
	private final ObjectReader objectReader;

	/** Private — use {@link #builder()}. */
	private JsonReader(final ObjectReader objectReader) {
		super();
		this.objectReader = objectReader;
	}

	/**
	 * Reads {@code json} into an instance of {@code clazz} using the wrapped
	 * reader.
	 *
	 * @param json  JSON document
	 * @param clazz target class
	 * @param <O>   target type
	 * @return the decoded value
	 * @throws HttpServerException when Jackson fails to parse or bind
	 */
	public <O> O read(final String json,
			          final Class<O> clazz) {
		try {
			return objectReader.forType(clazz).readValue(json);
		}
		catch (final Exception e) {
			throw new HttpServerException("read json failed", e);
		}
	}

	/*
	 *
	 * STATIC
	 *
	 */
	/**
	 * Convenience one-shot read: builds a default reader and decodes
	 * {@code json} into a value of {@code clazz}.
	 *
	 * @param json  JSON document to parse
	 * @param clazz target class
	 * @param <O>   target type
	 * @return the decoded value
	 */
	public static <O> O readValue(final String json,
			                      final Class<O> clazz) {
		return builder().build().read(json, clazz);
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

	/** Fluent builder for {@link JsonReader}. */
	public static class Builder {

		/** Optional Jackson view marker class. */
		private Class<?> view;

		/** Private — use {@link JsonReader#builder()}. */
		private Builder() {
		    super();
		}

		/**
		 * Binds the reader to a Jackson {@code @JsonView} marker class so
		 * only fields visible to that view are populated.
		 *
		 * @param view Jackson view marker class
		 * @return this builder
		 */
		public Builder view(final Class<?> view) {
			this.view = view;
			return this;
		}

		/**
		 * Returns the configured {@link JsonReader}.
		 *
		 * @return the configured {@link JsonReader}
		 */
		public JsonReader build() {
			ObjectReader reader = Mapper.jsonMapper().reader();
			if (view != null) {
				reader = reader.withView(view);
			}
			return new JsonReader(reader);
		}

	}

}
