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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.easygoingapi.yoja.core.YojaAppException;
import com.google.common.io.CharStreams;

/**
 * Utility class for reading classpath resources.
 */
public class ResourceUtil {

    /** Not instantiable. */
    private ResourceUtil() {}

    /**
     * Reads the full content of a classpath resource as a UTF-8 string.
     *
     * @param path the classpath-relative path to the resource (e.g. {@code "config/app.json"})
     * @return the resource content as a string
     * @throws com.easygoingapi.yoja.core.YojaAppException if the resource cannot be read
     */
    public static String read(final String path) {
        try (final InputStream reader = ResourceUtil.class
                                                    .getClassLoader()
                                                    .getResourceAsStream(path);
             final InputStreamReader inputStreamReader = new InputStreamReader(reader);
             final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);) {
            return CharStreams.toString(bufferedReader);
        }
        catch (final Exception e) {
            throw new YojaAppException("read resource failed: " + path, e);
        }
    }
    
}
