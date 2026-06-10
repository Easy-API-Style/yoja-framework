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
package com.easygoingapi.yoja.reverse.proxy;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.easygoingapi.yoja.core.util.PathUtil;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url.From;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url.To;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Declarative mapping {@code from} an inbound URL pattern {@code to} a set of
 * upstream targets.
 * <p>
 * A rule is identified by its {@link Url.From} (host + optional path); two
 * rules are equal when their {@code from} matches, so the rule table behaves
 * as a map keyed by {@code from.id()}. The target {@link Url.To} carries the
 * destination host, one or more ports (load-balanced by the switcher), an SSL
 * flag, and optional path-rewriting directives ({@code cutsPathWith},
 * {@code startsPathWith}).
 * <p>
 * Rules are JSON-serializable through Jackson annotations so that they can be
 * pushed dynamically via the {@link ReverseProxySwitcher} admin endpoints.
 * <p>
 * Path constraints checked at construction time:
 * <ul>
 *   <li>{@code from.path}, {@code cutsPathWith} and {@code startsPathWith}
 *       must begin with {@code /} when present;</li>
 *   <li>when both {@code from.path} and {@code cutsPathWith} are set,
 *       {@code from.path} must start with {@code cutsPathWith}.</li>
 * </ul>
 * Violations throw {@link ReverseProxyException}.
 */
@JsonPropertyOrder({"from", "to"})
public class ReverseProxyRule {

    @JsonProperty("from")
    private final From from;
    @JsonProperty("to")
    private final To to;

    /**
     * Jackson-deserializable constructor.
     *
     * @param from inbound URL pattern (must not be {@code null})
     * @param to   upstream target description (must not be {@code null})
     * @throws ReverseProxyException when paths violate the rule constraints
     */
    @JsonCreator
    public ReverseProxyRule(@JsonProperty("from") final From from,
                            @JsonProperty("to") final To to) {
        super();
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        checkRule(from, to);
        this.from = from;
        this.to = to;
    }

    /**
     * Validates the path-related invariants described in the class Javadoc.
     *
     * @param from inbound pattern
     * @param to   target description
     * @throws ReverseProxyException when any invariant is violated
     */
    private void checkRule(final From from, final To to) {
        boolean good = true;
        if (from.path() != null
                && !from.path().startsWith("/")) {
            good = false;
        }
        if (to.cutsPathWith != null
                && !to.cutsPathWith.startsWith("/")) {
            good = false;
        }
        if (to.startsPathWith != null
                && !to.startsPathWith.startsWith("/")) {
            good = false;
        }
        if (from.path() != null
                && to.cutsPathWith != null) {
            good = from.path().startsWith(to.cutsPathWith);
        }
        if (!good) {
            final StringBuilder error = new StringBuilder();
            error.append("reserve proxy rule error");
            error.append(" from: ");
            error.append(from);
            error.append(" to: ");
            error.append(to);
            throw new ReverseProxyException(error.toString());
        }
    }

    /**
     * Returns the inbound URL pattern.
     *
     * @return the inbound URL pattern
     */
    public From from() {
        return from;
    }

