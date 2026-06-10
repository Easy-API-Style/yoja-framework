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

import java.nio.file.Path;

import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.util.StringUtil;

/**
 * Immutable representation of an HTTP/HTTPS/WS/WSS URL, including protocol, host, port,
 * path, query parameters and fragment. Use {@link Builder} to construct instances.
 */
public class HttpUrl {

    private HttpProtocole protocol = HttpProtocole.https;
    private String host;
    private Integer port;
    private String path = "/";
    private HttpParameter httpParameter;
    private String fragment;
    
    private HttpUrl() {
    	
    }
    
    private HttpUrl(final HttpProtocole protocol, 
                    final String host,
                    final Integer port, 
                    final String path,
                    final HttpParameter httpParameter,
                    final String fragment) {
        super();
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.httpParameter = httpParameter;
        this.fragment = fragment;
    }
    
    /**
     * Returns the URL protocol.
     *
     * @return the protocol
     */
    public HttpProtocole protocol() {
        return protocol;
    }

    /**
     * Returns the host name or IP address.
     *
     * @return the host
     */
    public String host() {
        return host;
    }

    /**
     * Returns the port, or {@code null} if not explicitly set.
     *
     * @return the port, or {@code null}
     */
    public Integer port() {
        return port;
    }

    /**
     * Returns the URL path.
     *
     * @return the path
     */
    public String path() {
        return path;
    }
    
    /**
     * Returns the query string built from this URL's parameters, or {@code null} if there are none.
     *
     * @param format whether parameter names and values should be URL-encoded or decoded
     * @return the query string (e.g. {@code "foo=bar&baz=1"}), or {@code null}
     */
    public String parameterQuery(final Format format) {
        return httpParameter != null 
                && !httpParameter.isEmpty()
                  ? httpParameter.parameterQuery(format) 
                  : null;
    }
    
    /**
     * Returns the URL fragment, optionally encoded or decoded.
     *
     * @param format whether the fragment should be URL-encoded or decoded
     * @return the fragment string, or {@code null} if not set
     */
    public String fragment(final Format format) {
        return HttpEncoding.url(format, fragment);
    }
    
    /**
     * Returns the path combined with query string and fragment (e.g. {@code "/api?foo=bar#section"}).
     *
     * @param format whether query parameters and fragment should be URL-encoded or decoded
     * @return the path-and-query string, never {@code null}
     */
    public String pathAndQuery(final Format format) {
    	final StringBuilder result = new StringBuilder();
    	if (!StringUtil.isNullOrBlank(path)) {
            result.append(path);
        }
        if (httpParameter != null 
                && !httpParameter.isEmpty()) {
            result.append("?");
            result.append(parameterQuery(format));
        }
        if (!StringUtil.isNullOrBlank(fragment)) {
        	result.append("#");
            result.append(fragment(format));
        }
        return result.toString();
    }
    
    /**
     * Returns the full URL string (e.g. {@code "https://example.com:8080/api?foo=bar"}).
     *
     * @param format whether query parameters and fragment should be URL-encoded or decoded
     * @return the full URL string, never {@code null}
     */
    public String url(final Format format) {
        final StringBuilder result = new StringBuilder();
        if (protocol != null) {
            result.append(protocol);
            result.append("://");
        }
        result.append(host);
        if (port != null && port > 0) {
            result.append(":");
            result.append(port);
        }
        result.append(pathAndQuery(format));
        return result.toString();
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpUrl.class.getSimpleName());
        result.append(" [");
        result.append("protocol=");
        result.append(protocol.name());
        result.append(", ");
        result.append("host=");
        result.append(host);
        
        if (port != null) {
            result.append(", ");
            result.append("port=");
            result.append(port);
        }
        if (path != null) {
            result.append(", ");
            result.append("path=");
            result.append(path);
        }
        if (httpParameter != null) {
            result.append(", ");
            result.append("parameter=");
            result.append(httpParameter);
        }
        if (fragment != null) {
            result.append(", ");
            result.append("fragment=");
            result.append(fragment);
        }
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Creates a new {@link Builder} for the given host.
     *
     * @param host the target hostname or IP address (must not be blank)
     * @return a new builder instance
     */
    public static Builder builder(final String host) {
    	return new Builder(host);
    }
    
    /**
     * Fluent builder for {@link HttpUrl}.
     */
    public static class Builder {

    	private final HttpUrl httpUrl = new HttpUrl();

    	private Builder(final String host) {
    		httpUrl.host = host;
    	}

    	/**
    	 * Sets the protocol; defaults to {@link HttpProtocole#https} when {@code null}.
    	 *
    	 * @param httpProtocol the desired protocol
    	 * @return this builder
    	 */
    	public Builder protocol(final HttpProtocole httpProtocol) {
    		httpUrl.protocol = httpProtocol != null 
    		                      ? httpProtocol 
    		                      : HttpProtocole.https;
            return this;
        }

        /**
         * Sets the port; omit or pass {@code null} to use the protocol's default port.
         *
         * @param port the port number
         * @return this builder
         */
        public Builder port(final Integer port) {
        	httpUrl.port = port;
            return this;
        }
        
        /**
         * Sets the URL path from a {@link java.nio.file.Path}; separators are normalised to {@code /}.
         *
         * @param path the path, or {@code null} to reset to {@code "/"}
         * @return this builder
         */
        public Builder path(final Path path) {
            path(path != null ? path.toString() : null);
            return this;
        }

        /**
         * Sets the URL path from a string; separators are normalised to {@code /} and a leading
         * {@code /} is added if missing. Resets to {@code "/"} when {@code null}.
         *
         * @param path the path string
         * @return this builder
         */
        public Builder path(final String path) {
            if (path != null)  {
                String _path = path.replace("\\", "/");
                if (!_path.startsWith("/")) {
                    _path = "/" + path;
                }
                httpUrl.path = _path.strip();
            }
            else {
                httpUrl.path = "/";
            }
            return this;
        }
        
        /**
         * Sets the query parameters from an {@link HttpParameter} instance.
         *
         * @param httpParameter the parameters, or {@code null} to clear them
         * @return this builder
         */
        public Builder parameter(final HttpParameter httpParameter) {
        	httpUrl.httpParameter = httpParameter;
            return this;
        }

        /**
         * Sets the query parameters by parsing a raw query string (e.g. {@code "foo=bar&baz=1"}).
         *
         * @param parameterQuery the raw query string, or {@code null}/{@code ""} to clear parameters
         * @return this builder
         */
        public Builder parameterQuery(final String parameterQuery) {
        	httpUrl.httpParameter = !StringUtil.isNullOrBlank(parameterQuery)
        	                           ? HttpParameter.parse(parameterQuery)
        	                           : null;
            return this;
        }
        
        /**
         * Sets the URL fragment (the part after {@code #}).
         *
         * @param fragment the fragment value, or {@code null} to omit it
         * @return this builder
         */
        public Builder fragment(final String fragment) {
        	httpUrl.fragment = fragment;
            return this;
        }
        
        /**
         * Builds and returns the {@link HttpUrl} instance.
         *
         * @return the constructed URL
         */
        public HttpUrl build() {
        	return httpUrl;
        }
    	
    }
    
}
