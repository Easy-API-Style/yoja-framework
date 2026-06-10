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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaAppException;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.core.util.FutureUtil;
import com.easygoingapi.yoja.core.util.ResourceUtil;

/**
 * High-level wrapper around a Selenium {@link WebDriver} tailored to driving
 * Yoja-Web pages in tests.
 * <p>
 * The class adds, on top of the bare Selenium API:
 * <ul>
 *   <li><strong>safer script execution</strong>: every script is wrapped into
 *       a {@code window.ywSeleniumExecuteScript} function and re-injected as
 *       a {@code <script>} tag so it runs in the page's own context with full
 *       module visibility;</li>
 *   <li><strong>retrying variants</strong>: {@link #repeatScript} /
 *       {@link #repeatAsyncScript} re-run a script through a {@link WebDriverWait}
 *       until it returns a non-null value;</li>
 *   <li><strong>storage accessors</strong> for {@code localStorage} /
 *       {@code sessionStorage}, including a structured form keyed by
 *       {@link Storage#date}/{@link Storage#type}/{@link Storage#value};</li>
 *   <li><strong>tag lookup helpers</strong> {@link #firstTag}, {@link #findTags},
 *       {@link #firstTagFrom}, {@link #findTagsFrom} backed by the
 *       {@code yojaWeb} JS API;</li>
 *   <li><strong>page navigation</strong> through {@link #getHttpPage(HttpUrl)}
 *       that also waits for the Yoja-Web runtime to become ready;</li>
 *   <li><strong>browser logs</strong> captured by the {@code ywLogger.js}
 *       helper and exposed via {@link #logs()} / {@link #printLogs()}.</li>
 * </ul>
 * Instances are created through {@link #newInstance(Browser.Config)}, which
 * also picks driver-specific options for headless/debugger modes. In
 * {@code DEBUGGER} mode the timeout is forced to one hour so single-stepping
 * does not hit Selenium timeouts.
 * <p>
 * Implements {@link AutoCloseable}: closing the service calls
 * {@link WebDriver#quit()} and releases the browser.
 */
