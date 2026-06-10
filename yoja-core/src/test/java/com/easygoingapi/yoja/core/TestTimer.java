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

import static com.easygoingapi.yoja.core.util.FutureUtil.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.worker.Timer;
import com.easygoingapi.yoja.core.worker.Timer.Task;
import com.easygoingapi.yoja.core.worker.Worker;

import io.vertx.core.Handler;

public class TestTimer {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestTimer.class);
    
    @Test
    public void test_01() {
        final AtomicReference<String> value = new AtomicReference<>();
        final Handler<Task> action = v -> {
            value.set(v.id());
        };
        
        ;
        final Task task = Timer.schedule("timerId", action)
                               .delay(Duration.ofMillis(500))
                               .build();
        sleep(1000);
        assertEquals(task.id(), value.get());
        assertFalse(Timer.cancel(task.id()));
        YojaApp.stop();
        YojaApp.awaitStop();
    }
    
    @Test
    public void test_02() {
        final AtomicReference<Object> value = new AtomicReference<>();
        final Handler<Task> action = v -> {
            value.set(v.id());
        };
        Timer.schedule("timerId", action)
             .delay(Duration.ofMillis(1000))
             .build();
        sleep(500);
        assertNull(value.get());
        assertTrue(Timer.cancel("timerId"));
        YojaApp.stop();
        YojaApp.awaitStop();
    }
    
    @Test
    public void test_03() {
        final AtomicReference<Object> value = new AtomicReference<>();
        final Handler<Task> action = v -> {
            value.set(v.id());
        };
        final Task task = Timer.schedule("timerId", action)
                               .delay(Duration.ofMillis(1000))
                               .build();
        sleep(500);
        assertNull(value.get());
        assertTrue(Timer.cancel(task.id()));
        sleep(3000);
        assertNull(value.get());
        assertFalse(Timer.cancel(task.id()));
        YojaApp.stop();
        YojaApp.awaitStop();
    }

    @Test
    public void test_04() {
        final String name = "time_1";
        final AtomicReference<Object> value = new AtomicReference<>();
        final Handler<Task> action = v -> {
            value.set(v.id());
        };
        Timer.schedule(name, action)
             .delay(Duration.ofMillis(1000))
             .build();
        sleep(500);
        assertNull(value.get());
        assertTrue(Timer.has(name));
        assertTrue(Timer.cancel(name));
        sleep(3000);
        assertNull(value.get());
        assertFalse(Timer.has(name));
        assertFalse(Timer.cancel(name));
        YojaApp.stop();
        YojaApp.awaitStop();
    }

    @Test
    public void test_05() {
        final String name_1 = "time_1";
        final String name_2 = "time_2";
        
        final AtomicReference<Object> value_1 = new AtomicReference<>();
        final Handler<Task> action_1 = v -> {
            value_1.set(v.id());
        };
        Timer.schedule(name_1, action_1)
             .delay(Duration.ofMillis(1000))
             .build();
        final AtomicReference<Object> value_2 = new AtomicReference<>();
        final Handler<Task> action_2 = v -> {
            value_2.set(v.id());
        };
        Timer.schedule("time_2", action_2)
             .delay(Duration.ofMillis(250))
             .build();
        sleep(500);
        assertNull(value_1.get());
        assertEquals(name_2, value_2.get());
        assertTrue(Timer.has(name_1));
        assertTrue(Timer.has(name_2));
        
        sleep(2000);
        assertEquals("time_1", value_1.get());
        Timer.cancel(name_1);
        Timer.cancel(name_2);
        YojaApp.stop();
        YojaApp.awaitStop();
    }
    
    @Test
    public void test_06() {
        final String name = "time_1";
        final AtomicInteger i = new AtomicInteger();
        final Handler<Task> action = v -> {
            if (i.incrementAndGet() == 3) {
                Timer.cancel(name);
            }
        };
        Timer.schedule(name, action)
             .delay(Duration.ofMillis(1000))
             .period(Duration.ofMillis(500))
             .build();
        sleep(500);
        assertEquals(0, i.get());
        sleep(3000);
        assertEquals(3, i.get());
        Timer.cancel(name);
        YojaApp.stop();
        YojaApp.awaitStop();
    }
    
    @Test
    public void test_07() {
        final String name = "time_1";
        final AtomicInteger i = new AtomicInteger();
        final Handler<Task> action = v -> {
            if (i.incrementAndGet() == 3) {
                Timer.cancel(name);
            }
        };
        Timer.schedule(name, action)
             .period(Duration.ofMillis(500))
             .build();
        sleep(250);
        assertEquals(1, i.get());
        sleep(3000);
        assertEquals(3, i.get());
        Timer.cancel();
        YojaApp.stop();
        YojaApp.awaitStop();
    }

    @Test
    public void test_08() {
        final Worker worker = Worker.singleThread.get("worker_WORKER");
        final Set<String> threadNames = new HashSet<>();
        final Handler<Task> action = v -> {
            final String threadName = Thread.currentThread().getName();
            threadNames.add(threadName);
            LOGGER.debug(threadName);
        };
        Timer.schedule("time_1", action)
             .worker(worker)
             .period(Duration.ofMillis(500))
             .build();
        sleep(3000);
        assertEquals(1, threadNames.size());
        assertTrue(threadNames.iterator().next().contains("worker_WORKER"));
        Timer.cancel();
        YojaApp.stop();
        YojaApp.awaitStop();
    }
    
    @Test
    public void test_09() {
        final Worker worker = Worker.parallelThread.create("worker_WORKER", 4);
        final Set<String> threadNames = new HashSet<>();
        final Handler<Task> action_1 = v -> {
            final String threadName = Thread.currentThread().getName();
            threadNames.add(threadName);
            LOGGER.info("a {}", threadName);
        };
        Timer.schedule("time_1", action_1)
                    .worker(worker)
                    .period(Duration.ofMillis(500))
                    .build();
        
        final Handler<Task> action_2 = v -> {
            final String threadName = Thread.currentThread().getName();
            threadNames.add(threadName);
            LOGGER.info("b {}", threadName);
        };
        Timer.schedule("time_2", action_2)
             .worker(worker)
             .period(Duration.ofMillis(500))
             .build();
        
        sleep(5000);
        assertTrue(threadNames.iterator().next().contains("worker_WORKER"));
        assertTrue(threadNames.size() > 1);
        Timer.cancel();
        YojaApp.stop();
        YojaApp.awaitStop();
        YojaApp.stop();
        YojaApp.awaitStop();
    }

    @Test
    public void test_10() {
        final Worker worker = Worker.singleThread.get("worker_WORKER");
        final Set<String> threadNames = new HashSet<>();
        final Handler<Task> action = v -> {
            final String threadName = Thread.currentThread().getName();
            threadNames.add(threadName);
            LOGGER.debug(threadName);
        };
        Timer.schedule("time_1", action)
             .worker(worker)
             .period(Duration.ofMillis(500))
             .build();
        sleep(3000);
        assertEquals(1, threadNames.size());
        assertTrue(threadNames.iterator().next().contains("worker_WORKER"));
        Timer.cancel();
        YojaApp.stop();
        YojaApp.awaitStop();
    }
    
}
