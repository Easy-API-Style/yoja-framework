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
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.worker.Worker;
import com.easygoingapi.yoja.core.worker.Worker.Type;
import com.easygoingapi.yoja.core.worker.Worker.WorkerEvent;

import io.vertx.core.Future;

public class TestParallelThread {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(TestParallelThread.class);
	
	@Test
	public void test_01() {
		final Set<String> set = new HashSet<>();
		Worker.parallelThread.create("TEST_1", 8).execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("A");
				LOGGER.debug("parallel test_01 A");
			}
		});
		Worker.parallelThread.get("TEST_1").execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("parallel test_01 B");
			}
		});
		sleep(2000);
		final Worker worker_1 = Worker.parallelThread.get("TEST_1");
		assertEquals("TEST_1", worker_1.id());
		assertEquals(8, worker_1.poolSize());
		assertTrue(worker_1.isActive());
		YojaApp.stop();
		YojaApp.awaitStop();
		assertFalse(worker_1.isActive());
		assertEquals(2, set.size());
	}
	
	@Test
	public void test_02() {
		final Set<String> set = new HashSet<>();
		final AtomicReference<WorkerEvent> eventRemove = new AtomicReference<>();
		final Worker worker = Worker.parallelThread.create("TEST_1", 8);
		worker.onRemove(e -> {
			eventRemove.set(e);
		});
		final Future<Void> future_1 = worker.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("A");
				LOGGER.debug("parallel test_02 A");
			}
		});
		final Future<Void> future_2 = Worker.parallelThread.get("TEST_1").execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("parallel test_02 B");
			}
		});
		final Future<Void> future_3 = Worker.parallelThread.create("TEST_1", 10).execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("C");
				LOGGER.debug("parallel test_02 C");
			}
		});
		sleep(2000);
		final Worker worker_1 = Worker.parallelThread.get("TEST_1");
		assertEquals("TEST_1", worker_1.id());
		assertEquals(8, worker_1.poolSize());
		assertTrue(worker_1.isActive());
		Worker.remove(worker_1);
		await(future_1, future_2, future_3);
		assertFalse(worker_1.isActive());
		assertEquals(3, set.size());
		assertEquals(Type.parallelThread, eventRemove.get().type());
		assertEquals(8, eventRemove.get().poolSize());
		assertNull(Worker.parallelThread.get("TEST_1"));
		YojaApp.stop();
		YojaApp.awaitStop();
	}

	@Test
	public void test_03() {
		YojaApp.start();
		final Worker worker_1 = Worker.parallelThread.get("TEST_1");
		assertNull(worker_1);
		YojaApp.stop();
		YojaApp.awaitStop();
	}

	@Test
	public void test_04() {
		final Set<String> set = new HashSet<>();
		
		final Worker worker_1 = Worker.parallelThread.create("TEST_1", 2);
		final Worker worker_2 = Worker.parallelThread.create("TEST_2", 3);
		final Worker worker_3 = Worker.parallelThread.create("TEST_3", 4);
		
		final Set<String> workerIds = new TreeSet<>();
		workerIds.add(worker_1.id());
		workerIds.add(worker_2.id());
		workerIds.add(worker_3.id());
		
		final AtomicReference<WorkerEvent> eventRemove = new AtomicReference<>();
		worker_1.onRemove(e -> {
			eventRemove.set(e);
		});
		final Future<Void> future_1 = worker_1.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("A");
				LOGGER.debug("parallel test_04 A");
			}
		});
		final Future<Void> future_2 = worker_2.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("B");
				LOGGER.debug("parallel test_04 B");
			}
		});
		final Future<Void> future_3 = worker_3.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("C");
				LOGGER.debug("parallel test_04 C");
			}
		});
		sleep(2000);
		assertEquals(workerIds, Worker.parallelThread.ids());
		
		final Worker worker_11 = Worker.parallelThread.get("TEST_1");
		assertEquals(2, worker_11.poolSize());
		assertTrue(worker_11.isActive());
		final Worker worker_22 = Worker.parallelThread.get("TEST_2");
		assertEquals(3, worker_22.poolSize());
		assertTrue(worker_22.isActive());
		final Worker worker_33 = Worker.parallelThread.get("TEST_3");
		assertEquals(4, worker_33.poolSize());
		assertTrue(worker_33.isActive());
		
		Worker.remove(worker_1, worker_22, worker_33);
		await(future_1, future_2, future_3);
		
		assertEquals(3, set.size());
		
		assertEquals(Type.parallelThread, eventRemove.get().type());
		assertEquals(2, eventRemove.get().poolSize());
		assertNull(Worker.parallelThread.get("TEST_1"));
		assertNull(Worker.parallelThread.get("TEST_2"));
		assertNull(Worker.parallelThread.get("TEST_3"));
		YojaApp.stop();
		YojaApp.awaitStop();
	}

	@Test
	public void test_05() {
		final Set<String> set = new HashSet<>();
		
		final Worker worker_1 = Worker.parallelThread.create("TEST_1", 2);
		final Worker worker_2 = Worker.parallelThread.create("TEST_2", 3);
		final Worker worker_3 = Worker.parallelThread.create("TEST_3", 4);
		
		final Set<String> workerIds = new TreeSet<>();
		workerIds.add(worker_1.id());
		workerIds.add(worker_2.id());
		workerIds.add(worker_3.id());
		
		final AtomicBoolean stop = new AtomicBoolean();
		
		final AtomicReference<WorkerEvent> eventRemove = new AtomicReference<>();
		worker_1.onRemove(e -> {
			eventRemove.set(e);
		});
		final Future<Void> future_1 = worker_1.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					stop.set(true);
					break;
				}
				set.add("A");
				LOGGER.debug("parallel test_05 A");
			}
		});
		final Future<Void> future_2 = worker_2.execute(() -> {
			while (true) {
				if (stop.get()) {
					break;
				}
				set.add("B");
				LOGGER.debug("parallel test_05 B");
			}
		});
		final Future<Void> future_3 = worker_3.execute(() -> {
			while (true) {
				if (Worker.isClosed()) {
					break;
				}
				set.add("C");
				LOGGER.debug("parallel test_05 C");
			}
		});
		sleep(2000);
		assertEquals(workerIds, Worker.parallelThread.ids());
		
		final Worker worker_11 = Worker.parallelThread.get("TEST_1");
		assertEquals(2, worker_11.poolSize());
		assertTrue(worker_11.isActive());
		final Worker worker_22 = Worker.parallelThread.get("TEST_2");
		assertEquals(3, worker_22.poolSize());
		assertTrue(worker_22.isActive());
		final Worker worker_33 = Worker.parallelThread.get("TEST_3");
		assertEquals(4, worker_33.poolSize());
		assertTrue(worker_33.isActive());
		
		Worker.remove(worker_11, worker_33);
		await(future_1, future_2, future_3);
		
		assertEquals(3, set.size());
		
		assertEquals(Type.parallelThread, eventRemove.get().type());
		assertEquals(2, eventRemove.get().poolSize());
		assertNull(Worker.parallelThread.get("TEST_1"));
		final Worker worker_222 = Worker.parallelThread.get("TEST_2");
		assertEquals(3, worker_222.poolSize());
		assertTrue(worker_222.isActive());
		assertNull(Worker.parallelThread.get("TEST_3"));
		YojaApp.stop();
		YojaApp.awaitStop();
	}

	@Test
	public void test_06() {
		final Worker worker = Worker.parallelThread.create("TEST_1", 8);
		Worker.remove(worker);		
		final Future<Void> future = worker.execute(() -> {
			LOGGER.debug("parallel test_06");
		});
		await(future);
		assertNull(Worker.parallelThread.get("TEST_1"));
		assertFalse(worker.isActive());
		assertTrue(future.failed());
		YojaApp.stop();
		YojaApp.awaitStop();
	}
	
}
