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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.ContentType;
import com.easygoingapi.yoja.core.http.HttpHeader;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.Resources;

import io.vertx.core.Handler;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;

/**
 * Central routing entity that maps HTTP requests to {@link WebService} entries
 * and {@link WebResource} static-content endpoints.
 * <p>
 * An instance wraps a Vert.x {@link Router} configured with:
 * <ul>
 *   <li>an optional context-path prefix applied to every registered service;</li>
 *   <li>an optional {@link HttpSessionStore} that adds a session handler to all routes;</li>
 *   <li>a content-type table keyed by file extension, consulted when serving {@link WebResource} bytes;</li>
 *   <li>cross-cutting {@link HttpEvent} hooks (onRequest / onResponse).</li>
 * </ul>
 * Routers are built through {@link Builder} and consumed by {@link HttpServer}.
 * <p>
 * In addition to the routing logic, this class hosts a set of path-manipulation
 * helpers ({@link #formatPath(String)}, {@link #cleanPath(String)},
 * {@link #formatPath(String, String)}) used throughout the package to normalise
 * filesystem and URL paths to forward-slash form.
 */
public class HttpRouter {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpRouter.class);

    /** The underlying Vert.x Web router. */
    private final Router router;
    /** Global URL prefix applied to every route, or {@code null}. */
    private final String contextPath;
    /** Cross-cutting hooks ({@code onRequest} / {@code onResponse}). */
    private final HttpEvent httpEvent;
    /** Session store; when {@code null}, no session handler is installed. */
    private final HttpSessionStore httpSessionStore;
    /** Extension→Content-Type table for static resources. */
    private final Map<String, String> contentTypesByExtension;
    /** Registered services and resources, sorted so longer paths match first. */
    private final List<WebService> webServices;

    /** Private — construct via {@link #builder()}. */
    private HttpRouter(final String contextPath,
                       final HttpEvent httpEvent,
                       final HttpSessionStore httpSessionStore,
                       final Map<String, String> contentTypesByExtension,
                       final List<WebService> webServices) {
        this.router = Router.router(YojaApp.vertx());
        this.contextPath = contextPath;
        this.httpEvent = httpEvent;
        this.httpSessionStore = httpSessionStore;
        this.contentTypesByExtension = contentTypesByExtension;
        this.webServices = sortWebServices(webServices);
        this.initialize();
    }

    /**
     * Returns the global context path, or {@code null} when none.
     *
     * @return the global context path, or {@code null} when none
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Returns the configured session store, or {@code null} when sessions are disabled.
     *
     * @return the configured session store, or {@code null} when sessions are disabled
     */
    public HttpSessionStore getHttpSessionStore() {
        return httpSessionStore;
    }

    /**
     * Returns the live extension→Content-Type map.
     *
     * @return the live extension→Content-Type map
     */
    public Map<String, String> getContentTypes() {
        return contentTypesByExtension;
    }

    /**
     * Returns the list of services and resources in the order they will match.
     *
     * @return the list of services and resources in the order they will match
     */
    public List<WebService> getWebServices() {
        return webServices;
    }

    /**
     * Returns {@code true} when the given resource exists in the web app.
     *
     * @param webApp web app holding the resource
     * @param path   resource path (must begin with {@code /})
     * @return {@code true} when the resource exists in the web app
     */
    public boolean hasResource(final WebApp webApp,
                               final String path) {
        return exists(webApp, resolve(webApp, path));
    }

    /**
     * Reads a resource's bytes from the supplied web app.
     *
     * @param webApp web app holding the resource
     * @param path   resource path
     * @return the bytes, or {@code null} when the resource is missing
     */
    public byte[] loadResource(final WebApp webApp,
                               final String path) {
        return read(webApp, resolve(webApp, path));
    }

    /**
     * Returns the wrapped Vert.x Web router (server-internal access).
     *
     * @return the wrapped Vert.x Web router (server-internal access)
     */
    protected Router getRouter() {
        return router;
    }

    /**
     * Returns the cross-cutting event handlers (router-internal access).
     *
     * @return the cross-cutting event handlers (router-internal access)
     */
    protected HttpEvent getHttpEvent() {
        return httpEvent;
    }

    /**
     * Wires every route on the underlying Vert.x router based on the
     * configured services and resources. For each entry:
     * <ol>
     *   <li>a {@link BodyHandler} is installed for {@code POST} services;</li>
     *   <li>a blocking handler runs the {@code onRequest} pipeline and may abort with 444;</li>
     *   <li>each user-supplied handler is invoked, with uncaught exceptions
     *       routed to {@link io.vertx.ext.web.RoutingContext#fail(Throwable)};</li>
     *   <li>for {@link WebResource} entries, a final handler reads the bytes
     *       from disk/jar and runs the {@code onResponse} pipeline before sending.</li>
     * </ol>
     */
    private void initialize() {
        if (httpSessionStore != null) {
            router.route().handler(createSessionHandler(httpSessionStore.cookieName(),
                                                        httpSessionStore.timeout()));
        }
        for (final WebService webService : webServices) {
        	final WebService.Type webServiceType = webService.getType();
            final HttpMethod httpMethod = webService.getMethod();
            final Route route = router.route(io.vertx.core.http.HttpMethod.valueOf(httpMethod.name()),
                                             formatPathWithContextPath(webService.getPath()));
            if (HttpMethod.POST == httpMethod) {
                route.handler(BodyHandler.create());
            }
            route.blockingHandler(routingContext -> {
            	final HttpRoutingContext httpRoutingContext = new HttpRoutingContext(webServiceType, routingContext, this);
            	final HttpRequestEvent httpRequestEvent = new HttpRequestEvent(httpRoutingContext);
            	for (final Handler<HttpRequestEvent> action : httpEvent().getOnRequestActions()) {
        			action.handle(httpRequestEvent);
        		}
            	if (httpRequestEvent.aborted()) {
            	    routingContext.response()
            	                  .setStatusCode(444)
            	                  .end();
            	}
            	else {
            	    routingContext.next();
            	}
            }, false);
            for (final Handler<HttpRouting> handler : webService.getHandlers()) {
                route.blockingHandler(routingContext -> {
                    try {
                        handler.handle(new HttpRouting(webServiceType, routingContext, this));
                    }
                    catch (final Exception e) {
                        final HttpServerResponse httpServerResponse = routingContext.response();
                        if (httpServerResponse.ended()) {
                            httpServerResponse.reset();
                        }
                        routingContext.fail(e);
                    }
                }, false);
            }
            if (webService instanceof WebResource) {
                route.blockingHandler(routingContext -> {
                    try {
                        sendWebResource((WebResource) webService, new HttpRouting(webServiceType, routingContext, this));
                    }
                    catch (final Exception e) {
                        final HttpServerResponse httpServerResponse = routingContext.response();
                        if (httpServerResponse.ended()) {
                            httpServerResponse.reset();
                        }
                        routingContext.fail(e);
                    }
                }, false);
            }
        }
    }

    /**
     * Sorts services so that more specific paths (lexicographically larger
     * strings) match first; ties on path are broken by HTTP method.
     */
    private static List<WebService> sortWebServices(final List<WebService> webServices) {
        final Multimap<String, WebService> sortedWebServices = MultimapBuilder.treeKeys((v_1, v_2) -> v_2.toString().compareTo(v_1.toString()))
                                                                              .arrayListValues()
                                                                              .build();
        for (final WebService webService : webServices) {
            sortedWebServices.put(webService.getPath() + "||" + webService.getMethod().name(), webService);
        }
        return new ArrayList<>(sortedWebServices.values());
    }

    /**
     * Loads the bytes of the matched {@link WebResource}, runs the
     * {@code onResponse} pipeline (which may swap or abort the body), then
     * writes the result with the proper {@code Content-Type} resolved from
     * the configured extension table.
     */
    private void sendWebResource(final WebResource webResource,
    		                     final HttpRouting httpRouting) {
        final HttpRequest httpRequest = httpRouting.request();
        final String path = httpRequest.path();
        String contentType = null;
        for (final Entry<String, String> entry : contentTypesByExtension.entrySet()) {
            if (path.endsWith("." + entry.getKey())) {
                contentType = entry.getValue();
            }
        }
        final HttpResponse httpResponse = httpRouting.response();

        if (contentType != null) {
            httpResponse.putHeader(ContentType.key, contentType);
        }
        for (final Entry<String, String> header : webResource.headers().entrySet()) {
            if (!httpResponse.hasHeader(header.getKey())) {
                httpResponse.putHeader(header.getKey(), header.getValue());
            }
        }
        LOGGER.debug("load {}", path);

        final WebApp webApp = webResource.webApp();
        LOGGER.debug("webApp {}", webApp);LOGGER.debug("webApp {}", webApp);LOGGER.debug("webApp {}", webApp);
        Object httpBody = read(webApp, path);
    	final HttpRoutingContext httpRoutingContext =
    		new HttpRoutingContext(WebService.Type.WebResource,
                                   httpRouting.context(),
                                   this);
    	final HttpResponseEvent httpResponseEvent = new HttpResponseEvent(httpRoutingContext, httpBody);
    	if (httpBody != null) {
    		httpResponseEvent.statusCode(200);
        }
        else {
        	httpResponseEvent.statusCode(404);
        }
		for (final Handler<HttpResponseEvent> action : httpEvent().getOnResponseActions()) {
			action.handle(httpResponseEvent);
			httpBody = httpResponseEvent.body();
		}
		if (!httpResponseEvent.aborted()
				 && !httpResponse.sent()) {
			httpResponseEvent.sendBodyWithContentType(httpBody);
		}
    }

    /*
     *
     * SESSION
     *
     */
    /**
     * Builds the Vert.x session handler bound to the router's
     * {@link HttpSessionStore} with {@link CookieSameSite#STRICT}.
     */
    private SessionHandler createSessionHandler(final String cookieName,
                                                final Duration timeout) {
        final SessionHandler sessionHandler = SessionHandler.create(httpSessionStore.localSessionStore());
        sessionHandler.setSessionCookieName(cookieName);
        sessionHandler.setCookieSameSite(CookieSameSite.STRICT);
        if (timeout != null) {
            sessionHandler.setSessionTimeout(timeout.toMillis());
        }
        return sessionHandler;
    }

    /*
     *
     * contextPath
     *
     */
    /**
     * Resolves a resource path against the effective context path (the router's
     * context path concatenated with the web app's own context path when both are set).
     *
     * @param webApp web app that owns the resource
     * @param path   resource path
     * @return the resolved absolute path
     * @throws HttpServerException when {@code path} does not begin with {@code /}
     */
    protected String resolve(final WebApp webApp,
                             final String path) {
        if (path == null || !HttpRouter.formatPath(path).startsWith("/")) {
            throw new HttpServerException("resource path must begin with '/'");
        }
        final String result;
        final String contexPath = contextPath(webApp);
        if (contexPath != null) {
            result = formatPath(contexPath, path.substring(1));
        }
        else {
            result = formatPath(path);
        }
        return result;
    }

    /**
     * Computes the effective context path for the given web app by combining
     * the router's context path with the app's own.
     *
     * @param webApp web app whose own context path is to be combined
     * @return the combined path, or {@code null} when neither is configured
     */
    protected String contextPath(final WebApp webApp) {
        final String result;
        if (!Strings.isNullOrEmpty(contextPath)
              && !Strings.isNullOrEmpty(webApp.contextPath())) {
            result = formatPath(contextPath, webApp.contextPath().substring(1));
        }
        else if (!Strings.isNullOrEmpty(contextPath)) {
            result = contextPath;
        }
        else if (!Strings.isNullOrEmpty(webApp.contextPath())) {
            result = webApp.contextPath();
        }
        else {
            result = null;
        }
        return result;
    }

    /**
     * Prepends the router's context path to a service-level path.
     *
     * @param path service-level path
     * @return the path with the router's context path prepended
     */
    protected String formatPathWithContextPath(final String path) {
        final String result;
        if (!Strings.isNullOrEmpty(contextPath)) {
            result = formatPath(contextPath, path.substring(1));
        }
        else {
            result = formatPath(path);
        }
        return result;
    }

    /*
     *
     * RESOURCE
     *
     */
    /**
     * Reads a resource's bytes from the appropriate backend based on the web
     * app's {@link WebApp.Type}.
     *
     * @param webApp web app to read from
     * @param path   resource path
     * @return the bytes, or {@code null} when missing
     */
	protected byte[] read(final WebApp webApp,
			              final String path) {
		final byte[] result;
		if (WebApp.Type.jar == webApp.type()) {
			result = loadFromJar(webApp, path);
        }
        else {
        	result = loadFromFileSystem(webApp, path);
        }
		return result;
	}


    /*
     *
     * file
     *
     */
    /** Reads bytes for a jar-backed resource via the classloader. */
    private byte[] loadFromJar(final WebApp webApp,
                               final String path) {
		byte[] result = null;
		try {
			if (isFileWithExtension(path)) {
				final URL url = toURLFromJar(webApp, path);
//				if (isExistingFile(url)) {
					result = loadByteArrayFromJar(url);
//				}
			}
//			else {
//			    throw new HttpServerException("jar resource file must have extension");
//			}
		}
		catch (final Exception e) {
			LOGGER.warn("load resource not existing {}", path);
//			LOGGER.error("load resource failed {}", path, e);
		}
		return result;
    }

    /** Reads bytes for a folder-backed resource straight from the filesystem. */
    private byte[] loadFromFileSystem(final WebApp webApp,
				                          final String path) {
		byte[] result = null;
		try {
			if (exists(webApp, path)) {
				result = loadByteArrayFromFileSystem(webApp, path);
			}
		}
		catch (final Exception e) {
			LOGGER.error("load resource failed {}", path, e);
		}
		return result;
    }

    /**
     * Resource existence check that dispatches to the proper backend based on
     * {@link WebApp#type()}.
     */
    private boolean exists(final WebApp webApp,
                           final String path) {
        final boolean result;
        if (WebApp.Type.jar == webApp.type()) {
            result = isFileWithExtension(path)
            		   && isExistingFile(toURLFromJar(webApp, path));
        }
        else {
            final String _path = relativize(contextPath(webApp), path);
            final Path file = Path.of(webApp.path(), cleanPath(_path));
            return Files.exists(file)
            		  && !Files.isDirectory(file);
        }
        return result;
    }

    /**
     * Turns a logical resource path into a classloader URL for a jar-backed
     * web app (the dotted path is rewritten to a slashed one).
     */
    private URL toURLFromJar(final WebApp webApp,
                             final String path) {
    	final String _path = relativize(contextPath(webApp), path);
        final String fullPath = webApp.path().replace(".", "/")
                                   + "/" + cleanPath(_path);
        return HttpServer.class
        		         .getClassLoader()
        		         .getResource(formatPath(fullPath));
    }

    /**
     * Reads bytes from a folder-backed resource and wraps I/O failures as
     * {@link HttpServerException}.
     */
    private byte[] loadByteArrayFromFileSystem(final WebApp webApp,
                                               final String path) {
        try {
        	final String _path = relativize(contextPath(webApp), path);
            final Path file = Path.of(webApp.path(), cleanPath(_path));
            return Files.readAllBytes(file);
        }
        catch (final Exception e) {
            throw new HttpServerException("load resource failed " + path, e);
        }
    }

    /**
     * Reads all bytes of a classpath URL and wraps I/O failures as
     * {@link HttpServerException}.
     */
    private static byte[] loadByteArrayFromJar(final URL url) {
        try {
            return Resources.toByteArray(url);
        }
        catch (final Exception e) {
            throw new HttpServerException("load resource failed " + url, e);
        }
    }

    /**
     * @return {@code true} when the URL points to a regular existing file
     *         (folder paths and exceptions both yield {@code false})
     */
    private static boolean isExistingFile(final URL url) {
        boolean result = false;
        try {
            if (url != null) {
                final Path path = Paths.get(url.toURI());
                return Files.exists(path)
                		  && !Files.isDirectory(path);
            }
        }
        catch (final Exception e) {
            // do nothing
        }
        return result;
    }

    /** @return {@code true} when the final segment of {@code path} contains a dot. */
    private static boolean isFileWithExtension(final String path) {
        return Paths.get(path)
       		        .getFileName()
       		        .toString()
       		        .contains(".");
    }

    /**
     * Removes a context-path prefix from a resource path.
     *
     * @param contexPath context-path prefix (may be {@code null})
     * @param path       resource path to strip
     * @return the path relative to {@code contexPath}, or {@code path} unchanged
     */
    private static String relativize(final String contexPath,
    		                         final String path) {
        String result = path;
        if (contexPath != null) {
            result = formatPath(Path.of(contexPath).relativize(Path.of(result)));
        }
    	return result;
    }

    /**
     * Strips a leading slash and normalizes separators on a {@link Path}.
     *
     * @param path path to normalize
     * @return the cleaned, forward-slashed path
     */
    public static String cleanPath(final Path path) {
    	return cleanPath(path.toString());
    }

    /**
     * Strips a leading slash and normalizes separators on a string path.
     *
     * @param path path to normalize (may be {@code null})
     * @return the cleaned, forward-slashed path, or {@code null}
     */
    public static String cleanPath(final String path) {
    	final String result;
    	if (path != null) {
    	    if (path.startsWith("/")) {
                result = path.substring(1, path.length());
            }
            else {
                result = path;
            }
    	}
    	else {
    	    result = null;
    	}
        return formatPath(result);
    }

    /**
     * Concatenates a context path with a path while preserving a trailing
     * {@code *} wildcard and forcing forward-slash separators.
     *
     * @param contextPath context-path prefix
     * @param path        path to append (may end with {@code *})
     * @return the joined, normalized path
     */
    public static String formatPath(final String contextPath,
    		                        final String path) {
    	final boolean endsWithStar = path.strip().endsWith("*");
    	String result = Path.of(contextPath, path.replace("*", ""))
     		                .toString()
     		                .replace("\\", "/");
        if (endsWithStar) {
        	if (!result.endsWith("/")) {
        		result = result + "/";
        	}
        	result = result + "*";
        }
        return result;
    }

    /**
     * Replaces backslashes with forward slashes.
     *
     * @param path path to normalize (may be {@code null})
     * @return the normalized path, or {@code null}
     */
    public static String formatPath(final String path) {
        if (path != null) {
            return path.replace("\\", "/");
        }
        return null;
    }

    /**
     * {@link Path} overload of {@link #formatPath(String)}.
     *
     * @param path path to normalize (may be {@code null})
     * @return the normalized path, or {@code null}
     */
    public static String formatPath(final Path path) {
        if (path != null) {
            return formatPath(path.toString());
        }
        return null;
    }

    /*
     *
     * PROTECTED
     *
     */
    /**
     * Returns the cross-cutting event handlers.
     *
     * @return the cross-cutting event handlers
     */
    protected HttpEvent httpEvent() {
    	return httpEvent;
    }


    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpRouter.class.getSimpleName());
        result.append(" [contextPath=");
        result.append(contextPath);
        result.append(", httpEvent=");
        result.append(httpEvent);
        result.append(", httpSessionStore=");
        result.append(httpSessionStore);
        result.append(", contentTypesByExtension=");
        result.append(contentTypesByExtension.size());
        result.append(", webServices=");
        result.append(webServices.size());
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder with default (empty) configuration.
     *
     * @return a new builder with default (empty) configuration
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link HttpRouter}.
     * <p>
     * Collects services, resources, content-type mappings, session store and
     * cross-cutting event handlers, then materializes them into a router via
     * {@link #build()}.
     */
    public static class Builder {

        /** Optional URL prefix applied to every service. */
        private String contextPath;
        /** Optional session store; when {@code null}, sessions are disabled. */
        private HttpSessionStore httpSessionStore;
        /** Cross-cutting event handlers (always non-null). */
        private HttpEvent httpEvent = new HttpEvent();
        /** Extension → Content-Type mapping for static resources. */
        private Map<String, String> contentTypesByExtension = new HashMap<>();
        /** Accumulator for services and resources to wire on the router. */
        private List<WebService> webServices = new ArrayList<>();

        /** Private — use {@link HttpRouter#builder()}. */
        private Builder() {
            super();
        }

        /**
         * Attaches a session store; the router will install a session handler.
         *
         * @param httpSessionStore session store to use
         * @return this builder
         */
        public Builder session(final HttpSessionStore httpSessionStore) {
            this.httpSessionStore = httpSessionStore;
            return this;
        }

        /**
         * Sets a global URL prefix applied to every route.
         *
         * @param contextPath URL prefix
         * @return this builder
         */
        public Builder contextPath(final String contextPath) {
            this.contextPath = formatPath(contextPath);
            return this;
        }

        /**
         * Registers an extension → Content-Type mapping for static resources.
         *
         * @param extension   file extension (without dot)
         * @param contentType MIME type
         * @return this builder
         */
        public Builder contentType(final String extension,
                                   final String contentType) {
            this.contentTypesByExtension.put(extension, contentType);
            return this;
        }

        /**
         * Registers a batch of extension → Content-Type mappings.
         *
         * @param contentTypes mappings to register
         * @return this builder
         */
        public Builder contentTypes(final Map<String, String> contentTypes) {
            this.contentTypesByExtension.putAll(contentTypes);
            return this;
        }

        /**
         * Adds a {@link WebResource} with custom default headers.
         *
         * @param WebApp     source web app
         * @param path       path pattern (must begin with {@code /})
         * @param httpHeader default headers
         * @param handlers   optional pre-send handlers
         * @return this builder
         */
        @SafeVarargs
        public final Builder webResource(final WebApp WebApp,
                                         final String path,
                                         final HttpHeader httpHeader,
                                         final Handler<HttpRouting>... handlers) {
            if (path != null) {
                this.webServices.add(new WebResource(WebApp, path, httpHeader, handlers));
            }
            return this;
        }

        /**
         * Adds a {@link WebResource} with an empty default header set.
         *
         * @param WebApp   source web app
         * @param path     path pattern (must begin with {@code /})
         * @param handlers optional pre-send handlers
         * @return this builder
         */
        @SafeVarargs
        public final Builder webResource(final WebApp WebApp,
                                         final String path,
                                         final Handler<HttpRouting>... handlers) {
            return webResource(WebApp, path,  new HttpHeader(), handlers);
        }

        /**
         * Adds a pre-built {@link WebResource}. {@code null} is ignored.
         *
         * @param webResource resource to add
         * @return this builder
         */
        public Builder webResource(final WebResource webResource) {
            if (webResource != null) {
                this.webServices.add(webResource);
            }
            return this;
        }

        /**
         * Adds a service endpoint for the given method and path. Silently
         * ignored when either {@code method} or {@code path} is {@code null}.
         *
         * @param method   HTTP method
         * @param path     path pattern (must begin with {@code /})
         * @param handlers handlers to invoke when the endpoint matches
         * @return this builder
         */
        @SafeVarargs
        public final Builder webService(final HttpMethod method,
                                        final String path,
                                        final Handler<HttpRouting>... handlers) {
            if (method != null && path != null) {
                this.webServices.add(new WebService(method, path, handlers));
            }
            return this;
        }

        /**
         * Adds a pre-built {@link WebService}. {@code null} is ignored.
         *
         * @param webService service to add
         * @return this builder
         */
        public Builder webService(final WebService webService) {
            if (webService != null) {
                this.webServices.add(webService);
            }
            return this;
        }

        /**
         * Adds a batch of services. {@code null} is ignored.
         *
         * @param httpWebServices services to add
         * @return this builder
         */
        public Builder webServices(final List<WebService> httpWebServices) {
            if (httpWebServices != null) {
                this.webServices.addAll(httpWebServices);
            }
            return this;
        }

        /**
         * Registers a cross-cutting handler invoked before each service handler.
         *
         * @param action handler to register
         * @return this builder
         */
        public Builder onRequest(final Handler<HttpRequestEvent> action) {
            httpEvent.onRequest(action);
            return this;
        }

        /**
         * Registers a cross-cutting handler invoked before each response is written.
         *
         * @param action handler to register
         * @return this builder
         */
        public Builder onResponse(final Handler<HttpResponseEvent> action) {
            httpEvent.onResponse(action);
            return this;
        }

        /**
         * Returns the immutable router materialized with this configuration.
         *
         * @return the immutable router materialized with this configuration
         */
        public HttpRouter build() {
            return new HttpRouter(contextPath,
                                  httpEvent,
                                  httpSessionStore,
                                  contentTypesByExtension,
                                  webServices);
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();
            result.append(HttpRouter.class.getSimpleName());
            result.append(".");
            result.append(Builder.class.getSimpleName());
            result.append(" [contextPath=");
            result.append(contextPath);
            result.append(", httpSessionStore=");
            result.append(httpSessionStore);
            result.append(", httpEvent=");
            result.append(httpEvent);
            result.append(", contentTypesByExtension=");
            result.append(contentTypesByExtension);
            result.append(", webServices=");
            result.append(webServices.size());
            result.append("]");
            return result.toString();
        }

    }

}
