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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;

import io.vertx.core.VertxOptions;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestYojaApp {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(TestYojaApp.class);
	
	@BeforeAll
    public static void stopYojaApp() {
         await(YojaApp.stop());
    }
	
	@Test
	public void test_01() {
		assertFalse(YojaApp.isRunning());
		YojaApp.start();
		assertTrue(YojaApp.isRunning());
		YojaApp.stop();
		YojaApp.awaitStop();
		assertFalse(YojaApp.isRunning());
	}
	
	@Test
	public void test_02() {
		assertFalse(YojaApp.isRunning());
		YojaApp.start();
		assertTrue(YojaApp.isRunning());
		await(YojaApp.restart());
		assertTrue(YojaApp.isRunning());
		YojaApp.stop();
		YojaApp.awaitStop();
		assertFalse(YojaApp.isRunning());
	}
	
	@Test
    public void test_03() {
		YojaApp.vertxOptions().setEventLoopPoolSize(5);
		YojaApp.vertxOptions().setWorkerPoolSize(9);
		
        assertFalse(YojaApp.isRunning());
        YojaApp.start();
        assertTrue(YojaApp.isRunning());
        YojaApp.logOptions();
        assertEquals(5, YojaApp.options().getEventLoopPoolSize());
        assertEquals(9, YojaApp.options().getWorkerPoolSize());
        assertEquals(4, YojaApp.options().getMaxEventLoopExecuteTime());
        assertEquals(TimeUnit.SECONDS, YojaApp.options().getMaxEventLoopExecuteTimeUnit());
        assertEquals(5, YojaApp.options().getMaxWorkerExecuteTime());
        assertEquals(TimeUnit.MINUTES, YojaApp.options().getMaxWorkerExecuteTimeUnit());
        YojaApp.stop();
        YojaApp.awaitStop();
    }
	
	@Test
    public void test_04() {
        assertFalse(YojaApp.isRunning());
        YojaApp.start(new VertxOptions());
        assertTrue(YojaApp.isRunning());
        assertEquals(2000000000, YojaApp.options().getMaxEventLoopExecuteTime());
        assertEquals(TimeUnit.NANOSECONDS, YojaApp.options().getMaxEventLoopExecuteTimeUnit());
        assertEquals(60000000000L, YojaApp.options().getMaxWorkerExecuteTime());
        assertEquals(TimeUnit.NANOSECONDS, YojaApp.options().getMaxWorkerExecuteTimeUnit());
        YojaApp.logOptions();
        YojaApp.stop();
        YojaApp.awaitStop();
    }
	
    @Test
    public void test_05() {
        assertFalse(YojaApp.isRunning());
        YojaApp.start(new VertxOptions().setEventLoopPoolSize(10));
        assertTrue(YojaApp.isRunning());
        assertEquals(10, YojaApp.options().getEventLoopPoolSize());
        assertEquals(20, YojaApp.options().getWorkerPoolSize());
        assertEquals(2000000000, YojaApp.options().getMaxEventLoopExecuteTime());
        assertEquals(TimeUnit.NANOSECONDS, YojaApp.options().getMaxEventLoopExecuteTimeUnit());
        assertEquals(60000000000L, YojaApp.options().getMaxWorkerExecuteTime());
        assertEquals(TimeUnit.NANOSECONDS, YojaApp.options().getMaxWorkerExecuteTimeUnit());
        YojaApp.logOptions();
        YojaApp.stop();
        YojaApp.awaitStop();
    }
    
}
