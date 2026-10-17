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

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.HttpSession;
import com.easygoingapi.yoja.http.server.HttpSessionStore;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.selenium.Browser.Mode;

/**
 * The session cookie as a real browser sees it: with the secure defaults of
 * {@link HttpSessionStore} ({@code HttpOnly}, {@code Secure},
 * {@code SameSite=Strict}), the browser served over plain HTTP on
 * {@code localhost} keeps the cookie, sends it back (the session lives across
 * requests) and hides it from JavaScript.
 */
public class TestSessionCookie {

    private static final int PORT = 8889;
    private static final String COOKIE_NAME = "session.yoja";

    /** Session value: how many requests the session has served. */
    static record Counter(int value) {}

    @Test
    public void test_chrome() {
        assertSessionCookie(Browser.CHROME);
    }

    @Test
    public void test_firefox() {
        assertSessionCookie(Browser.FIREFOX);
    }

    private static void assertSessionCookie(final Browser browser) {
        final Browser.Config config = Browser.builder(browser)
                                             .mode(Mode.HEADLESS)
                                             .build();
        final HttpServer httpServer = startHttpServer();
        try (final YojaSeleniumService seleniumService = YojaSeleniumService.newInstance(config)) {
            final WebDriver webDriver = seleniumService.webDriver();
            final String url = "http://localhost:" + PORT + "/counter";
            webDriver.get(url);
            assertEquals("1", webDriver.findElement(By.tagName("body")).getText());

            // the browser kept the cookie, with its secure attributes
            final Cookie cookie = webDriver.manage().getCookieNamed(COOKIE_NAME);
            assertNotNull(cookie, browser + ": session cookie not kept");
            assertTrue(cookie.isHttpOnly(), browser + ": " + cookie);
            assertTrue(cookie.isSecure(), browser + ": " + cookie);
            assertEquals("Strict", cookie.getSameSite(), browser + ": " + cookie);
            assertEquals("/", cookie.getPath(), browser + ": " + cookie);

            // ... and sends it back: the same session serves the second request
            webDriver.get(url);
            assertEquals("2", webDriver.findElement(By.tagName("body")).getText());

            // HttpOnly: JavaScript can't read it
            final String documentCookie = seleniumService.executeScript("return document.cookie;");
            assertFalse(documentCookie.contains(COOKIE_NAME), browser + ": " + documentCookie);
        }
        finally {
            await(httpServer.stop());
        }
    }

    private static HttpServer startHttpServer() {
        final WebService webService = new WebService(HttpMethod.GET,
                                                     "/counter",
                                                     h -> {
                                                         final HttpSession session = h.session();
                                                         final Counter counter = session.get(Counter.class);
                                                         final int value = counter == null ? 1 : counter.value() + 1;
                                                         session.put(new Counter(value));
                                                         h.response().send(String.valueOf(value));
                                                     });
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .session(new HttpSessionStore(COOKIE_NAME, Duration.ofMinutes(5)))
                                                .webService(webService)
                                                .build();
        return awaitValue(HttpServer.builder(httpRouter, PORT).start());
    }

}
