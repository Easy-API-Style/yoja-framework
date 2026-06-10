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

import io.vertx.ext.web.RoutingContext;

/**
 * Lightweight wrapper around Vert.x' {@link RoutingContext} exposing the
 * Yoja-flavoured surface (session, resources, per-request data, abort/redirect)
 * but without the request and response objects.
 * <p>
 * Used as the context type for {@code onRequest}/{@code onResponse} event
 * handlers, and as the base class of {@link HttpRouting} which adds the actual
 * {@link HttpRequest}/{@link HttpResponse} pair for service handlers.
 */
public class HttpRoutingContext {

    /** Whether the routed entry is a regular service or a static resource. */
    private final WebService.Type webServiceType;
    /** The underlying Vert.x Web routing context. */
    private final RoutingContext routingContext;
    /** Owning router (used to resolve paths and reach the session store). */
    private final HttpRouter httpRouter;
    /** Session wrapper, or {@code null} when no session handler is configured. */
    private final HttpSession httpSession;

    /**
     * Constructs a routing context wrapping the given Vert.x context.
     *
     * @param webServiceType  whether the matched entry is a service or a resource
     * @param routingContext  the Vert.x Web routing context
     * @param httpRouter      the owning router
     */
    protected HttpRoutingContext(final WebService.Type webServiceType,
    		                     final RoutingContext routingContext,
    		                     final HttpRouter httpRouter) {
    	this.webServiceType = webServiceType;
        this.routingContext = routingContext;
        this.httpRouter = httpRouter;
        this.httpSession = routingContext.session() != null
        		                 ? new HttpSession(routingContext.session())
        		                 : null;
    }

    /**
     * Returns whether this context belongs to a {@link WebService} or {@link WebResource}.
     *
     * @return whether this context belongs to a {@link WebService} or {@link WebResource}
     */
    public WebService.Type webServiceType() {
		return webServiceType;
	}

    /*
     *
     * ABORT
     *
     */
    /**
     * Redirects the current exchange to the given path (resolved against the
     * router's context path).
     *
     * @param path target path
     */
    public void redirect(final String path) {
        context().redirect(httpRouter.formatPathWithContextPath(path));
    }

    /**
     * Fails the exchange with the given HTTP status code; subsequent handlers
     * are skipped and Vert.x' failure handler chain takes over.
     *
     * @param statusCode HTTP status to report
     */
    public void fail(final int statusCode) {
        context().fail(statusCode);
    }

    /**
     * Fails the exchange with the given throwable.
     *
     * @param throwable cause of the failure
     */
    public void fail(final Throwable throwable) {
        context().fail(throwable);
    }

    /**
     * Fails the exchange with both a status code and a cause.
     *
     * @param statusCode HTTP status to report
     * @param throwable  cause of the failure
     */
    public void fail(final int statusCode,
                     final Throwable throwable) {
        context().fail(statusCode, throwable);
    }

    /**
     * Returns {@code true} once any {@code fail(...)} method has been called.
     *
     * @return {@code true} once any {@code fail(...)} method has been called
     */
    public boolean failed() {
    	return context().failed();
    }

	/*
     *
     * SESSION
     *
     */
    /**
     * Returns the session store backing the router, or {@code null} when sessions are disabled.
     *
     * @return the session store backing the router, or {@code null} when sessions are disabled
     */
    public HttpSessionStore sessionStore() {
        return httpRouter.getHttpSessionStore();
    }

    /**
     * Returns the session for the current exchange, or {@code null} when no session exists.
     *
     * @return the session for the current exchange, or {@code null} when no session exists
     */
    public HttpSession session() {
        return httpSession;
    }

    /*
     *
     * RESOURCE
     *
     */
    /**
     * Returns the router-wide context path, or {@code null} when none was configured.
     *
     * @return the router-wide context path, or {@code null} when none was configured
     */
    public String contextPath() {
        return httpRouter.getContextPath();
    }

    /**
     * Returns the effective context path for the given web app, combined with the router's.
     *
     * @param webApp web app whose own context path should be combined with the router's
     * @return the effective context path for the given web app
     */
    public String contextPath(final WebApp webApp) {
        return httpRouter.contextPath(webApp);
    }

    /**
     * Returns {@code true} when the given resource exists in the web app.
     *
     * @param webApp web app to look the resource up in
     * @param path   resource path (must begin with {@code /})
     * @return {@code true} when the resource exists in the web app's filesystem or jar
     */
    public boolean hasResource(final WebApp webApp,
                               final String path) {
        return httpRouter.hasResource(webApp, path);
    }

    /**
     * Reads the bytes of a static resource bundled with the given web app.
     *
     * @param webApp web app to read from (folder or jar)
     * @param path   resource path (must begin with {@code /})
     * @return the resource bytes, or {@code null} when missing
     */
    public byte[] loadResource(final WebApp webApp,
                               final String path) {
        return httpRouter.loadResource(webApp, path);
    }

    /*
     *
     * DATA
     *
     */
    /**
     * Stores a value in the request-scoped data map, visible to subsequent
     * handlers of the same exchange.
     *
     * @param key   data key
     * @param value any value (may be {@code null})
     */
    public void putData(final String key,
                        final Object value) {
        routingContext.put(key, value);
    }

    /**
     * Returns the request-scoped value for the given key (unchecked cast), or {@code null} when absent.
     *
     * @param key data key
     * @param <T> expected value type
     * @return the stored value (unchecked cast), or {@code null} when absent
     */
    public <T> T getData(final String key) {
        return routingContext.get(key);
    }

    /**
     * Returns the request-scoped value for the given key, or {@code defaultValue} when absent.
     *
     * @param key          data key
     * @param defaultValue value returned when the key is missing
     * @param <T>          expected value type
     * @return the stored value or {@code defaultValue}
     */
    public <T> T getData(final String key,
                         final T defaultValue) {
        return routingContext.get(key, defaultValue);
    }

    /**
     * Removes and returns the value associated with {@code key}.
     *
     * @param key data key
     * @param <T> expected value type
     * @return the removed value (unchecked cast), or {@code null} when absent
     */
    public <T> T removeData(final String key) {
        return routingContext.remove(key);
    }

    /**
     * Returns the live request-scoped data map.
     *
     * @return the live request-scoped data map
     */
    public Map<String, Object> data() {
        return routingContext.data();
    }

    /*
     *
     * PROTECTED
     *
     */
    /**
     * Returns the owning router (router-internal access).
     *
     * @return the owning router (router-internal access)
     */
    protected HttpRouter router() {
        return httpRouter;
    }

    /**
     * Returns the wrapped Vert.x Web routing context.
     *
     * @return the wrapped Vert.x Web routing context
     */
    protected RoutingContext context() {
        return routingContext;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpRouting.class.getSimpleName());
        result.append(" [version=");
        result.append(routingContext.request().version().name());
        result.append(", method=");
        result.append(routingContext.request().method().name());
        result.append(", uri=");
        result.append(routingContext.request().absoluteURI());
        result.append("]");
        return result.toString();
    }

}
