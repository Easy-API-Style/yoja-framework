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
package com.easygoingapi.yoja.core.http;

/**
 * Common HTTP {@code Content-Type} media types.
 */
public enum ContentType {

    /** Plain text: {@code text/plain}. */
    text("text/plain"),

    /** JSON object: {@code application/json}. */
    jsonObject("application/json"),

    /** JSON array: {@code application/array-json}. */
    jsonArray("application/array-json");

    /** The HTTP header name for content type: {@code "content-type"}. */
    public static final String key = "content-type";
    
    private final String value;
    
    private ContentType(final String value) {
        this.value = value;
    }

    /**
     * Returns the MIME type string (e.g. {@code "application/json"}).
     *
     * @return the MIME type string
     */
    public String value() {
        return value;
    }
    
}
