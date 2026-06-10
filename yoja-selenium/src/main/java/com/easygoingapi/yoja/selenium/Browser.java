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
import java.util.Objects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Catalogue of supported browsers and the Selenium {@link WebDriver}
 * implementations that back each one.
 * <p>
 * Each enum value carries the matching driver class via
 * {@link #getWebDriverClass()}; {@link SeleniumService#newInstance(Config)}
 * uses this mapping to instantiate the right driver from a {@link Config}.
 * A {@link Mode} controls whether the browser runs in headless,
 * developer-friendly headful, or a long-timeout "debugger" mode that pins
 * timeouts to one hour and opens devtools (where supported).
 */
public enum Browser {

    /** Mozilla Firefox, backed by {@link FirefoxDriver}. */
    FIREFOX(FirefoxDriver.class),
    /** Google Chrome, backed by {@link ChromeDriver}. */
    CHROME(ChromeDriver.class),
//    CHROMIUM(ChromiumDriver.class)
    /** Microsoft Edge, backed by {@link EdgeDriver}. */
    EDGE(EdgeDriver.class);
//    SAFARI(SafariDriver.class),
//    OPERA(ChromeDriver.class);

    /** Display/runtime mode of the browser when started by {@link SeleniumService}. */
    public enum Mode {
        /** No window, no devtools — suitable for CI. */
        HEADLESS,
        /** Visible window, no devtools. */
        HEADFUL,
        /** Visible window with devtools open; timeouts pinned to one hour. */
        DEBUGGER
    }

    /** WebDriver implementation backing this browser. */
    private final Class<? extends WebDriver> webDriverClass;

    /**
     * @param webDriverClass the Selenium driver class to use for this browser
     */
    private Browser(final Class<? extends WebDriver> webDriverClass) {
        this.webDriverClass = webDriverClass;
    }

    /**
     * Returns the Selenium driver class to use for this browser.
     *
     * @return the Selenium driver class to use for this browser
     */
    @SuppressWarnings("unchecked")
	public Class<WebDriver> getWebDriverClass() {
        return (Class<WebDriver>) webDriverClass;
    }

    /*
     *
     * CLASS
     *
     */
    /**
     * Bundle of browser-launch settings consumed by
     * {@link SeleniumService#newInstance(Config)}: which {@link Browser} to
     * start, in which {@link Mode}, and with what default timeout for script
     * execution / page loads / implicit waits.
     * <p>
     * Built through {@link Browser#builder(Browser)}; the {@code browser} and
     * {@code mode} setters are private so the only way to construct a valid
     * config is via the builder.
     */
    public static class Config {

        /** Target browser. */
        private Browser browser;
        /** Display/runtime mode. */
        private Mode mode;
        /** Default Selenium timeout (script / page-load / implicit wait). */
        private Duration timeout;

        /** Private — instances are produced through {@link Browser#builder(Browser)}. */
        private Config() {
            super();
        }

        /**
         * Returns the configured browser.
         *
         * @return the configured browser
         */
        public Browser getBrowser() {
            return browser;
        }

        /**
         * @param browser the target browser
         */
        private void setBrowser(final Browser browser) {
            this.browser = browser;
        }

        /**
         * Returns the configured runtime mode.
         *
         * @return the configured runtime mode
         */
        public Mode getMode() {
            return mode;
        }

        /**
         * @param mode the display/runtime mode
         */
        private void setMode(final Mode mode) {
            this.mode = mode;
        }

        /**
         * Returns the configured Selenium timeout, or {@code null} for the {@code SeleniumService} default.
         *
         * @return the configured Selenium timeout, or {@code null} for the {@code SeleniumService} default
         */
        public Duration getTimeout() {
            return timeout;
        }

        /**
         * Sets the Selenium timeout applied to script execution, page loads and implicit waits.
         *
         * @param timeout the Selenium timeout to apply to script execution,
         *                page loads and implicit waits
         */
        public void setTimeout(final Duration timeout) {
            this.timeout = timeout;
        }

        @Override
        public int hashCode() {
            return Objects.hash(browser, mode, timeout);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Config other = (Config) obj;
            return browser == other.browser
                    && mode == other.mode
                    && Objects.equals(timeout, other.timeout);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getClass().getSimpleName());
            builder.append(" [browser=");
            builder.append(browser);
            builder.append(", mode=");
            builder.append(mode);
            builder.append("]");
            return builder.toString();
        }

    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Returns a new builder for a {@link Config} targeting the given browser, defaulting to {@link Mode#HEADFUL}.
     *
     * @param browser the target browser
     * @return a new builder defaulting to {@link Mode#HEADFUL}
     */
    public static Builder builder(final Browser browser) {
        return new Builder(browser);
    }

    /**
     * Fluent builder for {@link Config}. Defaults the runtime mode to
     * {@link Mode#HEADFUL}; the timeout is left {@code null} so the default
     * applied by {@link SeleniumService#newInstance(Config)} takes effect.
     */
    public static class Builder {

        /** Config being assembled. */
        private final Config config = new Config();

        /**
         * @param browser the target browser
         */
        private Builder(final Browser browser) {
            super();
            config.setBrowser(browser);
            config.setMode(Mode.HEADFUL);
        }

        /**
         * Overrides the default {@link Mode#HEADFUL} runtime mode.
         *
         * @param mode runtime mode to apply
         * @return this builder
         */
        public Builder mode(final Mode mode) {
            config.setMode(mode);
            return this;
        }

        /**
         * Sets the Selenium timeout (script / page-load / implicit wait).
         *
         * @param timeout duration to apply
         * @return this builder
         */
        public Builder timeout(final Duration timeout) {
            config.setTimeout(timeout);
            return this;
        }

        /**
         * Returns the assembled {@link Config}.
         *
         * @return the assembled {@link Config}
         */
        public Config build() {
            return config;
        }

    }

}
