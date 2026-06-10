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
package com.easygoingapi.yoja.core.util;

import java.nio.file.Path;

/**
 * Utility class for normalising file-system paths to use forward slashes.
 */
public class PathUtil {

    /** Not instantiable. */
    private PathUtil() {}

	/**
	 * Converts a {@link Path} to a forward-slash string (safe for use in URLs and configs).
	 *
	 * @param path the path to format, may be {@code null}
	 * @return the normalised string, or {@code null} if {@code path} was {@code null}
	 */
	public static String formatPath(final Path path) {
		if (path != null) {
			return path.toString().replace("\\", "/");
		}
    	return null;
    }
	
	/**
	 * Replaces all backslashes in a path string with forward slashes.
	 *
	 * @param path the path string to format, may be {@code null}
	 * @return the normalised string, or {@code null} if {@code path} was {@code null}
	 */
	public static String formatPath(final String path) {
		if (path != null) {
			return path.replace("\\", "/");
		}
		return null;
    }
	
	
}
