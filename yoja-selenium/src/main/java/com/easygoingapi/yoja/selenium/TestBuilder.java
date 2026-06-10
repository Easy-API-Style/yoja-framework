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

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.core.util.ResourceUtil;
import com.easygoingapi.yoja.core.util.TimeUtil;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.WebApp;
import com.easygoingapi.yoja.http.server.WebApp.Type;
import com.easygoingapi.yoja.http.server.WebResource;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.http.server.WebSocket;
import com.easygoingapi.yoja.http.server.WebSocketService;

/**
 * Fluent orchestrator for browser-driven test scenarios.
 * <p>
 * A {@code TestBuilder} accumulates the moving parts of a Selenium scenario —
 * browser configurations, embedded HTTP server fixtures, WebSocket endpoints,
 * page navigations, JavaScript module tests, log/debug operations — and then
 * either runs them directly via {@link #execute()} or exposes them as JUnit 5
 * {@link DynamicNode}s through {@link #stream()} for use in a
 * {@code @TestFactory}-annotated method.
 * <p>
 * Each registered "step" is a {@link Test} record pairing a label with a
 * {@code Consumer<TestContext>}. When {@link #execute(boolean)} runs, every
 * browser config is exercised in sequence: a fresh {@link SeleniumService}
 * and {@link TestContext} are created, the embedded {@link HttpServerContext}
 * is started, each step runs in declaration order, and (unless
 * {@code keepRunning} is true) the context is closed at the end.
 * <p>
 * Two specialised entry points exist to bootstrap the Yoja-Web runtime in
 * the page:
 * <ul>
 *   <li>{@link #startJavascript()} loads a minimal {@code hello.html} fixture
 *       that simply boots a vanilla JavaScript runtime;</li>
 *   <li>{@link #startYojaWeb()} loads {@code yojaWeb.html}, the full
 *       Yoja-Web bootstrap.</li>
 * </ul>
 * Both variants accept a {@link ScriptOption} to tell the post-load step
 * whether to install the {@code ywLogger.js} / {@code ywAssert.js} helpers.
 */