    /**
     * Returns the upstream target description.
     *
     * @return the upstream target description
     */
    public To to() {
        return to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from);
    }

    /** Equality is defined only by {@link #from()} (rules form a {@code From}-keyed map). */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ReverseProxyRule other = (ReverseProxyRule) obj;
        return Objects.equals(from, other.from);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(ReverseProxyRule.class.getSimpleName());
        result.append(" [from=");
        result.append(from);
        result.append(", to=");
        result.append(to);
        result.append("]");
        return result.toString();
    }

    /*
     *
     * CLASS
     *
     */
    /**
     * Namespace gathering the {@link From} and {@link To} value types and the
     * factories used to build them.
     */
    public static class Url {

        /** Not instantiable; use the static factories. */
        private Url() {
            super();
        }

        /*
         *
         * CONSTRUCTOR
         *
         */
        /**
         * Returns a new {@link From} value for the given host and optional path prefix.
         *
         * @param host inbound host (must not be {@code null})
         * @param path inbound path prefix (may be {@code null})
         * @return a new {@link From} value
         */
        public static From from(final String host,
                                final String path) {
            return new From(host, path);
        }

        /**
         * Returns a new {@link From} for the given host with no path constraint.
         *
         * @param host inbound host (must not be {@code null})
         * @return a new {@link From} with no path constraint
         */
        public static From from(final String host) {
            return new From(host, null);
        }

        /**
         * Returns a builder for a {@link To} targeting the given host.
         *
         * @param ssl  whether upstream uses TLS
         * @param host upstream host
         * @return a builder for {@link To}
         */
        public static To.Builder to(final boolean ssl,
                                    final String host) {
            return new To.Builder(ssl, host);
        }

        /**
         * Inbound URL pattern: a host plus an optional path prefix.
         * <p>
         * The {@link #id()} string (host + path or host + {@code /}) acts as
         * the lookup key in the switcher's rule table.
         */
        @JsonPropertyOrder({"host", "path"})
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class From {

            @JsonProperty("host")
            private final String host;
            @JsonProperty("path")
            private final String path;

            /**
             * Jackson-deserializable constructor.
             *
             * @param host inbound host (must not be {@code null})
             * @param path inbound path prefix (may be {@code null})
             * @throws ReverseProxyException when {@code path} is set but does not begin with {@code /}
             */
            @JsonCreator
            private From(@JsonProperty("host") final String host,
                         @JsonProperty("path") final String path) {
                super();
                Objects.requireNonNull(host, "needs host");
                checkPath(path);
                this.host = host;
                this.path = PathUtil.formatPath(path);
            }

            /**
             * Returns the rule key used as a lookup key in the switcher's table.
             *
             * @return the rule key (host + path, or host + {@code /} when no
             *         path was provided)
             */
            protected String id() {
                final StringBuilder result = new StringBuilder(host);
                if (path != null) {
                    result.append(path);
                }
                else {
                    result.append("/");
                }
                return result.toString();
            }

            /**
             * Returns the optional path prefix (may be {@code null}).
             *
             * @return the optional path prefix (may be {@code null})
             */
            public String path() {
                return path;
            }

            @Override
            public int hashCode() {
                return Objects.hash(host, path);
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                final From other = (From) obj;
                return Objects.equals(host, other.host)
                        && Objects.equals(path, other.path);
            }

            @Override
            public String toString() {
                final StringBuilder result = new StringBuilder();
                result.append(From.class.getSimpleName());
                result.append(" [host=");
                result.append(host);
                result.append(", path=");
                result.append(path);
                result.append("]");
                return result.toString();
            }

        }

        /**
         * Upstream target description.
         * <p>
         * Holds the destination host, one or more ports (the switcher picks
         * the least-used port for each request), the TLS flag, and two
         * optional path-rewriting steps applied in order by {@link #apply(String)}:
         * <ol>
         *   <li>{@code cutsPathWith}: strip the given prefix off the inbound path;</li>
         *   <li>{@code startsPathWith}: re-prepend a new prefix on the result.</li>
         * </ol>
         */
        @JsonPropertyOrder({"ssl", "host", "ports", "cutsPathWith", "startsPathWith"})
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class To {

            @JsonProperty("ssl")
            private final boolean ssl;
            @JsonProperty("host")
            private final String host;
            @JsonProperty("ports")
            private final Set<Integer> ports;
            @JsonProperty("cutsPathWith")
            private final String cutsPathWith;
            @JsonProperty("startsPathWith")
            private final String startsPathWith;

            /**
             * Jackson-deserializable constructor.
             *
             * @param ssl            whether upstream uses TLS
             * @param host           upstream host
             * @param ports          upstream ports (load-balanced when more than one)
             * @param cutsPathWith   prefix to strip from the inbound path (must start with {@code /})
             * @param startsPathWith prefix to prepend to the resulting path (must start with {@code /})
             */
            @JsonCreator
            private To(@JsonProperty("ssl") final boolean ssl,
                       @JsonProperty("host") final String host,
                       @JsonProperty("ports") final Set<Integer> ports,
                       @JsonProperty("cutsPathWith") final String cutsPathWith,
                       @JsonProperty("startsPathWith") final String startsPathWith) {
                super();
                this.ssl = ssl;
                this.host = host;
                this.ports = ports;
                this.cutsPathWith = PathUtil.formatPath(cutsPathWith);
                this.startsPathWith = PathUtil.formatPath(startsPathWith);
            }

            /**
             * Returns whether the upstream uses TLS.
             *
             * @return whether the upstream uses TLS
             */
            public boolean ssl() {
                return ssl;
            }

            /**
             * Returns the upstream host.
             *
             * @return the upstream host
             */
            public String host() {
                return host;
            }

            /**
             * Returns the set of upstream ports (load-balanced when more than one).
             *
             * @return the set of upstream ports (load-balanced when more than one)
             */
            public Set<Integer> ports() {
                return ports;
            }

            /**
             * Returns the optional path prefix to strip from inbound paths.
             *
             * @return the optional path prefix to strip from inbound paths
             */
            public String cutsPathWith() {
                return cutsPathWith;
            }

            /**
             * Returns the optional path prefix to prepend after stripping.
             *
             * @return the optional path prefix to prepend after stripping
             */
            public String startsPathWith() {
                return startsPathWith;
            }

            /**
             * Applies the configured path rewriting on the supplied inbound
             * path: optional {@code cutsPathWith} strip, then optional
             * {@code startsPathWith} prepend, and finally ensures the result
             * starts with {@code /}.
             *
             * @param path inbound path (must start with {@code /})
             * @return the rewritten upstream path
             * @throws ReverseProxyException when {@code path} does not begin
             *         with {@code cutsPathWith}
             */
            public String apply(final String path) {
            	String result = path;
                final String cutsPath = cutsPathWith();
                if (cutsPath != null) {
                    if (path.startsWith(cutsPath)) {
                        result = PathUtil.formatPath(Path.of(cutsPathWith()).relativize(Path.of(result)));
                    }
                    else {
                        throw new ReverseProxyException("to cut: the path must begin with " + cutsPath + " -> " + path);
                    }
                }
                final String startsPathWith = startsPathWith();
                if (startsPathWith != null) {
                    if (result.startsWith("/")) {
                        result = PathUtil.formatPath(Path.of(startsPathWith).resolve(result.toString().substring(1)));
                    }
                    else {
                        result = PathUtil.formatPath(Path.of(startsPathWith).resolve(result));
                    }
                }
                if (!result.startsWith("/")) {
                    result = "/" + result.toString();
                }
                return result;
            }

            @Override
            public int hashCode() {
                return Objects.hash(ssl, host, ports, cutsPathWith, startsPathWith);
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                final To other = (To) obj;
                return Objects.equals(ssl, other.ssl)
                        && Objects.equals(host, other.host)
                        && Objects.equals(ports, other.ports)
                        && Objects.equals(cutsPathWith, other.cutsPathWith)
                        && Objects.equals(startsPathWith, other.startsPathWith);
            }

            @Override
            public String toString() {
                final StringBuilder result = new StringBuilder();
                result.append(To.class.getSimpleName());
                result.append(" [ssl=");
                result.append(ssl);
                result.append(", host=");
                result.append(host);
                result.append(", ports=");
                result.append(ports);
                result.append(", cutsPathWith=");
                result.append(cutsPathWith);
                result.append(", startsPathWith=");
                result.append(startsPathWith);
                result.append("]");
                return result.toString();
            }

            /*
             *
             * BUILDER
             *
             */
            /**
             * Fluent builder for {@link To}.
             * <p>
             * Hosts and ports are validated eagerly: every supplied port must
             * be positive, and the supplied path prefixes must start with
             * {@code /}.
             */
            public static class Builder {

                private final boolean ssl;
                private final String host;
                private String cutsPathWith;
                private String startsPathWith;
                private Set<Integer> ports;

                /**
                 * @param ssl  whether upstream uses TLS
                 * @param host upstream host (must not be {@code null})
                 */
                private Builder(final boolean ssl,
                                final String host) {
                    super();
                    Objects.requireNonNull(host, "needs host");
                    this.ssl = ssl;
                    this.host = host;
                }

                /**
                 * Sets the prefix to strip from inbound paths.
                 *
                 * @param cutsPath path prefix (must start with {@code /})
                 * @return this builder
                 */
                public Builder cutsPathWith(final String cutsPath) {
                    checkPath(cutsPath);
                    this.cutsPathWith = cutsPath;
                    return this;
                }

                /**
                 * Sets the prefix to prepend after stripping.
                 *
                 * @param startsPathWith path prefix (must start with {@code /})
                 * @return this builder
                 */
                public Builder startsPathWith(final String startsPathWith) {
                    checkPath(startsPathWith);
                    this.startsPathWith = startsPathWith;
                    return this;
                }

                /**
                 * Sets the upstream port set (load-balanced when more than one).
                 *
                 * @param ports upstream ports (every entry must be positive)
                 * @return this builder
                 */
                public Builder ports(final Set<Integer> ports) {
                    checkPorts(ports);
                    this.ports = ports;
                    return this;
                }

                /**
                 * Convenience overload taking varargs.
                 *
                 * @param port      first upstream port (must be positive)
                 * @param morePorts additional ports
                 * @return this builder
                 */
                public Builder port(final int port, final int... morePorts) {
                    final Set<Integer> ports = new HashSet<>();
                    ports.add(port);
                    if (morePorts != null) {
                        for (final int morePort : morePorts) {
                            ports.add(morePort);
                        }
                    }
                    return ports(ports);
                }

                /**
                 * Returns the immutable {@link To} built from this builder's configuration.
                 *
                 * @return the immutable {@link To}
                 */
                public To build() {
                    return new To(ssl, host, ports, cutsPathWith, startsPathWith);
                }

            }

        }

    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Validates that every entry of {@code ports} is a positive integer.
     *
     * @param ports port set (may be {@code null})
     * @throws ReverseProxyException when any port is non-positive
     */
    private static void checkPorts(final Set<Integer> ports) {
        if (ports != null) {
            for (final Integer port : ports) {
                checkPort(port);
            }
        }
    }

    /**
     * @param port port to validate
     * @throws ReverseProxyException when {@code port} is non-positive
     */
    private static void checkPort(final int port) {
        if (port <= 0) {
            throw new ReverseProxyException("port must be positive: " + port);
        }
    }

    /**
     * @param path path to validate (may be {@code null})
     * @throws ReverseProxyException when {@code path} is non-null and does not start with {@code /}
     */
    private static void checkPath(final String path) {
        if (path != null && !path.startsWith("/")) {
            throw new ReverseProxyException("path must be absolute");
        }
    }

}
