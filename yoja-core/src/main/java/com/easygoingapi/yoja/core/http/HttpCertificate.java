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
 * Defines the TLS/SSL mode used by an HTTP server.
 */
public enum HttpCertificate {

    /** No TLS — plain HTTP, no certificate required. */
    NONE,

    /** TLS enabled using externally provided PEM certificate and private key files. */
    SSL,

    /** TLS enabled with a self-signed certificate generated automatically at startup. */
    SELF_SIGNED;
    
    /**
     * Returns {@code true} if this constant is the same as {@code httpCertificate}.
     *
     * @param httpCertificate the constant to compare against
     * @return {@code true} if they are the same constant
     */
    public boolean is(final HttpCertificate httpCertificate) {
        return this == httpCertificate;
    }
    
    /**
     * Returns the {@link HttpCertificate} constant whose name matches {@code value}, or {@code null} if none matches.
     *
     * @param value the name to look up (case-sensitive)
     * @return the matching constant, or {@code null}
     */
    public static HttpCertificate parse(final String value) {
        HttpCertificate result = null;
        for (final HttpCertificate certificate : HttpCertificate.values()) {
            if (certificate.name().equals(value)) {
                result = certificate;
                break;
            }
        }
        return result;
    }
    
}