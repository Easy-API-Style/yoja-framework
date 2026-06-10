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
package com.easygoingapi.yoja.selenium;

import java.time.Instant;
import java.util.Objects;

/**
 * Single browser-side log entry collected by the {@code ywLogger.js} helper
 * and surfaced by {@link SeleniumService#logs()}.
 * <p>
 * Each entry carries a timestamp (browser-side {@code Date.now()} value
 * converted to an {@link Instant}), a {@link Level} matching the JavaScript
 * {@code console.*} call that produced it, and the textual message.
 */
public class Log {

    /**
     * Severity tag of a {@link Log}. {@link #LOG} corresponds to
     * {@code console.log}; the spelling {@link #WRAN} mirrors the
     * (intentional) typo used in the companion {@code ywLogger.js} so the
     * Java and JS sides stay in sync.
     */
    public static enum Level {
        /** Mirror of {@code console.log}. */
        LOG,
        /** Mirror of {@code console.trace}. */
        TRACE,
        /** Mirror of {@code console.debug}. */
        DEBUG,
        /** Mirror of {@code console.info}. */
        INFO,
        /** Mirror of {@code console.warn} (kept as {@code WRAN} to match the JS side). */
        WRAN,
        /** Mirror of {@code console.error}. */
        ERROR
    }

    /** Browser-side timestamp of the log entry. */
    private final Instant date;
    /** Severity tag of the entry. */
    private final Level level;
    /** Textual message of the entry (never {@code null}; empty when absent). */
    private final String message;

    /**
     * Constructs a log entry with the given timestamp, level, and message.
     *
     * @param date    browser-side timestamp
     * @param level   severity tag
     * @param message textual message
     */
    public Log(final Instant date,
               final Level level,
               final String message) {
        super();
        this.date = date;
        this.level = level;
        this.message = message;
    }

    /**
     * Returns the browser-side timestamp of the entry.
     *
     * @return browser-side timestamp of the entry
     */
    public Instant date() {
        return date;
    }

    /**
     * Returns the severity tag of the entry.
     *
     * @return severity tag of the entry
     */
    public Level level() {
        return level;
    }

    /**
     * Returns the textual message of the entry.
     *
     * @return textual message of the entry
     */
    public String message() {
        return message;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, level, message);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Log other = (Log) obj;
        return Objects.equals(date, other.date)
                 && level == other.level
                 && Objects.equals(message, other.message);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(Log.class.getSimpleName());
        result.append(" [");
        result.append("date=");
        result.append(date);
        result.append(", ");
        result.append("level=");
        result.append(level);
        if (message != null) {
            result.append(", ");
            result.append("message=");
            result.append(message);
        }
        result.append("]");
        return result.toString();
    }

    /*
     *
     * STATIC
     *
     */

}
