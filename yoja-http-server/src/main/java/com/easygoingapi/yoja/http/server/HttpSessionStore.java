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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.util.JavaReflectUtil;
import com.google.common.collect.Sets;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.impl.LocalSessionStoreImpl;

/**
 * In-memory store of {@link HttpSession} instances backing the session cookie
 * served by {@link HttpRouter}.
 * <p>
 * Wraps Vert.x' {@link LocalSessionStore}, with two additions:
 * <ul>
 *   <li>the session cookie settings (name, timeout, {@code HttpOnly},
 *       {@code Secure}, {@code SameSite}, {@code Path}, {@code Max-Age}) are
 *       pinned at construction time and exposed on the API -- see
 *       {@link #builder(String)}, whose defaults are the secure ones;</li>
 *   <li>a list of {@code onSession} handlers is invoked every time the store
 *       allocates a fresh session, allowing the application to seed default
 *       attributes or instrument session creation.</li>
 * </ul>
 * The underlying Vert.x {@link LocalMap} is also exposed by reflection so
 * {@link #sessions()} can iterate the live set of sessions (an operation the
 * public Vert.x API does not expose).
 */
public class HttpSessionStore {
    
    /** Default idle timeout of a session. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
    /** Default path of the session cookie: the whole site. */
    public static final String DEFAULT_PATH = "/";

    /** Cookie name used to carry the session id. */
    private final String cookieName;
    /** Session timeout (idle deadline). */
    private final Duration timeout;
    /** Whether the cookie is hidden from JavaScript ({@code HttpOnly}). */
    private final boolean httpOnly;
    /** Whether the cookie is sent over HTTPS only ({@code Secure}). */
    private final boolean secure;
    /** {@code SameSite} attribute of the cookie. */
    private final CookieSameSite sameSite;
    /** {@code Path} attribute of the cookie. */
    private final String path;
    /** {@code Max-Age} of the cookie, or {@code null} for a browser-session cookie. */
    private final Duration maxAge;

    /** Handlers invoked whenever the store creates a fresh session. */
    private final List<Handler<HttpSession>> onSessionActions = new ArrayList<>();

    /** Vert.x' shared-data map backing the session store, accessed by reflection. */
    private final LocalMap<String, Session> localMap;
    /** The wrapped Vert.x session store. */
    private final LocalSessionStore localSessionStore;

    /**
     * Creates a new session store with the given cookie name and idle timeout,
     * and the secure defaults of {@link Builder} for the other cookie settings
     * ({@code HttpOnly}, {@code Secure}, {@code SameSite=Strict}, {@code Path=/}).
     *
     * @param cookieName cookie name used to carry the session id
     * @param timeout    idle timeout after which a session is reaped
     */
    public HttpSessionStore(final String cookieName,
                            final Duration timeout) {
        this(builder(cookieName).timeout(timeout));
    }

    /**
     * Creates a new session store from a validated builder.
     *
     * @param builder the cookie settings
     */
    private HttpSessionStore(final Builder builder) {
        super();
        builder.validate();
        this.cookieName = builder.cookieName;
        this.timeout = builder.timeout;
        this.httpOnly = builder.httpOnly;
        this.secure = builder.secure;
        this.sameSite = builder.sameSite;
        this.path = builder.path;
        this.maxAge = builder.maxAge;
        this.localSessionStore = create(YojaApp.vertx());
        this.localMap = JavaReflectUtil.getFieldValue(this.localSessionStore, "localMap");
        this.localMap.clear();
    }

    /**
     * Returns the session cookie name.
     *
     * @return the session cookie name
     */
    public String cookieName() {
        return cookieName;
    }

    /**
     * Returns the idle timeout.
     *
     * @return the idle timeout
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Returns whether the session cookie is hidden from JavaScript.
     *
     * @return whether the session cookie carries {@code HttpOnly}
     */
    public boolean httpOnly() {
        return httpOnly;
    }

    /**
     * Returns whether the session cookie is sent over HTTPS only.
     *
     * @return whether the session cookie carries {@code Secure}
     */
    public boolean secure() {
        return secure;
    }

    /**
     * Returns the {@code SameSite} attribute of the session cookie.
     *
     * @return the {@code SameSite} attribute of the session cookie
     */
    public CookieSameSite sameSite() {
        return sameSite;
    }

    /**
     * Returns the {@code Path} attribute of the session cookie.
     *
     * @return the {@code Path} attribute of the session cookie
     */
    public String path() {
        return path;
    }

    /**
     * Returns the {@code Max-Age} of the session cookie.
     *
     * @return the {@code Max-Age} of the session cookie, or {@code null} for a
     *         cookie dropped when the browser closes
     */
    public Duration maxAge() {
        return maxAge;
    }

