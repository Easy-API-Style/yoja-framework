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

import java.util.Comparator;
import java.util.Objects;

import io.vertx.core.http.CookieSameSite;

/**
 * Immutable representation of an HTTP cookie.
 * Equality and natural ordering are based on the combination of name, domain, and path.
 * Use {@link #of(String, String)} or {@link #builder(String, String)} to create instances.
 */
public class HttpCookie implements Comparable<HttpCookie> {

    private static final Comparator<HttpCookie> COMPARATOR = 
        Comparator.comparing(HttpCookie::getName, Comparator.nullsFirst(Comparator.naturalOrder()))
                  .thenComparing(HttpCookie::getDomain, Comparator.nullsFirst(Comparator.naturalOrder()))
                  .thenComparing(HttpCookie::getPath, Comparator.nullsFirst(Comparator.naturalOrder()));
    
    private String name; 
    private String value;
    private String domain;
    private String path;
    private long maxAge;
    private CookieSameSite sameSite;
    private boolean httpOnly;
    private boolean secure;
    
    private HttpCookie() {
        super();
    }

    /**
     * Returns the cookie name.
     *
     * @return the cookie name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the cookie value.
     *
     * @return the cookie value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the domain attribute, or {@code null} if not set.
     *
     * @return the domain, or {@code null}
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Returns the path attribute, or {@code null} if not set.
     *
     * @return the path, or {@code null}
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the max-age in seconds; {@code -1} means a session cookie.
     *
     * @return the max-age in seconds
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Returns the SameSite attribute.
     *
     * @return the SameSite value
     */
    public CookieSameSite getSameSite() {
        return sameSite;
    }

    /**
     * Returns {@code true} if the HttpOnly flag is set.
     *
     * @return {@code true} if HttpOnly
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Returns {@code true} if the Secure flag is set.
     *
     * @return {@code true} if Secure
     */
    public boolean isSecure() {
        return secure;
    }
    
    @Override
    public int compareTo(final HttpCookie httpCookie) {
        return COMPARATOR.compare(this, httpCookie);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(domain, name, path);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final HttpCookie other = (HttpCookie) obj;
        return Objects.equals(domain, other.domain)
                && Objects.equals(name, other.name)
                && Objects.equals(path, other.path);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpCookie.class.getSimpleName());
        result.append(" [name=");
        result.append(name);
        result.append(", value=");
        result.append(value);
        if (domain != null) {
            result.append(", domaine=");
            result.append(domain);
        }
        if (path != null) {
            result.append(", path=");
            result.append(path);
        }
        result.append(", maxAge=");
        result.append(maxAge);
        result.append(", sameSite=");
        result.append(sameSite);
        result.append(", httpOnly=");
        result.append(httpOnly);
        result.append(", secure=");
        result.append(secure);
        result.append("]");
        return result.toString();
    }
    
    /*
     * 
     * STATIC
     * 
     */
    /**
     * Creates a cookie with only a name and value, using default settings for all other attributes.
     *
     * @param name  the cookie name
     * @param value the cookie value
     * @return a new {@link HttpCookie} instance
     */
    public static HttpCookie of(final String name,
                                final String value) {
        return builder(name, value).build();
    }
    
    /*
     * 
     * BUILDER
     * 
     */
    /**
     * Creates a new {@link Builder} pre-initialised with the given name and value.
     *
     * @param name  the cookie name
     * @param value the cookie value
     * @return a new builder instance
     */
    public static Builder builder(final String name,
                                  final String value) {
        return new Builder(name, value);
    }
    
    /**
     * Fluent builder for {@link HttpCookie}.
     */
    public static class Builder {
        
        private final HttpCookie httpCookie = new HttpCookie();
        
        private Builder(final String name, 
                        final String value) {
            super();
            this.httpCookie.name = name;
            this.httpCookie.value = value;
        }
        
        /**
         * Sets the domain scope of the cookie.
         *
         * @param domain the domain, or {@code null} to omit
         * @return this builder
         */
        public Builder domain(final String domain) {
            this.httpCookie.domain = domain;
            return this;
        }

        /**
         * Sets the path scope of the cookie.
         *
         * @param path the path, or {@code null} to omit
         * @return this builder
         */
        public Builder path(final String path) {
            this.httpCookie.path = path;
            return this;
        }

        /**
         * Sets the maximum age of the cookie in seconds.
         * A value of {@code 0} instructs the browser to delete the cookie immediately.
         *
         * @param maxAge the max-age in seconds
         * @return this builder
         */
        public Builder maxAge(final long maxAge) {
            this.httpCookie.maxAge = maxAge;
            return this;
        }

        /**
         * Sets the SameSite policy for the cookie.
         *
         * @param sameSite the SameSite value, or {@code null} to omit the attribute
         * @return this builder
         */
        public Builder sameSite(final CookieSameSite sameSite) {
             this.httpCookie.sameSite = sameSite;
             return this;
        }
        
        /**
         * Sets whether the cookie is accessible only via HTTP (not via JavaScript).
         *
         * @param httpOnly {@code true} to set the HttpOnly flag
         * @return this builder
         */
        public Builder httpOnly(final boolean httpOnly) {
            this.httpCookie.httpOnly = httpOnly;
            return this;
        }

        /**
         * Sets whether the cookie should only be sent over HTTPS connections.
         *
         * @param secure {@code true} to set the Secure flag
         * @return this builder
         */
        public Builder secure(final boolean secure) {
            this.httpCookie.secure = secure;
             return this;
        }
        
        /**
         * Builds and returns the {@link HttpCookie} instance.
         *
         * @return the constructed cookie
         */
        public HttpCookie build() {
            return httpCookie;
        }
        
    }

}