public class SeleniumService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumService.class);

    /** Underlying Selenium driver. */
    private final WebDriver webDriver;
    /** Same driver, exposed as a JavaScript executor (driver classes always implement both). */
    private final JavascriptExecutor javascriptExecutor;

    /** When {@code true}, every {@link #setTimeouts(Duration)} call is overridden with one hour. */
    private final boolean debugMode;
    /** Default timeout for script execution, page loads and implicit waits. */
    private final Duration timeout;

    /**
     * @param webDriver underlying Selenium driver
     * @param debugMode whether to pin every timeout to one hour
     * @param timeout   default timeout for script execution and page loads
     */
    private SeleniumService(final WebDriver webDriver,
                            final boolean debugMode,
                            final Duration timeout) {
        super();
        this.webDriver = webDriver;
        this.javascriptExecutor = (JavascriptExecutor) webDriver;
        this.debugMode = debugMode;
        this.timeout = timeout;
        setTimeouts(timeout);
    }

    /*
     *
     * GETTER
     *
     */
    /**
     * Returns the underlying Selenium driver.
     *
     * @return the underlying Selenium driver
     */
    public WebDriver webDriver() {
        return webDriver;
    }

    /**
     * Returns the same driver exposed as a {@link JavascriptExecutor}.
     *
     * @return the same driver exposed as a {@link JavascriptExecutor}
     */
    public JavascriptExecutor javascriptExecutor() {
        return javascriptExecutor;
    }

    /*
     *
     * JAVASCRIPT
     *
     */
    /**
     * Reads the script from the given file and executes it with the default
     * timeout.
     *
     * @param script    path to the JS file
     * @param arguments arguments passed to the script
     * @param <V>       return type
     * @return the script's return value
     * @throws SeleniumException when the file cannot be read or the script fails
     */
    public <V> V executeScript(final Path script,
                               final Object... arguments) {
        try {
            return executeScript(Files.readString(script), arguments);
        }
        catch (final Exception e) {
            throw new SeleniumException("execuate javascript failed", e);
        }
    }

    /**
     * Reads the script from the given file and executes it with the supplied
     * timeout.
     *
     * @param duration  script timeout (may be {@code null} for the default)
     * @param script    path to the JS file
     * @param arguments arguments passed to the script
     * @param <V>       return type
     * @return the script's return value
     */
    public <V> V executeScript(final Duration duration,
                               final Path script,
                               final Object... arguments) {
        try {
            return executeScript(duration, Files.readString(script), arguments);
        }
        catch (final Exception e) {
            throw new SeleniumException("execuate javascript failed", e);
        }
    }

    /**
     * Executes the given script body with the default timeout.
     *
     * @param script    JavaScript source
     * @param arguments arguments accessible to the script as {@code arguments[0..]}
     * @param <V>       return type
     * @return the script's return value
     */
    public <V> V executeScript(final String script,
                               final Object... arguments) {
        return executeScript(null, script, arguments);
    }

    /**
     * Executes the given script body with an explicit timeout.
     *
     * @param duration  script timeout (may be {@code null} for the default)
     * @param script    JavaScript source
     * @param arguments arguments accessible to the script as {@code arguments[0..]}
     * @param <V>       return type
     * @return the script's return value
     */
    @SuppressWarnings("unchecked")
    public <V> V executeScript(final Duration duration,
                               final String script,
                               final Object... arguments) {
        setTimeouts(duration);
        return (V) _executeScript(script, arguments);
    }

    /**
     * Re-runs {@code script} through a {@link WebDriverWait} until it returns
     * a non-null value or the configured deadline elapses. Exceptions thrown
     * by the script are logged and treated as a null result so the wait keeps
     * retrying.
     *
     * @param until     polling deadline
     * @param script    JavaScript source
     * @param arguments script arguments
     * @param <V>       return type
     * @return the first non-null script result
     */
    @SuppressWarnings("unchecked")
    public <V> V repeatScript(final Duration until,
                              final String script,
                              final Object... arguments) {
//    	Object result = null;
//    	for (int i = 0; i < 6; i++) {
//    		result =  _executeScript(script, arguments);
//    		if (result != null) {
//    			break;
//    		}
//    	}
//    	return (V) result;

        final WebDriverWait webDriverWait = new WebDriverWait(webDriver, setTimeouts(until));
        return webDriverWait.until(c -> {
            try {
//            	await(Duration.ofSeconds(4));
                return (V) _executeScript(script, arguments);
            }
            catch (final Exception e) {
                LOGGER.warn("{}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * Reads an async script from a file and executes it.
     *
     * @param script    path to the JS file
     * @param arguments arguments passed to the script
     * @param <V>       return type
     * @return the value the script handed to its callback
     */
    public <V> V executeAsyncScript(final Path script,
                                    final Object... arguments) {
        try {
            return executeAsyncScript(Files.readString(script), arguments);
        }
        catch (final Exception e) {
            throw new SeleniumException("execuate javascript failed", e);
        }
    }

    /**
     * Reads an async script from a file and executes it with an explicit
     * timeout.
     *
     * @param duration  script timeout (may be {@code null} for the default)
     * @param script    path to the JS file
     * @param arguments arguments passed to the script
     * @param <V>       return type
     * @return the value the script handed to its callback
     */
    public <V> V executeAsyncScript(final Duration duration,
                                    final Path script,
                                    final Object... arguments) {
        try {
            return executeAsyncScript(duration, Files.readString(script), arguments);
        }
        catch (final Exception e) {
            throw new SeleniumException("execuate javascript failed", e);
        }
    }

    /**
     * Executes the given async script body with the default timeout.
     *
     * @param script    JavaScript source (must call its last argument as a callback)
     * @param arguments script arguments
     * @param <V>       return type
     * @return the value the script handed to its callback
     */
    public <V> V executeAsyncScript(final String script,
                                    final Object... arguments) {
        return executeAsyncScript(null, script, arguments);
    }

    /**
     * Executes the given async script body with an explicit timeout.
     *
     * @param duration  script timeout (may be {@code null} for the default)
     * @param script    JavaScript source (must call its last argument as a callback)
     * @param arguments script arguments
     * @param <V>       return type
     * @return the value the script handed to its callback
     */
    @SuppressWarnings("unchecked")
    public <V> V executeAsyncScript(final Duration duration,
                                    final String script,
                                    final Object... arguments) {
        setTimeouts(duration);
        return (V) _executeAsyncScript(script, arguments);
    }

    /**
     * Async counterpart of {@link #repeatScript(Duration, String, Object...)}:
     * re-runs the async script through a {@link WebDriverWait} until it
     * succeeds or the deadline elapses.
     *
     * @param until     polling deadline
     * @param script    JavaScript source
     * @param arguments script arguments
     * @param <V>       return type
     * @return the first non-null script result
     */
    @SuppressWarnings("unchecked")
    public <V> V repeatAsyncScript(final Duration until,
                                   final String script,
                                   final Object... arguments) {
        final WebDriverWait webDriverWait = new WebDriverWait(webDriver, setTimeouts(until));
        return webDriverWait.until(c -> {
            try {
                return (V) _executeAsyncScript(script, arguments);
            }
            catch (final Exception e) {
                LOGGER.warn("{}", e.getMessage());
                return null;
            }
        });
    }

    /** JS template that wraps the user script into a {@code window.ywSeleniumExecuteScript} function. */
    private static String ywSeleniumExecuteScript = """
        window.ywSeleniumExecuteScript = function(arguments) {
           {{script}}
        }
    """;

    /**
     * JS template that re-injects the wrapped script as a {@code <script>}
     * tag (so it runs in the page's own module scope) and then invokes
     * {@code window.ywSeleniumExecuteScript} with the original arguments.
     */
    private static String appendYwSeleniumScript = """
        let scriptTag = document.getElementById('ywSeleniumScript')
        if (scriptTag) {
            scriptTag.remove()
        }
        const indexScript = {{index}}
        scriptTag = document.createElement('script')
        scriptTag.id = 'ywSeleniumScript'
        scriptTag.innerHTML = arguments[indexScript]
        if (document.head) {
            document.head.appendChild(scriptTag)
        }
        else {
            document.appendChild(script)
        }
        const functionArguments = []
        for (let i = 0; i < arguments.length; i++) {
            if (i !== indexScript) {
                functionArguments.push(arguments[i])
            }
        }
        return window.ywSeleniumExecuteScript(functionArguments)
    """;

    /**
     * Appends the wrapped script to the caller's argument list (it travels as
     * the last positional argument) so the loader template can pluck it out
     * by index.
     *
     * @param executeScript the wrapped user script
     * @param arguments     original caller arguments
     * @return a new array carrying both
     */
    private static Object[] manageArguments(final String executeScript,
                                            final Object... arguments) {
        final Object[] result;
        if (arguments != null) {
            result = new Object[arguments.length + 1];
            int i = 0;
            for (final Object argument : arguments) {
                result[i] = argument;
                i++;
            }
            result[i] = executeScript;
        }
        else {
            result = new Object[1];
            result[0] = executeScript;
        }
        return result;
    }

    /**
     * Computes the position of the wrapped-script entry in the argument array
     * built by {@link #manageArguments(String, Object...)}.
     *
     * @param _arguments augmented argument array
     * @return the index of the wrapped script
     */
    private static int workOutIndex(final Object[] _arguments) {
        int index;
        if (_arguments.length == 0) {
            index = 0;
        }
        else {
            index = _arguments.length - 1;
        }
        return index;
    }

    /**
     * Synchronous script execution path: wraps the user script, prepends the
     * loader template and hands the whole thing to Selenium's
     * {@code executeScript}.
     *
     * @param script    raw user script
     * @param arguments user-supplied arguments
     * @param <V>       return type
     * @return the script's return value
     */
    @SuppressWarnings("unchecked")
    private <V> V _executeScript(final String script,
                                 final Object... arguments) {
        final String executeScript = ywSeleniumExecuteScript.replace("{{script}}", script);
        final Object[] _arguments = manageArguments(executeScript, arguments);
        final int index = workOutIndex(_arguments);
        return (V) javascriptExecutor.executeScript(appendYwSeleniumScript.replace("{{index}}",
                                                    String.valueOf(index)),
                                                    _arguments);
    }

    /**
     * Async counterpart of {@link #_executeScript(String, Object...)}; uses
     * Selenium's {@code executeAsyncScript} instead.
     *
     * @param script    raw user script
     * @param arguments user-supplied arguments
     * @param <V>       return type
     * @return the value the script handed to its callback
     */
    @SuppressWarnings("unchecked")
    private <V> V _executeAsyncScript(final String script,
                                      final Object... arguments) {
        final String executeScript = ywSeleniumExecuteScript.replace("{{script}}", script);
        final Object[] _arguments = manageArguments(executeScript, arguments);
        final int index = workOutIndex(_arguments);
        return (V) javascriptExecutor.executeAsyncScript(appendYwSeleniumScript.replace("{{index}}",
                                                         String.valueOf(index)),
                                                         _arguments);
    }

    /*
     *
     * STORAGE
     *
     */
    /**
     * Field names of the structured {@code yojaWebItemKey__*} entries kept in
     * the browser's storage: when Yoja-Web stores a value, it wraps it in an
     * object with these three properties.
     */
    public static enum Storage {
        /** Last-update timestamp of the entry. */
        date,
        /** Logical type tag. */
        type,
        /** Stored value. */
        value
    }

    /**
     * Reads a raw {@code localStorage} entry.
     *
     * @param key entry key
     * @param <V> expected return type
     * @return the stored value, or {@code null} when missing
     */
    @SuppressWarnings("unchecked")
	public <V> V localStorage(final String key) {
		final Object result = executeScript("""
		    return localStorage.getItem(arguments[0])
		""", key);
		return (V) result;
    }

    /**
     * Reads a sub-field ({@link Storage#date} / {@link Storage#type} /
     * {@link Storage#value}) of a structured {@code yojaWebItemKey__*} entry.
     *
     * @param key     logical entry key (without the {@code yojaWebItemKey__} prefix)
     * @param storage which sub-field to read
     * @param <V>     expected return type
     * @return the sub-field value, or {@code null} when missing
     */
    @SuppressWarnings("unchecked")
    public <V> V localStorage(final String key,
                              final Storage storage) {
        final Object result = executeScript("""
            const value = localStorage.getItem('yojaWebItemKey__' + arguments[0])
            if (value) {
                return JSON.parse(value)[arguments[1]]
            }
            return undefined
        """, key, storage.name());
        return (V) result;
    }

    /**
     * Reads a raw {@code sessionStorage} entry.
     *
     * @param key entry key
     * @param <V> expected return type
     * @return the stored value, or {@code null} when missing
     */
    @SuppressWarnings("unchecked")
	public <V> V sessionStorage(final String key) {
    	final Object result = executeScript("""
		    return sessionStorage.getItem(arguments[0])
		""", key);
		return (V) result;
    }

    /**
     * Reads a sub-field of a structured {@code yojaWebItemKey__*} entry from
     * {@code sessionStorage}.
     *
     * @param key     logical entry key
     * @param storage which sub-field to read
     * @param <V>     expected return type
     * @return the sub-field value, or {@code null} when missing
     */
    @SuppressWarnings("unchecked")
    public <V> V sessionStorage(final String key,
                                final Storage storage) {
        final Object result = executeScript("""
            const value = sessionStorage.getItem('yojaWebItemKey__' + arguments[0])
            if (value) {
                return JSON.parse(value)[arguments[1]]
            }
            return undefined
        """, key, storage.name());
        return (V) result;
    }

    /*
     *
     * TAG
     *
     */
    /**
     * Returns the first DOM element matching the given CSS selector, or {@code null}.
     *
     * @param cssSelector CSS selector
     * @return the first DOM element matching {@code cssSelector}, or {@code null}
     */
    public WebElement firstTag(final String cssSelector) {
        return firstTag(null, cssSelector);
    }

    /**
     * Returns the first DOM element matching the given CSS selector, waiting up to {@code duration}.
     *
     * @param duration    selector timeout (may be {@code null} for the default)
     * @param cssSelector CSS selector
     * @return the first DOM element matching {@code cssSelector}, or {@code null}
     */
    public WebElement firstTag(final Duration duration,
                               final String cssSelector) {
        final Object result = executeScript(duration, """
            return yojaWeb.firstTag(arguments[0])
        """, cssSelector);
        return result != null
                ? (WebElement) result
                : null;
    }

    /**
     * Returns the first descendant of {@code formTag} matching the given CSS selector.
     *
     * @param formTag     element to scope the search under
     * @param cssSelector CSS selector
     * @return the first descendant of {@code formTag} matching {@code cssSelector}, or {@code null}
     */
    public WebElement firstTagFrom(final WebElement formTag,
                                   final String cssSelector) {
        return firstTagFrom(null, formTag, cssSelector);
    }

    /**
     * Returns the first descendant of {@code formTag} matching the given CSS selector, waiting up to {@code duration}.
     *
     * @param duration    selector timeout (may be {@code null} for the default)
     * @param formTag     element to scope the search under
     * @param cssSelector CSS selector
     * @return the first descendant of {@code formTag} matching {@code cssSelector}, or {@code null}
     */
    public WebElement firstTagFrom(final Duration duration,
                                   final WebElement formTag,
                                   final String cssSelector) {
        final Object result = executeScript(duration, """
            if (!arguments[0]) return null
            return yojaWeb.firstTag(arguments[1], arguments[0])
        """, formTag, cssSelector);
        return result != null
                ? (WebElement) result
                : null;
    }

    /**
     * Returns every DOM element matching the given CSS selector.
     *
     * @param cssSelector CSS selector
     * @return every DOM element matching {@code cssSelector}; empty when none
     */
    public List<WebElement> findTags(final String cssSelector) {
        return findTags(null, cssSelector);
    }

    /**
     * Returns every DOM element matching the given CSS selector, waiting up to {@code duration}.
     *
     * @param duration    selector timeout (may be {@code null} for the default)
     * @param cssSelector CSS selector
     * @return every DOM element matching {@code cssSelector}; empty when none
     */
    @SuppressWarnings("unchecked")
    public List<WebElement> findTags(final Duration duration,
                                     final String cssSelector) {
        final Object result = executeScript(duration, """
            return yojaWeb.findTags(arguments[0])
        """, cssSelector);
        return result instanceof List<?>
                ? (List<WebElement>) result
                : new ArrayList<>();
    }

    /**
     * Returns every descendant of {@code formTag} matching the given CSS selector.
     *
     * @param formTag     element to scope the search under
     * @param cssSelector CSS selector
     * @return every descendant of {@code formTag} matching {@code cssSelector}; empty when none
     */
    public List<WebElement> findTagsFrom(final WebElement formTag,
                                         final String cssSelector) {
        return findTagsFrom(null, formTag, cssSelector);
    }

    /**
     * Returns every descendant of {@code formTag} matching the given CSS selector, waiting up to {@code duration}.
     *
     * @param duration    selector timeout (may be {@code null} for the default)
     * @param formTag     element to scope the search under
     * @param cssSelector CSS selector
     * @return every descendant of {@code formTag} matching {@code cssSelector}; empty when none
     */
    @SuppressWarnings("unchecked")
    public List<WebElement> findTagsFrom(final Duration duration,
                                         final WebElement formTag,
                                         final String cssSelector) {
        final Object result = executeScript(duration, """
            if (!arguments[0]) return null
            return yojaWeb.findTags(arguments[1], arguments[0])
        """, formTag, cssSelector);
        return result instanceof List<?>
                ? (List<WebElement>) result
                : new ArrayList<>();
    }

    /*
     *
     * PAGE
     *
     */
    /**
     * Navigates to the given {@link HttpUrl} with the default timeout.
     *
     * @param httpUrl target URL
     */
    public void getHttpPage(final HttpUrl httpUrl) {
    	getHttpPage(null, httpUrl.url(Format.encoded));
    }

    /**
     * Navigates to the given {@link HttpUrl} with an explicit timeout.
     *
     * @param duration page-load timeout (may be {@code null} for the default)
     * @param httpUrl  target URL
     */
    public void getHttpPage(final Duration duration,
                            final HttpUrl httpUrl) {
        getHttpPage(duration, httpUrl.url(Format.encoded));
    }

    /**
     * Navigates to the given URL with the default timeout.
     *
     * @param url target URL
     */
    public void getHttpPage(final String url) {
    	getHttpPage(null, url);
    }

    /**
     * Navigates to the given URL, sleeps one second to let the page begin
     * loading, then polls until the Yoja-Web runtime reports the page ready
     * (or the page has no Yoja markers at all, in which case the wait
     * returns immediately).
     *
     * @param duration page-load timeout (may be {@code null} for the default)
     * @param url      target URL
     */
    public void getHttpPage(final Duration duration,
                            final String url) {
        webDriver.get(url);
        await(Duration.ofSeconds(1));
        awaitYojaJsHttpPageReady(duration);
    }

    /**
     * Polls the page through {@link #repeatScript} until the Yoja-Web runtime
     * reports it ready (or until the page exposes no Yoja markers at all).
     *
     * @param duration polling deadline
     */
    private void awaitYojaJsHttpPageReady(final Duration duration) {
        repeatScript(duration, """
            const isReady = function() {
                const hasYojaElements = document.querySelector('[yw-include],[yw-controler],[yw-css],[yw-language],[yw-slot]');
                // console.info('yoja page Ready: ' + hasYojaElements)
                if (hasYojaElements || window.yojaWeb) {
                    return window.yojaWeb && window.yojaWeb.isReady() ? true : null;
                }
                else {
                    return true;
                }
            };
            return window ? isReady() : null
        """);
    }

    /** Reloads the page with no extra script options ({@link #reload(ScriptOption)}). */
    public void reload() {
        reload(ScriptOption.apply());
    }

    /**
     * Reloads the page, waits for the Yoja-Web runtime to become ready, then
     * optionally re-installs the {@code ywLogger.js} and {@code ywAssert.js}
     * helpers as instructed by {@code scriptOption}.
     *
     * @param scriptOption which post-reload helpers to re-install
     */
    public void reload(final ScriptOption scriptOption) {
        executeScript("location.reload()");
        await(Duration.ofSeconds(1));
        awaitYojaJsHttpPageReady(timeout);
        if (scriptOption.wyLogger) {
            saveLogs();
        }
        if (scriptOption.ywAssert) {
            loadYwAssert();
        }
    }

    /**
     * Sleeps for {@code duration} on the test thread. Any
     * {@link InterruptedException} is wrapped in a {@link SeleniumException}.
     *
     * @param duration how long to sleep
     */
    public void await(final Duration duration) {
        try {
            Thread.sleep(duration);
        }
        catch (final InterruptedException e) {
            throw new SeleniumException(e);
        }
    }

    /**
     * Resizes the browser window.
     *
     * @param width  target width in pixels
     * @param heigth target height in pixels
     */
    public void resizeWindow(final int width,
    		                 final int heigth) {
    	webDriver.manage()
    	         .window()
    	         .setSize(new Dimension(width, heigth));
    }

    /*
     *
     * UTIL
     *
     */
    /**
     * Applies the script / page-load / implicit-wait timeouts on the driver.
     * In debug mode every call returns one hour regardless of the input. Out
     * of debug mode, the supplied {@code duration} wins; when {@code null},
     * the constructor-time default is used.
     *
     * @param duration requested timeout (may be {@code null})
     * @return the effective timeout actually applied
     */
    private Duration setTimeouts(final Duration duration) {
        final Duration result;
        if (debugMode) {
            result = Duration.ofHours(1);
        }
        else if (duration != null) {
            result = duration;
        }
        else {
            result = timeout;
        }
        webDriver.manage()
                 .timeouts()
                 .scriptTimeout(result)
                 .pageLoadTimeout(result)
                 .implicitlyWait(result);
        return result;
    }

    /**
     * Pause-point helper backed by {@link Debugger#debugger()}. Set a
     * breakpoint on that method to halt the test thread here.
     */
    public void debugger() {
        Debugger.debugger();
    }

    /*
     *
     * JAVASCRIPT
     *
     */
    /**
     * Injects {@code script} as a new {@code <script>} tag in the page's head
     * (or root when no head is present) and waits for it to finish executing.
     * The script can rely on a global {@code window.load.callback()} hook to
     * signal completion; this method tacks the call at the end automatically.
     *
     * @param script JavaScript source to inject
     */
    public void loadJavascript(final String script) {
        final StringBuilder javascript = new StringBuilder(script);
        javascript.append(System.lineSeparator());
        javascript.append("window.load.callback()");
        executeAsyncScript("""
             const callback = arguments[arguments.length - 1]
             window.load = {}
             window.load.callback = function() {
                 callback()
             }
             const script = document.createElement('script')
             script.innerHTML = arguments[0]
             if (document.head) {
                 document.head.appendChild(script)
             }
             else {
                 document.appendChild(script)
             }
        """, javascript.toString());
    }

    /*
     *
     * LOGS
     *
     */
    /**
     * Reads the in-page {@code window.yojaWebLogs} array maintained by the
     * {@code ywLogger.js} helper and converts each raw entry into a {@link Log}.
     *
     * @return the captured log entries; empty when the helper is not loaded
     */
    public List<Log> logs() {
        final List<Map<String, Object>> logs = executeScript("return window ? window.yojaWebLogs : []");
        final List<Log> result = new ArrayList<>();
        if (logs != null) {
        	 for (final Map<String, Object> log : logs) {
                 final Object message = log.get("message");
                 result.add(new Log(Instant.ofEpochMilli((Long) log.get("date")),
                                    Log.Level.valueOf(log.get("level").toString()),
                                    message != null ? message.toString() : ""));
             }
        }
        return result;
    }

    /** Clears the in-page {@code window.yojaWebLogs} array. */
    public void clearLogs() {
    	executeScript("window.yojaWebLogs = []");
    }

    /**
     * Ensures the {@code ywLogger.js} helper is installed: re-loads it only
     * when {@code window.yojaWebLogs} is not yet an array (i.e. when the
     * helper has not run on this page).
     */
    public void saveLogs() {
    	final Boolean isSaving = executeScript("return Array.isArray(window.yojaWebLogs) ? true : false");
    	if (!isSaving) {
    		 loadJavascript(ResourceUtil.read("com/easygoingapi/yoja/selenium/ywLogger.js"));
    	}
    }

    /** Loads the {@code ywAssert.js} helper so test code in the page can use {@code ywAssert}. */
    public void loadYwAssert() {
        loadJavascript(ResourceUtil.read("com/easygoingapi/yoja/selenium/js/ywAssert.js"));
    }

    /**
     * Drains {@link #logs()} into the SLF4J logger at a severity matching each
     * entry's {@link Log.Level}, prints a header/footer banner around the
     * batch, and clears the in-page log buffer.
     */
    public void printLogs() {
        //
        final List<Log> logs = logs();
        clearLogs();
        if (logs != null) {
            LOGGER.info("########################################");
            LOGGER.info("############# browser logs #############");
            LOGGER.info("########################################");
            for (final Log log : logs) {
                if (Log.Level.TRACE == log.level()) {
                    LOGGER.trace("[{}] [{}] {}",
                                 DateUtil.formatTimestamp(log.date()),
                                 log.level(),
                                 log.message());
                }
                else if (Log.Level.LOG == log.level()) {
                    LOGGER.info("[{}]   [{}] {}",
                                DateUtil.formatTimestamp(log.date()),
                                log.level(),
                                log.message());
                }
                else if (Log.Level.INFO == log.level()) {
                    LOGGER.info("[{}]  [{}] {}",
                                DateUtil.formatTimestamp(log.date()),
                                log.level(),
                                log.message());
                }
                else if (Log.Level.DEBUG == log.level()) {
                    LOGGER.debug("[{}] [{}] {}",
                                 DateUtil.formatTimestamp(log.date()),
                                 log.level(),
                                 log.message());
                }
                else if (Log.Level.WRAN == log.level()) {
                    LOGGER.warn("[{}]  [{}] {}",
                                DateUtil.formatTimestamp(log.date()),
                                log.level(),
                                log.message());
                }
                else if (Log.Level.ERROR == log.level()) {
                    LOGGER.error("[{}] [{}] {}",
                                 DateUtil.formatTimestamp(log.date()),
                                 log.level(),
                                 log.message());
                }
            }
            LOGGER.info("----------------------------------------");
            LOGGER.info("------------- browser logs -------------");
            LOGGER.info("----------------------------------------");
        }
    }

    /** Quits the underlying {@link WebDriver}; the service must not be used after this call. */
    @Override
    public void close() {
        webDriver.quit();
    }

    /*
     *
     * BUILDER
     *
     */
    /**
     * Creates a Selenium service for the given browser configuration:
     * instantiates the right driver class with the appropriate options for
     * the requested {@link Browser.Mode}, maximizes the window in
     * {@code DEBUGGER} mode (where supported), and applies a default 10-second
     * timeout when none was configured.
     *
     * @param browserConfig browser configuration produced by {@link Browser#builder(Browser)}
     * @return a Selenium service driving the freshly-launched browser
     * @throws YojaAppException when the browser is missing from the configuration
     */
    public static SeleniumService newInstance(final Browser.Config browserConfig) {
        final WebDriver webDriver;
        if (browserConfig.getBrowser() == Browser.FIREFOX) {
            final FirefoxOptions firefoxOptions = new FirefoxOptions();
            if (Browser.Mode.HEADLESS == browserConfig.getMode()) {
                firefoxOptions.addArguments("-headless");
            }
            else if (Browser.Mode.DEBUGGER == browserConfig.getMode()) {
                firefoxOptions.addArguments("-devtools");
            }
//            final FirefoxDriverService firefoxDriverService =
//                    new GeckoDriverService.Builder()
//                                          .withTruncatedLogs(false)
//                                          .withLogLevel(FirefoxDriverLogLevel.INFO)
//                                          .withLogOutput(System.out)
//                                          .build();
            webDriver = new FirefoxDriver(firefoxOptions);
            if (Browser.Mode.DEBUGGER == browserConfig.getMode()) {
				FutureUtil.sleep(1000);
				webDriver.manage().window().maximize();
			}
        }
        else if (browserConfig.getBrowser() == Browser.CHROME) {
            final ChromeOptions chromeOptions = new ChromeOptions();
            if (Browser.Mode.HEADLESS == browserConfig.getMode()) {
                chromeOptions.addArguments("-headless");
            }
            else if (Browser.Mode.DEBUGGER == browserConfig.getMode()) {
                chromeOptions.addArguments("--auto-open-devtools-for-tabs");
            }
//            final ChromeDriverService chromeDriverService =
//                    new ChromeDriverService.Builder()
//                                           .withLogLevel(ChromiumDriverLogLevel.INFO)
//                                           .withLogOutput(System.out)
//                                           .build();
            webDriver = new ChromeDriver(chromeOptions);
//			if (Browser.Mode.DEBUGGER == browserConfig.getMode()) {
//				FutureUtil.sleep(2000);
//				webDriver.manage().window().maximize();
//			}
        }
        else if (browserConfig.getBrowser() == Browser.EDGE) {
			final EdgeOptions edgeOptions = new EdgeOptions();
			if (Browser.Mode.HEADLESS == browserConfig.getMode()) {
				edgeOptions.addArguments("-headless");
			}
			else if (Browser.Mode.DEBUGGER == browserConfig.getMode()) {
				edgeOptions.addArguments("--auto-open-devtools-for-tabs");
			}
			webDriver = new EdgeDriver(edgeOptions);
			if (Browser.Mode.DEBUGGER == browserConfig.getMode()) {
				FutureUtil.sleep(2000);
				webDriver.manage().window().maximize();
			}
        }
        else {
            throw new YojaAppException("need browser config (@see Browser.Config class)");
        }
        final boolean debugMode = Browser.Mode.DEBUGGER == browserConfig.getMode();
        final Duration timeout = browserConfig.getTimeout() != null
                                    ? browserConfig.getTimeout()
                                    : Duration.ofSeconds(10);
        return new SeleniumService(webDriver, debugMode, timeout);
    }

}
