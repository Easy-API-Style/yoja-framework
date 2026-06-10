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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Objects;

import com.easygoingapi.yoja.core.YojaAppException;

/**
 * Utility class for URL encoding and decoding of HTTP values.
 */
public class HttpEncoding {

    /** Not instantiable. */
    private HttpEncoding() {}

    /**
     * Indicates whether a string value should be URL-encoded or decoded.
     * <ul>
     *   <li>{@code encoded} — the value is (or should be) percent-encoded (e.g. {@code foo%20bar})</li>
     *   <li>{@code decoded} — the value is (or should be) in plain, human-readable form (e.g. {@code foo bar})</li>
     * </ul>
     */
    /**
     * Controls how URL components are rendered.
     */
    public enum Format {
        /** URL-encoded (percent-encoded) form. */
        encoded,
        /** Decoded (human-readable) form. */
        decoded
    }

    /**
     * Percent-decodes a URL-encoded string using UTF-8.
     *
     * @param value the URL-encoded string, may be {@code null}
     * @return the decoded string, or {@code null} if {@code value} was {@code null}
     * @throws com.easygoingapi.yoja.core.YojaAppException if decoding fails
     */
    public static String urlDecode(final String value) {
        try {
            final String result;
            if (value != null) {
                result = URLDecoder.decode(value, "UTF-8");
            }
            else {
                result = null;
            }
            return result;
        } 
        catch (final Exception e) {
            throw new YojaAppException("decode path failed", e);
        }
    }
    
    /**
     * Percent-encodes a string for use in a URL using UTF-8.
     *
     * @param value the plain string to encode, may be {@code null}
     * @return the URL-encoded string, or {@code null} if {@code value} was {@code null}
     * @throws com.easygoingapi.yoja.core.YojaAppException if encoding fails
     */
    public static String urlEncode(final String value) {
        try {
            final String result;
            if (value != null) {
                result = URLEncoder.encode(value, "UTF-8");
            }
            else {
                result = null;
            }
            return result;
        } 
        catch (final Exception e) {
            throw new YojaAppException("decode path failed", e);
        }
    }
    
    /**
     * Encodes or decodes {@code value} according to the requested {@code format}.
     *
     * @param format {@link Format#encoded} to URL-encode, {@link Format#decoded} to URL-decode
     * @param value  the string to transform, may be {@code null}
     * @return the transformed string, or {@code null} if {@code value} was {@code null}
     */
    public static String url(final Format format,
                             final String value) {
        Objects.requireNonNull(format, "need format");
        final String result;
        if (Format.encoded == format) {
            result = urlEncode(value);
        }
        else {
            result = urlDecode(value);
        }
        return result;
    }
    
}
