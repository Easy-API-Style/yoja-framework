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

import java.util.Comparator;
import java.util.List;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.google.common.collect.Lists;

import io.vertx.core.Handler;

/**
 * Declarative description of a single HTTP endpoint: an HTTP method, a path
 * pattern and an ordered list of handlers invoked when a matching request is
 * received.
 * <p>
 * The path must begin with {@code /}; the constructor will throw an
 * {@link HttpServerException} otherwise. Subclasses such as {@link WebResource}
 * specialize the endpoint for static-content serving and are identified at
 * runtime through {@link #getType()}.
 */
public class WebService {

	/** Tag distinguishing handler-only services from static resources. */
	public static enum Type {
        /** A {@link WebResource} backed by a file/jar entry. */
        WebResource,
        /** A regular {@link WebService} backed only by handlers. */
        WebService;
    }

	/** Natural ordering: by method, then by path. */
	private static final Comparator<WebService> COMPARATOR =
	   Comparator.comparing(WebService::getMethod)
	             .thenComparing(WebService::getPath);

	/** Discriminator computed in the constructor. */
	private final Type type;
	/** HTTP method the endpoint reacts to. */
    private final HttpMethod method;
    /** Path pattern (after normalization). */
    private final String path;
    /** Handlers invoked in registration order on each matched request. */
    private final List<Handler<HttpRouting>> handlers;

    /**
     * Constructs a new service endpoint for the given method and path.
     *
     * @param method   HTTP method (must not be {@code null} to actually match)
     * @param path     path pattern; must begin with {@code /}
     * @param handlers handlers to invoke when a request matches
     * @throws HttpServerException when {@code path} is null or does not begin with {@code /}
     */
    @SafeVarargs
    public WebService(final HttpMethod method,
                      final String path,
                      final Handler<HttpRouting>... handlers) {
        super();
        if (path == null || !HttpRouter.formatPath(path).startsWith("/")) {
            throw new HttpServerException("WebService path must begin with '/'");
        }
        this.type = typeOf(this);
        this.method = method;
        this.path = HttpRouter.formatPath(path);
        this.handlers = Lists.newArrayList(handlers);
    }

    /**
     * Returns {@link Type#WebResource} or {@link Type#WebService}.
     *
     * @return {@link Type#WebResource} or {@link Type#WebService}
     */
    public Type getType() {
    	return type;
    }

    /**
     * Returns the HTTP method (e.g. GET, POST).
     *
     * @return the HTTP method (e.g. GET, POST)
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Returns the normalized path pattern.
     *
     * @return the normalized path pattern
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the live list of handlers registered for this endpoint.
     *
     * @return the live list of handlers registered for this endpoint
     */
    public List<Handler<HttpRouting>> getHandlers() {
        return handlers;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append(" [method=");
        result.append(method);
        result.append(", path=");
        result.append(path);
        result.append(", handlers=");
        result.append(handlers.size());
        result.append("]");
        return result.toString();
    }

    /*
     *
     * STATIC
     *
     */
    /**
     * @param webService the instance to classify
     * @return {@link Type#WebResource} for {@link WebResource} subclasses,
     *         {@link Type#WebService} otherwise; {@code null} when the input is null
     */
    private static Type typeOf(final WebService webService) {
    	Type result = null;
        if (webService != null) {
            result = webService instanceof WebResource
                        ? Type.WebResource
                        : Type.WebService;
        }
        return result;
    }

    /**
     * Compares two services by their natural ordering (method, then path).
     *
     * @param webService_1 first service
     * @param webService_2 second service
     * @return a negative integer, zero, or positive integer as per {@link Comparator#compare}
     */
    public static int compare(final WebService webService_1,
                              final WebService webService_2) {
        return COMPARATOR.compare(webService_1, webService_2);
    }

    /**
     * Returns the natural-order comparator (method, then path).
     *
     * @return the natural-order comparator (method, then path)
     */
    public static Comparator<WebService> comparator() {
        return COMPARATOR;
    }

}
