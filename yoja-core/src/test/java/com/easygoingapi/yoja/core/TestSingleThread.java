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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.YojaAppException;
import com.easygoingapi.yoja.core.worker.Worker;

import io.vertx.core.Future;

public class TestSingleThread {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(TestSingleThread.class);
	
	@Test
	public void test_01() {
		final Future<String> value = Worker.singleThread.get("TEST_1").execute(handler -> {
			handler.complete("test_01");
		});
		await(value);
		assertEquals("test_01", value.result());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
	@Test
	public void test_02() {
		final Future<String> value = Worker.singleThread.get("TEST_1").execute(handler -> {
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
		final Future<String> value = Worker.singleThread.get("TEST_1").execute(handler -> {
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
		final Future<String> value = Worker.singleThread.get("TEST_1").execute(() -> {
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
		final Future<String> value = Worker.singleThread.get("TEST_1").execute(() -> {
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
		final Future<Void> future = Worker.singleThread.get("TEST_1").execute(() -> {
			LOGGER.debug("single test_06 A");
			LOGGER.debug("single test_06 B");
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
		Worker.singleThread.get("TEST_1").execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("A");
				LOGGER.debug("single test_07 A");
			}
		});
		Worker.singleThread.get("TEST_1").execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("single test_07 B");
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
		Worker.singleThread.get("TEST_1").execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("A");
				LOGGER.debug("single test_08 A");
			}
		});
		Worker.singleThread.get("TEST_2").execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("single test_08 B");
			}
		});
		sleep(2000);
		YojaApp.stop();
		YojaApp.awaitStop();
		assertEquals(2, set.size());
	}
	
	@Test
	public void test_09() {
		final Future<String> future_1 = Worker.singleThread.get("TEST_1").execute(() -> {
			return "test_09 A";
		});
		final Future<String> future_2 = Worker.singleThread.get("TEST_1").execute(() -> {
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
		final Set<String> set = new HashSet<>();
		final Worker worker = Worker.singleThread.get("TEST_1");
		final Future<String> future_1 = worker.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					return "test_10 done";
				}
				set.add("A");
				LOGGER.debug("single test_10 A");
			}
		});
		sleep(2000);
		worker.remove();
		await(future_1);
		assertEquals("test_10 done", future_1.result());
		assertFalse(worker.isActive());
		assertEquals(1, set.size());
		
		Worker.singleThread.get("TEST_1").execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("single test_10 B");
			}
		});
		sleep(2000);
		YojaApp.stop();
		YojaApp.awaitStop();
		assertEquals(2, set.size());
	}

	@Test
	public void test_11() {
		final Set<String> set = new HashSet<>();
		final Worker worker = Worker.singleThread.get("TEST_1");
		final Future<String> future_1 = worker.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					return "test_11 A done";
				}
				set.add("A");
				LOGGER.debug("single test_11 A");
			}
		});
		sleep(2000);
		worker.remove();
		await(future_1);
		assertEquals("test_11 A done", future_1.result());
		assertFalse(worker.isActive());
		assertEquals(1, set.size());
		
		final Future<String> future_2 = worker.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					return "test_11 B done";
				}
				set.add("B");
				LOGGER.debug("single test_11 B");
			}
		});
		sleep(2000);
		await(future_2);
		assertTrue(future_2.failed());
		YojaApp.stop();
		YojaApp.awaitStop();
		assertEquals(1, set.size());
	}
	
}