    /**
     * Returns a future resolving to the current session count.
     *
     * @return a future resolving to the current session count
     */
    public Future<Integer> size() {
        return localSessionStore.size();
    }

    /**
     * Snapshots every active session into {@link HttpSession} wrappers.
     *
     * @return a future resolving to the set of live sessions
     */
    public Future<Set<HttpSession>> sessions() {
        final ContextInternal ctx = ((VertxInternal) YojaApp.vertx()).getOrCreateContext();
        return ctx.succeededFuture(Sets.newHashSet(localMap.values()
                                                           .stream()
                                                           .filter(v -> v != null)
                                                           .map(HttpSession::new)
                                                           .toList()));
    }

    /**
     * Looks up a session by id.
     *
     * @param sessionId the session id to look up
     * @return a future resolving to the matching wrapper, or {@code null} when
     *         the session has expired or never existed
     */
    public Future<HttpSession> get(final String sessionId) {
        return localSessionStore.get(sessionId)
                                .map(v -> {
                                    final HttpSession result;
                                    if (v != null) {
                                        result = new HttpSession(v);
                                    }
                                    else {
                                        result = null;
                                    }
                                    return result;
                                });
    }

    /**
     * Deletes the session with the given id.
     *
     * @param sessionId the session id to delete
     * @return a future completing when the deletion is done
     */
    public Future<Void> delete(final String sessionId) {
        return localSessionStore.delete(sessionId);
    }

    /**
     * Drops every session from the store.
     *
     * @return a future completing when the store is empty
     */
    public Future<Void> clear() {
        return localSessionStore.clear();
    }

    /**
     * Registers a handler invoked every time the store creates a new session.
     *
     * @param action handler to register
     */
    public void onSession(final Handler<HttpSession> action) {
        onSessionActions.add(action);
    }

    /**
     * Invokes every registered {@link #onSession} handler with the freshly
     * created session. Called by the custom {@link LocalSessionStoreImpl}
     * created in {@link #create(Vertx)}.
     *
     * @param session the freshly created Vert.x session
     */
    protected void applyOnSession(final Session session) {
        for (final Handler<HttpSession> action : onSessionActions) {
            action.handle(new HttpSession(session));
        }
    }

