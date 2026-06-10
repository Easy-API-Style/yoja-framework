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
package com.easygoingapi.yoja.http.server;

import java.util.Map;
import java.util.Set;

import com.easygoingapi.yoja.core.http.HttpHeader;
import com.easygoingapi.yoja.core.http.HttpMethod;

import io.vertx.core.Handler;

/**
 * A {@link WebService} specialized for serving static content read from a
 * {@link WebApp} (a folder on disk or a packaged jar).
 * <p>
 * Always registered against {@link HttpMethod#GET}. The path is rewritten to
 * include the web app's own context path so that resources keep their natural
 * layout under the prefix exposed by the app.
 * <p>
 * Extra {@link HttpHeader} entries supplied at construction time are added to
 * every response unless a previous handler already set the header explicitly.
 */
public class WebResource extends WebService {

    /** The web app the resource is loaded from (folder or jar). */
    private final WebApp webApp;
    /** Default response headers to attach when serving the resource. */
    private final HttpHeader httpHeader;

    /**
     * Convenience constructor with an empty default header set.
     *
     * @param webApp   source web app
     * @param path     path pattern (must begin with {@code /})
     * @param handlers optional pre-send handlers
     */
    @SafeVarargs
    public WebResource(final WebApp webApp,
                       final String path,
                       final Handler<HttpRouting>... handlers) {
        this(webApp, path, new HttpHeader(), handlers);
    }

    /**
     * Constructs a resource endpoint backed by the given web app.
     *
     * @param webApp     source web app
     * @param path       path pattern (must begin with {@code /})
     * @param httpHeader default headers applied to every response
     * @param handlers   optional pre-send handlers
     * @throws HttpServerException when {@code path} is invalid
     */
    @SafeVarargs
    public WebResource(final WebApp webApp,
                       final String path,
                       final HttpHeader httpHeader,
                       final Handler<HttpRouting>... handlers) {
        super(HttpMethod.GET, concatContextPath(webApp, path), handlers);
        this.webApp = webApp;
        this.httpHeader = new HttpHeader(httpHeader);
    }

    /**
     * Returns the source web app.
     *
     * @return the source web app
     */
    public WebApp webApp() {
        return webApp;
    }

    /**
     * Returns {@code true} when any default header is configured.
     *
     * @return {@code true} when any default header is configured
     */
    public boolean hasHeader() {
        return !httpHeader.isEmpty();
    }

    /**
     * Returns the number of default headers configured.
     *
     * @return the number of default headers configured
     */
    public int headerSize() {
        return httpHeader.size();
    }

    /**
     * Returns a snapshot of default headers as a name→value map.
     *
     * @return a snapshot of default headers as a name→value map
     */
    public Map<String, String> headers() {
        return httpHeader.values();
    }

    /**
     * Returns {@code true} when a default header with the given name is set.
     *
     * @param name header name
     * @return {@code true} when a default header with this name is set
     */
    public boolean hasHeader(final String name) {
        return httpHeader.has(name);
    }

    /**
     * Returns the set of configured default header names.
     *
     * @return the set of configured default header names
     */
    public Set<String> headerNames() {
        return httpHeader.names();
    }

    /**
     * Prefixes {@code path} with the web app's context path (when set) to form
     * the actual route pattern.
     *
     * @throws HttpServerException when {@code path} is null or does not start with {@code /}
     */
    private static String concatContextPath(final WebApp webApp,
                                            final String path) {
        if (path == null || !HttpRouter.formatPath(path).startsWith("/")) {
            throw new HttpServerException("WebResource path must begin with '/'");
        }
        final String result;
        if (webApp.contextPath() != null) {
            result = HttpRouter.formatPath(webApp.contextPath(), HttpRouter.cleanPath(path));
        }
        else {
            result = HttpRouter.formatPath(path);
        }
        return result;
    }

}
