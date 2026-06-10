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
package com.easygoingapi.yoja.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.util.TimeUtil;

public class TestTimeUtil {

    @Test
    public void test_01() {
        assertEquals("0 millisecond", TimeUtil.prettyPrint(Duration.ZERO));
    }

    @Test
    public void test_02() {
        assertEquals("1 millisecond", TimeUtil.prettyPrint(Duration.ofMillis(1)));
    }

    @Test
    public void test_03() {
        assertEquals("500 milliseconds", TimeUtil.prettyPrint(Duration.ofMillis(500)));
    }

    @Test
    public void test_04() {
        assertEquals("1 second ", TimeUtil.prettyPrint(Duration.ofSeconds(1)));
    }

    @Test
    public void test_05() {
        assertEquals("2 seconds ", TimeUtil.prettyPrint(Duration.ofSeconds(2)));
    }

    @Test
    public void test_06() {
        assertEquals("1 second 500 milliseconds", TimeUtil.prettyPrint(Duration.ofMillis(1500)));
    }

    @Test
    public void test_07() {
        assertEquals("1 minute ", TimeUtil.prettyPrint(Duration.ofMinutes(1)));
    }

    @Test
    public void test_08() {
        assertEquals("2 minutes ", TimeUtil.prettyPrint(Duration.ofMinutes(2)));
    }

    @Test
    public void test_09() {
        assertEquals("1 hour ", TimeUtil.prettyPrint(Duration.ofHours(1)));
    }

    @Test
    public void test_10() {
        assertEquals("1 day ", TimeUtil.prettyPrint(Duration.ofDays(1)));
    }

    @Test
    public void test_11() {
        assertEquals("2 days ", TimeUtil.prettyPrint(Duration.ofDays(2)));
    }

    @Test
    public void test_12() {
        final Duration d = Duration.ofDays(1)
                                   .plusHours(2)
                                   .plusMinutes(3)
                                   .plusSeconds(4)
                                   .plusMillis(5);
        assertEquals("1 day 2 hours 3 minutes 4 seconds 5 milliseconds",
                     TimeUtil.prettyPrint(d));
    }

    @Test
    public void test_13() {
        assertEquals("1 hour 30 minutes ", TimeUtil.prettyPrint(Duration.ofMinutes(90)));
    }

    @Test
    public void test_14() {
        assertEquals("- 1 day ", TimeUtil.prettyPrint(Duration.ofDays(-1)));
    }

    @Test
    public void test_15() {
        assertEquals("- 1 day 1 hour ", TimeUtil.prettyPrint(Duration.ofHours(-25)));
    }

}
