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
package com.easygoingapi.yoja.core.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Utility interface for time and duration helpers.
 */
public interface TimeUtil {

    /**
     * Returns the duration between now and the given future (or past) date.
     * A negative duration indicates the date is in the past.
     *
     * @param date the target date
     * @return the duration from the current instant to {@code date}
     */
    public static Duration delayFromNow(final Date date) {
        final long seconds = ChronoUnit.SECONDS.between(new Date().toInstant(), 
                                                        date.toInstant());
        return Duration.ofSeconds(seconds);
    }

    /**
     * Formats a {@link Duration} as a human-readable string (e.g. {@code "2 hours 30 minutes 5 seconds"}).
     * A leading {@code "- "} is prepended for negative durations.
     *
     * @param duration the duration to format
     * @return a non-blank human-readable representation of the duration
     */
    public static String prettyPrint(final Duration duration) {
        final StringBuilder result = new StringBuilder();
        
        final long days = Math.abs(duration.toDaysPart());
        final int hours = Math.abs(duration.toHoursPart());
        final int minutes = Math.abs(duration.toMinutesPart());
        final int seconds = Math.abs(duration.toSecondsPart());
        final int milliseconds = Math.abs(duration.toMillisPart());
        
        if (duration.toDaysPart() < 0) {
            result.append("- ");
        }
        if (days > 0) {
        	 result.append(plural(days, "day"));
             result.append(" ");
        }
        if (hours > 0) {
        	 result.append(plural(hours, "hour"));
             result.append(" ");
        }
        if (minutes > 0) {
        	 result.append(plural(minutes, "minute"));
             result.append(" ");
        }
        if (seconds > 0) {
        	result.append(plural(seconds, "second"));
        	result.append(" ");
        }
        if (milliseconds > 0) {
          result.append(plural(milliseconds, "millisecond"));
        }
        if (result.length() == 0) {
        	result.append("0 millisecond");
        }
        return result.toString();
    }

    private static String plural(final long num, 
    		                     final String unit) {
        return num + " " + unit + (num == 1 ? "" : "s");
    }
    
}