public class TestBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestBuilder.class);

    /**
     * A single test step.
     *
     * @param name   human-readable label (also used as the JUnit dynamic-test display name)
     * @param action work to run against the per-browser {@link TestContext}
     */
    private static record Test(String name, Consumer<TestContext> action) {}

    /** Browser configurations to exercise (iteration order preserved via {@link LinkedHashSet}). */
    private final LinkedHashSet<Browser.Config> browserConfigs = new LinkedHashSet<>();
    /** Test steps in declaration order. */
    private final List<Test> tests = new ArrayList<>();

    /** JS-unit web apps mounted as fixtures on the embedded server. */
    private final List<WebApp> jsUnitWebApps = new ArrayList<>();

    /** WebSocket service receiving any endpoint added via {@link #webSocket(WebSocket)}. */
    private final WebSocketService webSocketService = new WebSocketService();
    /** Additional services / static resources to register on the embedded server. */
    private final List<WebService> webServices = new ArrayList<>();
    /** Extension → Content-Type table for the embedded server. */
    private final Map<String, String> contentTypes = new TreeMap<>();

    /** Host the embedded server binds to. */
    private String host = "localhost";

    /** Guard preventing {@link #startJavascript()} / {@link #startYojaWeb()} from running twice. */
    private boolean doStartYojaJs;

    /** Public no-arg constructor; instances may also be obtained through {@link #builder()}. */
    public TestBuilder() {
        super();
    }

    /*
     *
     * BROWSER
     *
     */
    /**
     * Adds a browser configuration to exercise.
     *
     * @param config browser configuration (no-op when already registered)
     * @return this builder
     */
    public TestBuilder browser(final Browser.Config config) {
        this.browserConfigs.add(config);
        return this;
    }

    /**
     * Overrides the default {@code localhost} host of the embedded server.
     *
     * @param host host name to bind to
     * @return this builder
     */
    public TestBuilder host(final String host) {
        this.host = host;
        return this;
    }

    /*
     *
     * WEBSERVICE
     *
     */
    /**
     * Boots a minimal JavaScript-enabled fixture page with no extra options.
     *
     * @return this builder
     */
    public TestBuilder startJavascript() {
    	return start(ScriptOption.apply(), false);
    }

    /**
     * Boots a minimal JavaScript-enabled fixture page with the supplied options.
     *
     * @param yojaWebOption post-load options (logger / assert helpers)
     * @return this builder
     */
    public TestBuilder startJavascript(final ScriptOption yojaWebOption) {
    	return start(yojaWebOption, false);
    }

    /**
     * Boots the full Yoja-Web runtime fixture page with no extra options.
     *
     * @return this builder
     */
    public TestBuilder startYojaWeb() {
        return start(ScriptOption.apply(), true);
    }

    /**
     * Boots the full Yoja-Web runtime fixture page with the supplied options.
     *
     * @param yojaWebOption post-load options (logger / assert helpers)
     * @return this builder
     */
    public TestBuilder startYojaWeb(final ScriptOption yojaWebOption) {
    	return start(yojaWebOption, true);
    }

    /**
     * Common bootstrap path used by {@link #startJavascript()} and
     * {@link #startYojaWeb()}: extracts the chosen template HTML from
     * resources into a temp folder, registers it as a {@link WebResource},
     * and inserts a navigation step at the front of {@link #tests}.
     *
     * @param yojaWebOption post-load options
     * @param yojaWeb       {@code true} to load the full Yoja-Web template,
     *                      {@code false} for the minimal one
     * @return this builder
     * @throws SeleniumException when the bootstrap has already run or the
     *                           template extraction fails
     */
    private TestBuilder start(final ScriptOption yojaWebOption,
    		                  final boolean yojaWeb) {
        if (!this.doStartYojaJs) {
            try {
                this.doStartYojaJs = true;
                final Path temporaryFolder = Files.createTempDirectory("yojaWeb_");
                final String homeFileContent = yojaWeb
                		                         ? ResourceUtil.read("com/easygoingapi/yoja/selenium/yojaWeb.html")
                		                         : ResourceUtil.read("com/easygoingapi/yoja/selenium/hello.html");
                Files.writeString(temporaryFolder.resolve("yojaSelenium.html"), homeFileContent);
                final String contextPath = "/" + TestBuilder.class.getName().replace(".", "/");
                final WebApp webApp = WebApp.builder(Type.folder,
                                                     HttpRouter.formatPath(temporaryFolder))
                                            .contextPath(contextPath)
                                            .build();
                this.webServices.add(new WebResource(webApp, "/yojaSelenium.html"));
                final Consumer<TestContext> action = testContext -> {
                    final HttpUrl httpUrl = testContext.httpUrlBuilder()
                                                       .path(contextPath + "/yojaSelenium.html")
                                                       .build();
                    testContext.getHttpPage(Duration.ofSeconds(10), httpUrl);
                    if (yojaWebOption.wyLogger) {
                        testContext.seleniumService().saveLogs();
                    }
                    if (yojaWebOption.ywAssert) {
                        testContext.seleniumService().loadYwAssert();
                    }
                };
                this.tests.add(0, new Test("startYojaJs", action));
            }
            catch (final Exception e) {
                throw new SeleniumException("start yojaJs failed", e);
            }
        }
        else {
            throw new SeleniumException("yojaJs already started");
        }
        return this;
    }

    /**
     * Adds a navigation step pointing at {@code path} on the embedded server,
     * with a 2-second page-load timeout.
     *
     * @param path URL path to navigate to
     * @return this builder
     */
    public TestBuilder getPage(final String path) {
    	 this.tests.add(new Test("page " + path,
    			                 c -> c.getHttpPage(Duration.ofSeconds(2),
                                                    c.httpUrlBuilder()
                                                     .path(path)
                                                     .build())));
    	return this;
    }

    /*
     *
     * WEBSERVICE
     *
     */
    /**
     * Registers an extension → Content-Type mapping on the embedded server.
     *
     * @param extension   file extension (without dot)
     * @param contentType MIME type
     * @return this builder
     */
    public TestBuilder contentType(final String extension,
                                   final String contentType) {
        this.contentTypes.put(extension, contentType);
        return this;
    }

    /**
     * Mounts a jar-backed web app under {@code /*} with no context path.
     *
     * @param packageName classpath base of the resources
     * @return this builder
     */
    public TestBuilder webResource(final String packageName) {
        final WebApp webApp = WebApp.builder(Type.jar, packageName)
                                    .contextPath("/")
                                    .build();
        this.webServices.add(new WebResource(webApp, "/*"));
        return this;
    }

    /**
     * Mounts a jar-backed web app under {@code /*} with the given context
     * path.
     *
     * @param packageName classpath base of the resources
     * @param contextPath URL prefix to mount under
     * @return this builder
     */
    public TestBuilder webResource(final String packageName,
                                   final String contextPath) {
        final WebApp webApp = WebApp.builder(Type.jar, packageName)
                                    .contextPath(contextPath)
                                    .build();
        this.webServices.add(new WebResource(webApp, "/*"));
        return this;
    }

    /**
     * Mounts a jar-backed web app under the given URL with the given context
     * path.
     *
     * @param packageName classpath base of the resources
     * @param contextPath URL prefix to mount under
     * @param url         URL pattern of the resource entry
     * @return this builder
     */
    public TestBuilder webResource(final String packageName,
                                   final String contextPath,
                                   final String url) {
        final WebApp webApp = WebApp.builder(Type.jar, packageName)
                                    .contextPath(contextPath)
                                    .build();
        this.webServices.add(new WebResource(webApp,  url));
        return this;
    }

    /**
     * Mounts an existing {@link WebApp} at the given URL.
     *
     * @param webApp the web app to mount
     * @param url    URL pattern of the resource entry
     * @return this builder
     */
    public TestBuilder webResource(final WebApp webApp,
                                   final String url) {
        this.webServices.add(new WebResource(webApp, url));
        return this;
    }

    /**
     * Adds a pre-built {@link WebResource}.
     *
     * @param webResource resource to mount
     * @return this builder
     */
    public TestBuilder webResource(final WebResource webResource) {
        this.webServices.add(webResource);
        return this;
    }

    /**
     * Adds a pre-built {@link WebService}.
     *
     * @param webService service to register
     * @return this builder
     */
    public TestBuilder webService(final WebService webService) {
        this.webServices.add(webService);
        return this;
    }

    /*
     *
     * TOOL
     *
     */
    /**
     * Adds a step that re-installs the {@code ywLogger.js} helper in the page.
     *
     * @return this builder
     */
    public TestBuilder saveLogs() {
        this.tests.add(new Test("save logs", c -> c.seleniumService().saveLogs()));
        return this;
    }

    /**
     * Adds a step that drains the browser logs to SLF4J via
     * {@link SeleniumService#printLogs()}.
     *
     * @return this builder
     */
    public TestBuilder printLogs() {
        this.tests.add(new Test("print logs", c -> c.seleniumService().printLogs()));
        return this;
    }

    /**
     * Adds a step that triggers the IDE breakpoint hook through
     * {@link SeleniumService#debugger()}.
     *
     * @return this builder
     */
    public TestBuilder debugger() {
        this.tests.add(new Test("debugger", c -> c.seleniumService().debugger()));
        return this;
    }

    /**
     * Adds a step that loads the {@code ywAssert.js} helper into the page.
     *
     * @return this builder
     */
    public TestBuilder loadYwAssert() {
        this.tests.add(new Test("load assert", c -> c.seleniumService().loadYwAssert()));
        return this;
    }

    /**
     * Adds a step that reloads the current page with no extra options.
     *
     * @return this builder
     */
    public TestBuilder reload() {
        return reload(ScriptOption.apply());
    }

    /**
     * Adds a step that reloads the current page with the supplied options.
     *
     * @param scriptOption options applied after reload (logger / assert helpers)
     * @return this builder
     */
    public TestBuilder reload(final ScriptOption scriptOption) {
        this.tests.add(new Test("reload", c ->  c.seleniumService().reload(scriptOption)));
        return this;
    }

    /**
     * Adds a step that resizes the browser window.
     *
     * @param width  target width in pixels
     * @param heigth target height in pixels
     * @return this builder
     */
    public TestBuilder resizeWindow(final int width, final int heigth) {
        this.tests.add(new Test("resize window [" + width + "x" + heigth + "]",
                                c -> c.seleniumService().resizeWindow(width, heigth)));
        return this;
    }

    /**
     * Adds a step that sleeps for the given duration on the test thread.
     *
     * @param timeout how long to sleep
     * @return this builder
     */
    public TestBuilder await(final Duration timeout) {
        this.tests.add(new Test("await " + TimeUtil.prettyPrint(timeout), c -> c.seleniumService().await(timeout)));
        return this;
    }

    /*
     *
     * TEST
     *
     */
    /**
     * Adds an arbitrary user-supplied test step.
     *
     * @param testName display name of the step
     * @param test     action receiving the per-browser {@link TestContext}
     * @return this builder
     */
    public TestBuilder test(final String testName,
                            final Consumer<TestContext> test) {
        this.tests.add(new Test(testName, test));
        return this;
    }

    /** Async JS template that imports a module and invokes one or more named functions on it. */
    private static final String jsUnitTemplate = """
        const callback = arguments[arguments.length - 1]
        window.yojaWebJsUnitLogs = []
        import(encodeURI('{{filePath}}'))
          .then(module => {
              try {
                  {{functions}}
                  callback('DONE')
              }
              catch(error) {
                 callback('ERROR: ' + error)
              }
          })
          .catch(error => {
              callback('ERROR: ' + error)
          })
    """;

    /** Snippet inserted into {@link #jsUnitTemplate} for each function call. */
    private static final String jsUnitFonctionCallTemplate = """
          try {
              module.{{functionName}}(arguments)
              window.yojaWebJsUnitLogs.push({date: Date.now(), message: '[jsUnitTest] {{functionName}} DONE'})
          }
          catch(error) {
             callback('ERROR: [{{functionName}}] [{{filePath}}] ' + error)
          }
    """;

    /**
     * Adds a JS-unit step: imports the given module and runs each named
     * function against the supplied arguments. The accumulated logs in
     * {@code window.yojaWebJsUnitLogs} are dumped to SLF4J after the step, and
     * any error message returned by the script triggers a JUnit
     * {@code fail(...)}.
     *
     * @param filePath      URL of the JS module to import
     * @param functionNames functions to invoke on the imported module
     * @param arguments     arguments passed to each function
     * @return this builder
     */
    public TestBuilder testJsUnit(final String filePath,
                                  final List<String> functionNames,
                                  final Object... arguments) {
        final List<String> fonctionCalls = new ArrayList<>();
        final String file =  HttpRouter.formatPath(filePath);
        for (final String function : functionNames) {
            fonctionCalls.add(jsUnitFonctionCallTemplate.replace("{{functionName}}", function)
                                                        .replace("{{filePath}}", file));
        }
        test("jsUnit: " + HttpRouter.formatPath(filePath), testContext -> {
            final String script = jsUnitTemplate.replace("{{filePath}}", file)
                                                .replace("{{functions}}",
                                                         String.join(System.lineSeparator(),
                                                                     fonctionCalls));
            final String result = testContext.seleniumService()
                                             .executeAsyncScript(script, arguments);
            final List<Map<String, Object>> logs = testContext.seleniumService()
                                                              .executeScript("return window ? window.yojaWebJsUnitLogs : []");
            if (logs != null) {
                LOGGER.info("-------------------------------------------------------------------");
                LOGGER.info("---> [jsUnit] {}", file);
                for (final Map<String, Object> log : logs) {
                    LOGGER.info("[{}] {}",
                                DateUtil.formatTimestamp((Long) log.get("date")),
                                log.get("message"));
                }
                LOGGER.info("     [jsUnit] {} <---", file);
                LOGGER.info("-------------------------------------------------------------------");
            }
            if (result.startsWith("ERROR:")) {
                fail(result.substring(7));
            }
        });
        return this;
    }

    /**
     * Adds an async module test (the module's {@code default} export is called
     * with {@code (args, resolve, reject)}) with an explicit timeout.
     *
     * @param duration  script timeout
     * @param filePath  URL of the JS module
     * @param arguments arguments passed to the module
     * @return this builder
     */
    public TestBuilder testAsyncModule(final Duration duration,
                                       final String filePath,
                                       final Object... arguments) {
        return testModule(true, duration, filePath, arguments);
    }

    /**
     * Async module test with the default timeout.
     *
     * @param filePath  URL of the JS module
     * @param arguments arguments passed to the module
     * @return this builder
     */
    public TestBuilder testAsyncModule(final String filePath,
                                       final Object... arguments) {
        return testModule(true, null, filePath, arguments);
    }

    /**
     * Imports a module for its side effects, without calling any function on it.
     *
     * @param filePath URL of the JS module
     * @return this builder
     */
    public TestBuilder loadModule(final String filePath) {
        return testModule(false, null, filePath);
    }

    /**
     * Synchronous module test: imports the module and calls its
     * {@code default} export with {@code (arguments)} (without resolve/reject
     * callbacks).
     *
     * @param filePath  URL of the JS module
     * @param arguments arguments passed to the module
     * @return this builder
     */
    public TestBuilder testModule(final String filePath,
                                  final Object... arguments) {
        return testModule(false, null, filePath, arguments);
    }

    /**
     * Async JS template that imports a module and calls
     * {@code module.default(args, resolve, repeat)}: the {@code repeat}
     * callback signals the {@code repeatAsyncScript} loop should re-run, while
     * {@code resolve} signals success.
     */
    private static final String repeatModuleTemplate = """
        const callback = arguments[arguments.length - 1]
        import(encodeURI('{{filePath}}'))
          .then(module => {
              const repeat = function() {
                  callback(false)
              }
              const resolve = function() {
                  callback(true)
              }
              const args = []
              for (let i = 0; i < (arguments.length - 1); i++) {
                  args.push(arguments[i])
              }
              module.default(args, resolve, repeat)
              repeat()
          })
          .catch(error => callback('ERROR: [{{filePath}}] ' + error))
    """;

    /**
     * Adds a polling module-test step: re-runs the module until it resolves
     * or {@code until} elapses.
     *
     * @param until     polling deadline
     * @param filePath  URL of the JS module
     * @param arguments arguments passed to the module
     * @return this builder
     */
    public TestBuilder repeatTestModuleUntil(final Duration until,
                                             final String filePath,
                                             final Object... arguments) {
        final String formatedPath = HttpRouter.formatPath(filePath);
        test("repeatJsModuleUntil: " + "->" + formatedPath, testContext -> {
            try {
                String script = repeatModuleTemplate;
                script = script.replace("{{filePath}}", formatedPath);
                final Object result = testContext.seleniumService()
                                                  .repeatAsyncScript(until, script, arguments);
                if (result instanceof String v
                        && v.startsWith("ERROR:")) {
                    fail(v.substring(7));
                }
            }
            catch (final Exception e) {
                throw new SeleniumException("execute jsModule failed: " + formatedPath
                                          + "; cause: " + e.getMessage(), e);
            }
        });
        return this;
    }

    /** Synchronous module template: imports the module and calls its default export. */
    private static final String moduleTemplate = """
        const callback = arguments[arguments.length - 1]
        import(encodeURI('{{filePath}}'))
          .then(module => {
              if (module.default) {
                 module.default(arguments)
              }
              callback('DONE')
          })
          .catch(error => callback('ERROR: [{{filePath}}] ' + error))
    """;

    /** Async module template: imports the module and calls its default export with resolve/reject callbacks. */
    private static final String asyncModuleTemplate = """
        let errorModule = null
        const callback = arguments[arguments.length - 1]

        const resolve = function resolve() {
            callback('DONE')
        }
        const rejet = function rejet(error) {
            let message
            if (error) {
               message = error
            }
            else {
               message = 'module rejected'
            }
            callback('ERROR: [{{filePath}}] ' + message)
        }
        const args = []
        for (let i = 0; i < (arguments.length - 1); i++) {
            args.push(arguments[i])
        }
        import(encodeURI('{{filePath}}'))
          .then(module => {
              module.default(args, resolve, rejet)
          })
          .catch(error => callback('ERROR: [{{filePath}}] ' + error))
    """;

    /**
     * Common path for {@link #testModule}, {@link #testAsyncModule} and
     * {@link #loadModule}: picks the synchronous or asynchronous JS template,
     * runs it once, and fails the JUnit assertion when the script reports an
     * error.
     *
     * @param async     {@code true} for the async template (module gets
     *                  {@code resolve}/{@code reject} callbacks)
     * @param duration  script timeout (may be {@code null} for the default)
     * @param filePath  URL of the JS module
     * @param arguments arguments passed to the module
     * @return this builder
     */
    private TestBuilder testModule(final boolean async,
                                   final Duration duration,
                                   final String filePath,
                                   final Object... arguments) {
        final String formatedPath = HttpRouter.formatPath(filePath);
        test("jsModule: " + "->" + formatedPath, testContext -> {
            try {
                String script = async ? asyncModuleTemplate : moduleTemplate;
                script = script.replace("{{filePath}}", formatedPath);
                final String result = testContext.seleniumService()
                                                 .executeAsyncScript(duration, script, arguments);
                if (result.startsWith("ERROR:")) {
                    fail(result.substring(7));
                }
            }
            catch (final Exception e) {
                throw new SeleniumException("execute jsModule failed: " + formatedPath
                                          + "; cause: " + e.getMessage(), e);
            }
        });
        return this;
    }

    /*
     *
     * WEBSOCKET
     *
     */
    /**
     * Registers a WebSocket endpoint on the embedded server.
     *
     * @param webSocket endpoint to register (no-op when {@code null})
     * @return this builder
     */
    public TestBuilder webSocket(final WebSocket webSocket) {
        if (webSocket != null) {
            this.webSocketService.add(webSocket);
        }
        return this;
    }

    /*
     *
     * BUILD
     *
     */
    /** Equivalent to {@link #execute(boolean) execute(false)}. */
    public void execute() {
        execute(false);
    }

    /**
     * Runs every registered step against every registered browser config.
     * <p>
     * For each browser config a fresh {@link SeleniumService} and
     * {@link TestContext} are created, the embedded {@link HttpServerContext}
     * is started, the steps execute in declaration order, and (unless
     * {@code keepRunning} is true) the context is closed in the surrounding
     * {@code finally}. Step failures (assertion errors or exceptions) are
     * logged but do not interrupt the run.
     *
     * @param keepRunning when {@code true}, leaves the per-browser context
     *                    open after the steps finish (useful for debugging)
     */
    public void execute(final boolean keepRunning) {
        final HttpServerContext httpServerContext = new HttpServerContext(host,
                                                                          jsUnitWebApps,
                                                                          webServices,
                                                                          webSocketService,
                                                                          contentTypes);
        for (final Browser.Config browserConfig : browserConfigs) {
            final SeleniumService seleniumService = SeleniumService.newInstance(browserConfig);
            final TestContext testContext = new TestContext(browserConfig,
                                                            httpServerContext,
                                                            seleniumService);
            try {
                httpServerContext.start();
                for (final Test test : tests) {
                    try {
                        test.action().accept(testContext);
                    }
                    catch (final AssertionError | Exception e) {
                        LOGGER.error("{} failed", test.name(), e);
                    }
                }
            }
            finally {
                if (!keepRunning) {
                    testContext.close();
                }
            }
        }
    }

    /**
     * Materialises the scenario as a list of JUnit 5 {@link DynamicNode}s.
     * <p>
     * Behaviour depends on the registered browsers and steps:
     * <ul>
     *   <li>no browser registered → falls back to FIREFOX (and, in a
     *       previously-empty list, would also add a single
     *       "start debugger server" node);</li>
     *   <li>single step → wraps it so the embedded server starts before the
     *       step and the context is closed in a {@code finally};</li>
     *   <li>multiple steps → the first step starts the embedded server, the
     *       last step closes the context, the middle ones run untouched.</li>
     * </ul>
     *
     * @return the list of dynamic nodes
     */
    private List<DynamicNode> build() {
        if (browserConfigs.isEmpty()) {
            browserConfigs.add(Browser.builder(Browser.FIREFOX).build());
        }
        final HttpServerContext httpServerContext = new HttpServerContext(host,
                                                                          jsUnitWebApps,
                                                                          webServices,
                                                                          webSocketService,
                                                                          contentTypes);
        final List<DynamicNode> dynamicNodes = new ArrayList<>();
        if (browserConfigs.isEmpty()) {
            dynamicNodes.add(DynamicTest.dynamicTest("start debugger server",
                             () -> {
                                try {
                                    httpServerContext.start();
                                    Debugger.debugger();
                                }
                                finally {
                                    httpServerContext.close();
                                }
                             }));
        }
        else {
            for (final Browser.Config browserConfig : browserConfigs) {
                final SeleniumService seleniumService = SeleniumService.newInstance(browserConfig);
                final TestContext testContext = new TestContext(browserConfig,
                                                                httpServerContext,
                                                                seleniumService);
                if (tests.size() == 1) {
                    final Test test = tests.get(0);
                    final Consumer<TestContext> action = c -> {
                        httpServerContext.start();
                        try {
                            test.action().accept(c);
                        }
                        finally {
                            testContext.close();
                        }
                    };
                    dynamicNodes.add(TestBuilder.test(testContext,
                                                      test.name(),
                                                      action));
                }
                else {
                    int index = 0;
                    for (final Test test : tests) {
                        final Consumer<TestContext> action;
                        if (index == 0) {
                            action = c -> {
                                httpServerContext.start();
                                test.action().accept(c);
                            };
                        }
                        else if (index == tests.size() - 1) {
                            action = c -> {
                                try {
                                    test.action().accept(c);
                                }
                                finally {
                                    testContext.close();
                                }
                            };
                        }
                        else {
                            action = test.action();
                        }
                        index++;
                        dynamicNodes.add(TestBuilder.test(testContext,
                                                          test.name(),
                                                          action));
                    }
                }
            }
        }
        return dynamicNodes;
    }

    /**
     * Returns the scenario as a stream of JUnit dynamic nodes.
     *
     * @return the scenario as a stream of JUnit dynamic nodes — meant to be
     *         returned from a {@code @TestFactory}-annotated method
     */
    public Stream<DynamicNode> stream() {
        return build().stream();
    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Returns a new builder.
     *
     * @return a new builder
     */
    public static TestBuilder builder() {
        return new TestBuilder();
    }

    /**
     * Wraps a single step as a JUnit {@link DynamicTest} whose display name
     * is {@code "<browser> -> <step name>"}.
     *
     * @param testContext context the step will receive
     * @param testName    name of the step
     * @param test        action to run
     * @return the dynamic-test node
     */
    private static DynamicTest test(final TestContext testContext,
                                    final String testName,
                                    final Consumer<TestContext> test) {
        return DynamicTest.dynamicTest(testContext.browser().name()
                                        + " -> "
                                        + testName,
                                       () -> test.accept(testContext));
    }

}
