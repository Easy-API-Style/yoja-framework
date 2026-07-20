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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.ScriptTimeoutException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver.Timeouts;

/**
 * Verifies how {@link YojaSeleniumService} manages the Selenium timeouts
 * (script / page-load / implicit-wait). Each test launches a headless Chrome,
 * so it inspects the timeouts actually applied on the live driver.
 */
public class TestTimeout {

    /** Headless Chrome config, optionally with an explicit timeout ({@code null} = service default). */
    private static Browser.Config headless(final Duration timeout) {
        final Browser.Builder builder = Browser.builder(Browser.CHROME)
                                               .mode(Browser.Mode.HEADLESS);
        if (timeout != null) {
            builder.timeout(timeout);
        }
        return builder.build();
    }

    @Test
    public void test_default_timeout_is_10s() {
        try (YojaSeleniumService service = YojaSeleniumService.newInstance(headless(null))) {
            final Timeouts timeouts = service.timeouts();
            assertEquals(YojaSeleniumService.DEFAULT_TIMEOUT, timeouts.getScriptTimeout());
            assertEquals(YojaSeleniumService.DEFAULT_TIMEOUT, timeouts.getPageLoadTimeout());
            assertEquals(YojaSeleniumService.DEFAULT_TIMEOUT, timeouts.getImplicitWaitTimeout());
        }
    }

    @Test
    @Disabled
    public void test_debugger_timeout_is_1h() {
        try (YojaSeleniumService service = YojaSeleniumService.newInstance(Browser.builder(Browser.CHROME)
                                                                          .mode(Browser.Mode.DEBUGGER)
                                                                          .build())) {
            final Timeouts timeouts = service.timeouts();
            assertEquals(YojaSeleniumService.DEBUGGER_TIMEOUT, timeouts.getScriptTimeout());
            assertEquals(YojaSeleniumService.DEBUGGER_TIMEOUT, timeouts.getPageLoadTimeout());
            assertEquals(YojaSeleniumService.DEBUGGER_TIMEOUT, timeouts.getImplicitWaitTimeout());
        }
    }

    @Test
    public void test_custom_timeout_applied_to_all_three() {
        final Duration custom = Duration.ofSeconds(3);
        try (YojaSeleniumService service = YojaSeleniumService.newInstance(headless(custom))) {
            final Timeouts timeouts = service.timeouts();
            assertEquals(custom, timeouts.getScriptTimeout());
            assertEquals(custom, timeouts.getPageLoadTimeout());
            assertEquals(custom, timeouts.getImplicitWaitTimeout());
        }
    }

    @Test
    public void test_executeAsyncScript_restores_scriptTimeout_after_success() {
        try (YojaSeleniumService service = YojaSeleniumService.newInstance(headless(null))) {
            final String result = service.executeAsyncScript(Duration.ofSeconds(2),
                                                             "const cb = arguments[arguments.length - 1]; cb('ok')");
            assertEquals("ok", result);
            // The explicit per-call timeout must be rolled back to the default.
            assertEquals(YojaSeleniumService.DEFAULT_TIMEOUT, service.timeouts().getScriptTimeout());
        }
    }

    @Test
    public void test_executeAsyncScript_enforces_explicit_timeout() {
        try (YojaSeleniumService service = YojaSeleniumService.newInstance(headless(null))) {
            final long startNanos = System.nanoTime();
            // Script never calls its callback: it must time out at the explicit
            // 1s, far below the 10s default.
            assertThrows(ScriptTimeoutException.class,
                         () -> service.executeAsyncScript(Duration.ofSeconds(1),
                                                          "const cb = arguments[arguments.length - 1]; /* never resolves */"));
            final Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
            assertTrue(elapsed.compareTo(Duration.ofSeconds(6)) < 0,
                       "expected a timeout near 1s (well under the 10s default), but took " + elapsed);
        }
    }

    @Test
    public void test_getHttpPage_restores_pageLoadTimeout_after_success() {
        try (YojaSeleniumService service = YojaSeleniumService.newInstance(headless(null))) {
            // The explicit per-call timeout applies during the load, then the
            // page-load timeout is rolled back to the service default.
            service.getHttpPage(Duration.ofSeconds(5), "about:blank");
            assertEquals(YojaSeleniumService.DEFAULT_TIMEOUT, service.timeouts().getPageLoadTimeout());
        }
    }

    @Test
    public void test_getHttpPage_enforces_pageLoadTimeout() throws IOException {
        // A server socket that accepts connections but never answers, so the
        // browser hangs on the response until the page-load timeout fires.
        final List<Socket> accepted = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            final Thread acceptor = new Thread(() -> {
                try {
                    while (true) {
                        accepted.add(serverSocket.accept());
                    }
                }
                catch (final IOException ignored) {
                    // serverSocket closed at end of test
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            final String url = "http://localhost:" + serverSocket.getLocalPort() + "/";
            try (YojaSeleniumService service = YojaSeleniumService.newInstance(headless(null))) {
                final long startNanos = System.nanoTime();
                // The explicit 2s must abort the never-completing load, far
                // below the 10s default.
                assertThrows(TimeoutException.class,
                             () -> service.getHttpPage(Duration.ofSeconds(2), url));
                final Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
                assertTrue(elapsed.compareTo(Duration.ofSeconds(8)) < 0,
                           "expected a page-load timeout near 2s (under the 10s default), but took " + elapsed);
            }
        }
    }

}