    /**
     * Returns the wrapped Vert.x session store (router-internal access).
     *
     * @return the wrapped Vert.x session store (router-internal access)
     */
    protected LocalSessionStore localSessionStore() {
        return localSessionStore;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpSessionStore.class.getSimpleName());
        result.append(" [cookieName=");
        result.append(cookieName);
        result.append(", timeout=");
        result.append(timeout);
        result.append(", httpOnly=");
        result.append(httpOnly);
        result.append(", secure=");
        result.append(secure);
        result.append(", sameSite=");
        result.append(sameSite);
        result.append(", path=");
        result.append(path);
        result.append(", maxAge=");
        result.append(maxAge);
        result.append(", sessions=");
        result.append(localMap.size());
        result.append("]");
        return result.toString();
    }

    /**
     * Builds a Vert.x {@link LocalSessionStoreImpl} whose
     * {@code createSession} hooks call {@link #applyOnSession(Session)} so the
     * registered {@code onSession} handlers fire for every brand-new session.
     */
    private LocalSessionStore create(final Vertx vertx) {
        final LocalSessionStoreImpl store = new LocalSessionStoreImpl() {

            @Override
            public Session createSession(final long timeout) {
                final Session session = super.createSession(timeout);
                applyOnSession(session);
                return session;
            }

            @Override
            public Session createSession(final long timeout,
                                         final int length) {
                final Session session = super.createSession(timeout, length);
                applyOnSession(session);
                return session;
            }

        };
        store.init(vertx,
                   new JsonObject().put("reaperInterval", LocalSessionStore.DEFAULT_REAPER_INTERVAL)
                                   .put("mapName", LocalSessionStore.DEFAULT_SESSION_MAP_NAME));
        return store;
    }
    
    /*
     * 
     * BUILDER
     * 
     */
    /**
     * Creates a new {@link Builder} of a session store, with secure defaults:
     * {@code HttpOnly}, {@code Secure}, {@code SameSite=Strict}, {@code Path=/},
     * no {@code Max-Age} (the cookie is dropped when the browser closes) and an
     * idle timeout of {@link #DEFAULT_TIMEOUT}.
     * <p>
     * The cookie has no {@code Domain} attribute: it is sent back to the exact
     * host that set it only, never to its sub-domains.
     *
     * @param cookieName cookie name used to carry the session id; a name
     *                   prefixed {@code __Host-} (the most robust) requires
     *                   {@code Secure} and {@code Path=/}, a name prefixed
     *                   {@code __Secure-} requires {@code Secure}
     * @return a new builder
     */
    public static Builder builder(final String cookieName) {
        return new Builder(cookieName);
    }

    /** Builder of a {@link HttpSessionStore}: its session cookie settings. */
    public static class Builder {

        private final String cookieName;
        private Duration timeout = DEFAULT_TIMEOUT;
        private boolean httpOnly = true;
        private boolean secure = true;
        private CookieSameSite sameSite = CookieSameSite.STRICT;
        private String path = DEFAULT_PATH;
        private Duration maxAge;

        /**
         * Creates a builder with secure defaults; use {@link HttpSessionStore#builder(String)}.
         *
         * @param cookieName cookie name used to carry the session id
         */
        private Builder(final String cookieName) {
            this.cookieName = cookieName;
        }

        /**
         * Sets the idle timeout after which a session is reaped (default
         * {@link #DEFAULT_TIMEOUT}).
         *
         * @param timeout the idle timeout
         * @return this builder
         */
        public Builder timeout(final Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets whether the cookie is hidden from JavaScript (default {@code true}):
         * a script injected in a page (XSS) can't read the session id.
         *
         * @param httpOnly whether the cookie carries {@code HttpOnly}
         * @return this builder
         */
        public Builder httpOnly(final boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        /**
         * Sets whether the cookie is sent over HTTPS only (default {@code true}).
         * Browsers treat {@code http://localhost} as secure, so a {@code Secure}
         * cookie works there too; turn it off only for a plain HTTP server
         * reached by another name.
         *
         * @param secure whether the cookie carries {@code Secure}
         * @return this builder
         */
        public Builder secure(final boolean secure) {
            this.secure = secure;
            return this;
        }

        /**
         * Sets the {@code SameSite} attribute of the cookie (default
         * {@link CookieSameSite#STRICT}): it guards against cross-site request
         * forgery (CSRF). {@link CookieSameSite#LAX} keeps the user signed in
         * when coming from a link of another site (an email...);
         * {@link CookieSameSite#NONE} requires {@code Secure}.
         *
         * @param sameSite the {@code SameSite} attribute
         * @return this builder
         */
        public Builder sameSite(final CookieSameSite sameSite) {
            this.sameSite = sameSite;
            return this;
        }

        /**
         * Sets the {@code Path} attribute of the cookie (default {@link #DEFAULT_PATH}).
         *
         * @param path the path, starting with {@code /}
         * @return this builder
         */
        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the {@code Max-Age} of the cookie (default {@code null}: the
         * cookie is dropped when the browser closes). Set it only to keep the
         * user signed in across browser restarts ("remember me"); the session
         * itself still expires on the server after its {@link #timeout(Duration)}.
         *
         * @param maxAge the {@code Max-Age}, or {@code null}
         * @return this builder
         */
        public Builder maxAge(final Duration maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        /**
         * Builds the session store.
         *
         * @return the session store
         * @throws IllegalArgumentException when the settings are inconsistent
         *                                  (see {@link HttpSessionStore#builder(String)})
         */
        public HttpSessionStore build() {
            return new HttpSessionStore(this);
        }

        /**
         * Checks the settings are consistent: the browser would otherwise drop
         * the cookie silently.
         *
         * @throws IllegalArgumentException when the settings are inconsistent
         */
        private void validate() {
            if (cookieName == null
                    || cookieName.isBlank()) {
                throw new IllegalArgumentException("cookieName is required");
            }
            if (timeout == null
                    || timeout.isNegative()
                    || timeout.isZero()) {
                throw new IllegalArgumentException("timeout must be positive: " + timeout);
            }
            Objects.requireNonNull(sameSite, "sameSite");
            if (path == null
                    || !path.startsWith("/")) {
                throw new IllegalArgumentException("path must start with '/': " + path);
            }
            if (maxAge != null
                    && maxAge.isNegative()) {
                throw new IllegalArgumentException("maxAge must not be negative: " + maxAge);
            }
            if (sameSite == CookieSameSite.NONE
                    && !secure) {
                throw new IllegalArgumentException("SameSite=None requires Secure");
            }
            if (cookieName.startsWith("__Secure-")
                    && !secure) {
                throw new IllegalArgumentException("a cookie prefixed __Secure- requires Secure");
            }
            if (cookieName.startsWith("__Host-")
                    && (!secure || !DEFAULT_PATH.equals(path))) {
                throw new IllegalArgumentException("a cookie prefixed __Host- requires Secure and Path=/");
            }
        }

    }

}
