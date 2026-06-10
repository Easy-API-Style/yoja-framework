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
import java.util.Set;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.util.JavaReflectUtil;
import com.google.common.collect.Sets;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
 *   <li>cookie name and timeout are pinned at construction time and exposed
 *       on the API;</li>
 *   <li>a list of {@code onSession} handlers is invoked every time the store
 *       allocates a fresh session, allowing the application to seed default
 *       attributes or instrument session creation.</li>
 * </ul>
 * The underlying Vert.x {@link LocalMap} is also exposed by reflection so
 * {@link #sessions()} can iterate the live set of sessions (an operation the
 * public Vert.x API does not expose).
 */
public class HttpSessionStore {

    /** Cookie name used to carry the session id. */
    private final String cookieName;
    /** Session timeout (idle deadline). */
    private final Duration timeout;

    /** Handlers invoked whenever the store creates a fresh session. */
    private final List<Handler<HttpSession>> onSessionActions = new ArrayList<>();

    /** Vert.x' shared-data map backing the session store, accessed by reflection. */
    private final LocalMap<String, Session> localMap;
    /** The wrapped Vert.x session store. */
    private final LocalSessionStore localSessionStore;

    /**
     * Creates a new session store with the given cookie name and idle timeout.
     *
     * @param cookieName cookie name used to carry the session id
     * @param timeout    idle timeout after which a session is reaped
     */
    public HttpSessionStore(final String cookieName,
                            final Duration timeout) {
        super();
        this.cookieName = cookieName;
        this.timeout = timeout;
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

}
