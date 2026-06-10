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

import java.util.Objects;

/**
 * Bundle of static resources served by the HTTP server.
 * <p>
 * A web app describes <em>where</em> the resources live (a folder on disk or
 * a packaged jar entry) and <em>under which URL prefix</em> they are exposed.
 * It is paired with one or more {@link WebResource} entries on an
 * {@link HttpRouter} to actually wire the URLs.
 * <p>
 * Instances are immutable; use {@link #builder(Type, String)} or one of the
 * static factories ({@link #folder(String)}, {@link #jar(String)}, {@link #of}).
 */
public class WebApp {

    /** Where resources are loaded from. */
    public static enum Type {
        /** Resources live in a filesystem folder. */
        folder,
        /** Resources are bundled in a jar (classpath lookup). */
        jar
    }

    /** Folder vs jar discriminator. */
    private final Type type;
    /** Filesystem path or classpath base, depending on {@link #type}. */
    private final String path;
    /** URL prefix to mount the resources under (may be {@code null}). */
    private final String contextPath;

    /** Private — use the builder or static factories. */
    private WebApp(final Type type,
                   final String path,
                   final String contextPath) {
        super();
        this.type = type;
        this.path = path;
        this.contextPath = contextPath;
    }

    /**
     * Returns whether the resources are folder- or jar-backed.
     *
     * @return whether the resources are folder- or jar-backed
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the filesystem path or classpath base.
     *
     * @return the filesystem path or classpath base
     */
    public String path() {
        return path;
    }

    /**
     * Returns the URL prefix the resources are mounted under, or {@code null}.
     *
     * @return the URL prefix the resources are mounted under, or {@code null}
     */
    public String contextPath() {
        return contextPath;
    }

	@Override
    public int hashCode() {
        return Objects.hash(contextPath, path, type);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final WebApp other = (WebApp) obj;
        return Objects.equals(contextPath, other.contextPath)
                && Objects.equals(path, other.path)
                && type == other.type;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(WebApp.class.getSimpleName());
        result.append(" [type=");
        result.append(type);
        result.append(", path=");
        result.append(path);
        result.append(", contextPath=");
        result.append(contextPath);
        result.append("]");
        return result.toString();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder for the given store type and base path.
     *
     * @param type kind of resource store
     * @param path filesystem path or classpath base
     * @return a new builder
     */
    public static Builder builder(final Type type,
                                  final String path) {
        return new Builder(type, path);
    }

    /** Fluent builder for {@link WebApp}. */
    public static class Builder {

        /** Resource store kind. */
        private final Type type;
        /** Filesystem path or classpath base. */
        private final String path;

        /** Optional URL prefix; defaults to {@code null}. */
        private String contextPath;

        /**
         * @param type kind of resource store (must not be {@code null})
         * @param path filesystem path or classpath base
         */
        private Builder(final Type type,
                        final String path) {
            super();
            Objects.requireNonNull(type, "needs type");
            this.type = type;
            this.path = HttpRouter.formatPath(path);
        }

        /**
         * Sets the URL prefix to mount the resources under. Must begin with
         * {@code /}.
         *
         * @param contextPath URL prefix (must begin with {@code /})
         * @return this builder
         * @throws HttpServerException when {@code contextPath} is invalid
         */
        public Builder contextPath(final String contextPath) {
            if (path != null && !HttpRouter.formatPath(contextPath).startsWith("/")) {
                throw new HttpServerException("context path must begin with '/'");
            }
            this.contextPath = HttpRouter.formatPath(contextPath);
            return this;
        }

        /**
         * Returns the immutable {@link WebApp}.
         *
         * @return the immutable {@link WebApp}
         */
        public WebApp build() {
            return new WebApp(type, path, contextPath);
        }

    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Shortcut for {@code builder(type, path).build()}.
     *
     * @param type kind of resource store
     * @param path filesystem path or classpath base
     * @return a new {@link WebApp}
     */
    public static WebApp of(final Type type,
                            final String path) {
        return WebApp.builder(type, path).build();
    }

    /**
     * Shortcut for {@code builder(type, path).contextPath(contextPath).build()}.
     *
     * @param type        kind of resource store
     * @param path        filesystem path or classpath base
     * @param contextPath URL prefix to mount the resources under
     * @return a new {@link WebApp}
     */
    public static WebApp of(final Type type,
                            final String path,
                            final String contextPath) {
        return WebApp.builder(type, path)
                     .contextPath(contextPath)
                     .build();
    }

    /**
     * Returns a jar-backed web app with no context path.
     *
     * @param path classpath base
     * @return a jar-backed web app with no context path
     */
    public static WebApp jar(final String path) {
        return WebApp.builder(Type.jar, path).build();
    }

    /**
     * Returns a folder-backed web app with no context path.
     *
     * @param path filesystem path
     * @return a folder-backed web app with no context path
     */
    public static WebApp folder(final String path) {
        return WebApp.builder(Type.folder, path).build();
    }

}
