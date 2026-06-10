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

import java.time.Duration;
import java.util.List;

import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.selenium.HttpServerContext.HttpUrlBuilder;

/**
 * Per-test context handed to every {@code Consumer<TestContext>} registered
 * through {@link TestBuilder#test}.
 * <p>
 * Bundles together the {@link Browser.Config} the test is running against,
 * the embedded {@link HttpServerContext} serving the test fixtures, and the
 * {@link SeleniumService} driving the browser. Most methods are thin
 * delegators that hide the indirection — {@code getHttpPage} forwards to
 * {@link SeleniumService#getHttpPage(Duration, String)} for instance.
 * <p>
 * Implements {@link AutoCloseable}: closing the context closes both the
 * embedded server and the WebDriver session.
 */
public class TestContext implements AutoCloseable {

    /** Browser configuration the running test is exercising. */
    private final Browser.Config browserConfig;
    /** Embedded HTTP server serving the fixtures. */
    private final HttpServerContext httpServerContext;
    /** Active Selenium driver wrapper. */
    private final SeleniumService seleniumService;

    /**
     * Constructs a test context for the given browser, server, and Selenium service.
     *
     * @param browserConfig     browser config the test is targeting
     * @param httpServerContext embedded server serving the fixtures
     * @param seleniumService   active Selenium wrapper
     */
    public TestContext(final Browser.Config browserConfig,
                       final HttpServerContext httpServerContext,
                       final SeleniumService seleniumService) {
        super();
        this.browserConfig = browserConfig;
        this.httpServerContext = httpServerContext;
        this.seleniumService = seleniumService;
    }

    /**
     * Returns the {@link Browser} the test is targeting.
     *
     * @return the {@link Browser} the test is targeting
     */
    public Browser browser() {
        return browserConfig.getBrowser();
    }

    /**
     * Returns the embedded HTTP server context.
     *
     * @return the embedded HTTP server context
     */
    public HttpServerContext httpServerContext() {
        return httpServerContext;
    }

    /**
     * Returns a fresh URL builder pointing at the embedded server.
     *
     * @return a fresh URL builder pointing at the embedded server
     */
    public HttpUrlBuilder httpUrlBuilder() {
        return httpServerContext.httpUrlBuilder();
    }

    /**
     * Returns the browser-side logs collected so far.
     *
     * @return the browser-side logs collected so far (delegates to {@link SeleniumService#logs()})
     */
    public List<Log> logs() {
        return seleniumService.logs();
    }

    /**
     * Returns the underlying Selenium service.
     *
     * @return the underlying Selenium service
     */
    public SeleniumService seleniumService() {
        return seleniumService;
    }

    /**
     * Navigates the browser to the given URL using the default page-load
     * timeout.
     *
     * @param httpUrl target URL
     */
	public void getHttpPage(final HttpUrl httpUrl) {
		getHttpPage(null, httpUrl);
	}

    /**
     * Navigates the browser to the given URL with an explicit timeout.
     *
     * @param duration page-load timeout (may be {@code null} for the default)
     * @param httpUrl  target URL
     */
    public void getHttpPage(final Duration duration,
                            final HttpUrl httpUrl) {
        seleniumService.getHttpPage(duration, httpUrl.url(Format.encoded));
    }

    /**
     * Navigates the browser to a path relative to the embedded server.
     *
     * @param duration page-load timeout (may be {@code null} for the default)
     * @param httpUrl  path relative to the embedded server
     */
    public void getHttpPage(final Duration duration,
                            final String httpUrl) {
    	getHttpPage(duration, httpUrlBuilder().path(httpUrl)
                                              .build());
    }

    /**
     * Navigates the browser to a path relative to the embedded server, with
     * the default page-load timeout.
     *
     * @param httpUrl path relative to the embedded server
     */
    public void getHttpPage(final String httpUrl) {
		getHttpPage(null, httpUrl);
	}

    /** Closes both the embedded HTTP server and the WebDriver session. */
    @Override
    public void close() {
        httpServerContext.close();
        seleniumService.close();
    }

}
