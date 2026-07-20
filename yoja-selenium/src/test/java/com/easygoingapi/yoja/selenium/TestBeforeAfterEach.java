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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * Verifies the per-step / per-browser hooks of {@link YojaSeleniumBuilder} (JUnit's own
 * {@code @BeforeEach}/{@code @AfterEach} only fire around the {@code @TestFactory} method, not the
 * individual dynamic steps):
 * <ul>
 *   <li>{@code beforeEach}/{@code afterEach} run around <b>every</b> step;</li>
 *   <li>{@code beforeEachBrowser} runs <b>once</b>, before the browser's first step;</li>
 *   <li>{@code afterEachBrowser} runs <b>once</b>, after the browser's last step — asserted in
 *       {@code @AfterAll}, since it fires after all the dynamic steps have run.</li>
 * </ul>
 * The steps do no browser work; they only assert the hook counters, so the check is fast.
 */
public class TestBeforeAfterEach {

    private static final AtomicInteger before = new AtomicInteger();
    private static final AtomicInteger after = new AtomicInteger();
    private static final AtomicInteger beforeBrowser = new AtomicInteger();
    private static final AtomicInteger afterBrowser = new AtomicInteger();

    @TestFactory
    Stream<DynamicNode> hooksRunAtTheRightTime() {
        return YojaSeleniumBuilder.builder()
                          .browser(Browser.builder(Browser.CHROME)
                                          .mode(Browser.Mode.HEADLESS)
                                          .build())
                          .beforeEachBrowser(ctx -> beforeBrowser.incrementAndGet())
                          .afterEachBrowser(ctx -> afterBrowser.incrementAndGet())
                          .beforeEach(ctx -> before.incrementAndGet())
                          .afterEach(ctx -> after.incrementAndGet())
                          // before step 1: browser hook + beforeEach ran once; nothing "after" yet
                          .test("step 1", ctx -> {
                              assertEquals(1, beforeBrowser.get(), "beforeEachBrowser runs once before the first step");
                              assertEquals(1, before.get(), "beforeEach runs before step 1");
                              assertEquals(0, after.get(), "afterEach has not run yet");
                              assertEquals(0, afterBrowser.get(), "afterEachBrowser does not run during the steps");
                          })
                          // step 2: browser hook still once, beforeEach 2, afterEach 1
                          .test("step 2", ctx -> {
                              assertEquals(1, beforeBrowser.get(), "beforeEachBrowser does not run again");
                              assertEquals(2, before.get(), "beforeEach runs before step 2");
                              assertEquals(1, after.get(), "afterEach ran after step 1");
                              assertEquals(0, afterBrowser.get(), "afterEachBrowser still not run");
                          })
                          // step 3: browser hook still once, beforeEach 3, afterEach 2
                          .test("step 3", ctx -> {
                              assertEquals(1, beforeBrowser.get(), "beforeEachBrowser does not run again");
                              assertEquals(3, before.get(), "beforeEach runs before step 3");
                              assertEquals(2, after.get(), "afterEach ran after step 2");
                              assertEquals(0, afterBrowser.get(), "afterEachBrowser still not run");
                          })
                          .stream();
    }

    @AfterAll
    static void assertBrowserHooks() {
        // by now every dynamic step has run: the per-browser hooks fired exactly once each,
        // and afterEach fired after each of the 3 steps
        assertEquals(1, beforeBrowser.get(), "beforeEachBrowser ran once for the browser");
        assertEquals(1, afterBrowser.get(), "afterEachBrowser ran once, after the browser's last step");
        assertEquals(3, before.get(), "beforeEach ran before every step");
        assertEquals(3, after.get(), "afterEach ran after every step");
    }
}
