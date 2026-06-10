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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date-formatting helpers used to render browser-log timestamps consistently
 * in the format {@code yyyy-MM-dd HH:mm:ss}, in the JVM's default time zone.
 */
public class DateUtil {

    /** Not instantiable; only static helpers are exposed. */
    private DateUtil() {
        super();
    }

    /** Pre-configured formatter pinned to the JVM's system zone. */
    private static DateTimeFormatter logDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                                             .withZone(ZoneId.systemDefault());

    /**
     * Formats a millisecond Unix timestamp as {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @param timestamp epoch-milliseconds value
     * @return the formatted timestamp
     */
    public static String formatTimestamp(final Long timestamp) {
        return logDateTimeFormatter.format(Instant.ofEpochMilli(timestamp));
    }

    /**
     * Formats an {@link Instant} as {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @param instant point in time
     * @return the formatted timestamp
     */
    public static String formatTimestamp(final Instant instant) {
        return logDateTimeFormatter.format(instant);
    }

}
