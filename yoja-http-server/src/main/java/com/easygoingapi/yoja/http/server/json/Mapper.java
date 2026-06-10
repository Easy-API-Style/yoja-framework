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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Process-wide accessor for the Jackson {@link ObjectMapper} used by
 * {@link JsonReader} and {@link JsonWriter}.
 * <p>
 * The mapper is created with default settings at class load time and can be
 * replaced wholesale via {@link #jsonMapper(ObjectMapper)} to configure custom
 * modules, naming strategies or visibility rules.
 */
public class Mapper {

	/** Shared Jackson mapper used by every {@link JsonReader} built from the package. */
	private static ObjectMapper JSON_OBJECT_MAPPER =
		JsonMapper.builder()
                  .build();

	/** Not instantiable; use the static methods. */
	private Mapper() {}

	/**
	 * Replaces the shared mapper. The change is process-wide and effective
	 * from the next {@link #jsonMapper()} call.
	 *
	 * @param objectMapper the new mapper (must not be {@code null})
	 */
	public static void jsonMapper(final ObjectMapper objectMapper) {
		JSON_OBJECT_MAPPER = objectMapper;
	}

	/**
	 * Returns the shared Jackson mapper.
	 *
	 * @return the shared Jackson mapper
	 */
	public static ObjectMapper jsonMapper() {
		return JSON_OBJECT_MAPPER;
	}

}
