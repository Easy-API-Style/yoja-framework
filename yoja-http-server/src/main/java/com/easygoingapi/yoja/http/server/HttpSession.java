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
import java.util.Objects;

import io.vertx.ext.web.Session;

/**
 * Thin façade around a Vert.x Web {@link Session}.
 * <p>
 * Mirrors the underlying API (put/get/remove, regenerate, destroy, ...) but
 * keys stored values by their class name and returns Yoja-typed values where
 * it matters (e.g. a {@link Duration} timeout rather than a raw millisecond
 * count). Identity is defined by the underlying session id, so
 * {@link #equals(Object)} and {@link #hashCode()} are stable across
 * re-wrappings.
 */
public final class HttpSession {

    /** The wrapped Vert.x session. */
    private final Session session;

    /**
     * Wraps the given Vert.x Web session.
     *
     * @param session the underlying Vert.x Web session
     */
    public HttpSession(final Session session) {
        super();
        this.session = session;
    }

    /**
     * Returns the underlying session id.
     *
     * @return the underlying session id
     */
    public String id() {
        return session.id();
    }

    /**
     * Stores {@code obj} in the session, keyed by its fully qualified class name.
     *
     * @param obj value to store
     * @return the underlying Vert.x session (for chaining with the native API)
     */
    public Session put(final Object obj) {
        return session.put(obj.getClass().getName(), obj);
    }

    /**
     * Stores {@code obj} keyed by its fully qualified class name, only if no
     * value is currently associated with that class name.
     *
     * @param obj value to store
     * @return the underlying Vert.x session
     */
    public Session putIfAbsent(final Object obj) {
        return session.putIfAbsent(obj.getClass().getName(), obj);
    }
    
    /**
     * Returns the value stored under the given class name (unchecked cast), or
     * {@code null} when missing.
     *
     * @param key class whose name identifies the stored value
     * @param <T> expected value type
     * @return the stored value (unchecked cast), or {@code null} when missing
     */
    public <T> T get(final Class<?> key) {
        return session.get(key.getName());
    }

    /**
     * Removes and returns the value stored under the given class name.
     *
     * @param key class whose name identifies the stored value
     * @param <T> expected value type
     * @return the removed value (unchecked cast), or {@code null} when absent
     */
    public <T> T remove(final Class<?> key) {
        return session.remove(key.getName());
    }

    /**
     * Returns {@code true} when the session holds no data.
     *
     * @return {@code true} when the session holds no data
     */
    public boolean isEmpty() {
        return session.isEmpty();
    }

    /**
     * Returns the session timeout as a {@link Duration}.
     *
     * @return the session timeout as a {@link Duration}
     */
    public Duration timeout() {
        return Duration.ofMillis(session.timeout());
    }

    /**
     * Rotates the session id (typically after privilege escalation, e.g. on
     * login) without losing the stored data.
     */
    public void regenerateId() {
        session.regenerateId();
    }

    /**
     * Returns {@code true} when {@link #regenerateId()} has been called.
     *
     * @return {@code true} when {@link #regenerateId()} has been called
     */
    public boolean isRegenerated() {
        return session.isRegenerated();
    }

    /**
     * Returns the previous id once {@link #regenerateId()} has been invoked.
     *
     * @return the previous id once {@link #regenerateId()} has been invoked
     */
    public String oldId() {
        return session.oldId();
    }

    /** Destroys the session; subsequent requests will allocate a fresh one. */
    public void destroy() {
        session.destroy();
    }

    /**
     * Returns {@code true} when the session has been destroyed.
     *
     * @return {@code true} when the session has been destroyed
     */
    public boolean isDestroyed() {
        return session.isDestroyed();
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }

    /**
     * Equality is based purely on the session id, so two wrappers built around
     * the same underlying session are equal even if they are distinct Java
     * objects.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HttpSession other = (HttpSession) obj;
        return Objects.equals(session.id(), other.session.id());
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(HttpSession.class.getSimpleName());
        result.append(" [id=");
        result.append(id());
        result.append("]");
        return result.toString();
    }

}
