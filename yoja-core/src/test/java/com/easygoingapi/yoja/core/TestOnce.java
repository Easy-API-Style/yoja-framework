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

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.YojaAppException;
import com.easygoingapi.yoja.core.worker.Worker;

import io.vertx.core.Future;

public class TestOnce {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(TestOnce.class);
	
	@Test
	public void test_01() {
		final Future<String> value = Worker.singleThread.once("TEST_1", handler -> {
			handler.complete("test_01");
		});
		await(value);
		assertEquals("test_01", value.result());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
	@Test
	public void test_02() {
		final Future<String> value = Worker.singleThread.once("TEST_1", handler -> {
			handler.fail("test_02 error");
		});
		await(value);
		assertTrue(value.failed());
		assertEquals("test_02 error", value.cause().getMessage());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
	@Test
	public void test_03() {
		final Future<String> value = Worker.singleThread.once("TEST_1", handler -> {
			throw new YojaAppException("test_03 error");
		});
		await(value);
		assertTrue(value.failed());
		assertEquals("test_03 error", value.cause().getMessage());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
	@Test
	public void test_04() {
		final Future<String> value = Worker.singleThread.once("TEST_1", () -> {
			throw new YojaAppException("test_04 error");
		});
		await(value);
		assertTrue(value.failed());
		assertEquals("test_04 error", value.cause().getMessage());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
	@Test
	public void test_05() {
		final Future<String> value = Worker.singleThread.once("TEST_1", () -> {
			return "test_05";
		});
		await(value);
		assertFalse(value.failed());
		assertEquals("test_05", value.result());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
	@Test
	public void test_06() {
		final AtomicReference<String> value = new AtomicReference<>();
		final Future<Void> future = Worker.singleThread.once("TEST_1", () -> {
			LOGGER.debug("once test_06 A");
			LOGGER.debug("once test_06 B");
			value.set("test_06");
		});
		await(future);
		assertFalse(future.failed());
		assertNull(future.result());
		assertEquals("test_06", value.get());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
	@Test
	public void test_07() {
		final Set<String> set = new HashSet<>();
		Worker.singleThread.once("TEST_1", () -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("A");
				LOGGER.debug("once test_07 A");
			}
		});
		Worker.singleThread.once("TEST_1", () -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("once test_07 B");
			}
		});
		sleep(2000);
		YojaApp.stop();
		YojaApp.awaitStop();
		assertEquals(1, set.size());
	}

	@Test
	public void test_08() {
		final Set<String> set = new HashSet<>();
		Worker.singleThread.once("TEST_1", () -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("A");
				LOGGER.debug("once test_08 A");
			}
		});
		Worker.singleThread.once("TEST_2", () -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("once test_08 B");
			}
		});
		sleep(2000);
		YojaApp.stop();
		YojaApp.awaitStop();
		assertEquals(2, set.size());
	}
	
	@Test
	public void test_09() {
		final Future<String> future_1 = Worker.singleThread.once("TEST_1", () -> {
			return "test_09 A";
		});
		final Future<String> future_2 = Worker.singleThread.once("TEST_1", () -> {
			return "test_09 B";
		});
		await(future_1, future_2);
		assertEquals("test_09 A", future_1.result());
		assertEquals("test_09 B", future_2.result());
		YojaApp.stop();
		YojaApp.awaitStop();
	}

    @Test
    public void test_10() {
        final AtomicBoolean value_1 = new AtomicBoolean();
        final AtomicBoolean value_2 = new AtomicBoolean();
        final AtomicBoolean value_3 = new AtomicBoolean();
        final Future<Void> future = Worker.parallelThread.execute(() -> {
            value_1.set(YojaApp.isWorkerThread());
        })
        .andThen(h -> {
            value_2.set(YojaApp.isWorkerThread());
        })
        .onComplete(h -> {
            value_3.set(YojaApp.isWorkerThread());
        });
        await(future);
        assertTrue(value_1.get());
        assertFalse(value_2.get());
        assertFalse(value_3.get());
        YojaApp.stop();
        YojaApp.awaitStop();
    }
	
}
