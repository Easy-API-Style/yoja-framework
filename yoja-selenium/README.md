# yoja-selenium

[![Website](https://img.shields.io/badge/website-easygoingapi.com%2Fyoja--selenium-blue)](https://easygoingapi.com/modules/selenium.html) [![Email](https://img.shields.io/badge/email-easy.api.contact%40gmail.com-red)](mailto:easy.api.contact@gmail.com)

[![Release](https://img.shields.io/badge/release-1.0.0-brightgreen)](https://github.com/Easy-API-Style/yoja-framework/releases/tag/1.0.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-green)](https://github.com/Easy-API-Style/yoja-framework/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange)](https://openjdk.org/projects/jdk/25/)
[![Maven Central](https://img.shields.io/badge/maven--central-com.easygoingapi%3Ayoja--selenium%3A1.0.0-blue)](https://central.sonatype.com/artifact/com.easygoingapi/yoja-selenium/1.0.0)

Selenium testing module of the Yoja Framework. Provides a fluent API to write browser-driven tests with an **embedded HTTP server**, JavaScript module execution, browser log capture, and JUnit 6 integration.

## Installation

```groovy
dependencies {
    implementation 'com.easygoingapi:yoja-selenium:VERSION'
}
```

---

## Table of Contents

- [Quick Start](#quick-start)
- [TestBuilder — Fluent Test Runner](#testbuilder-fluent-test-runner)
  - [Browser configuration](#browser-configuration)
  - [Serving pages and resources](#serving-pages-and-resources)
  - [Navigation steps](#navigation-steps)
  - [Test steps](#test-steps)
  - [JS module tests](#js-module-tests)
  - [JS unit tests](#js-unit-tests)
  - [Utility steps](#utility-steps)
  - [WebSocket support](#websocket-support)
  - [Execution modes](#execution-modes)
- [TestContext — In-test API](#testcontext-in-test-api)
- [SeleniumService — Browser Control](#seleniumservice-browser-control)
  - [JavaScript execution](#javascript-execution)
  - [DOM querying](#dom-querying)
  - [Page navigation](#page-navigation)
  - [Browser storage](#browser-storage)
  - [Browser logs](#browser-logs)
  - [Utilities](#utilities)
- [Browser and BrowserConfig](#browser-and-browserconfig)
- [ScriptOption](#scriptoption)
- [ywAssert — Browser-side Assertions](#ywassert-browser-side-assertions)
- [Log](#log)
- [HttpServerContext and HttpUrlBuilder](#httpservercontext-and-httpurlbuilder)
- [SeleniumException](#seleniumexception)

---

## Quick Start

### JUnit 6 — `@TestFactory`

```java
@TestFactory
Stream<DynamicNode> myBrowserTest() {
    return TestBuilder.builder()
        .browser(Browser.builder(Browser.CHROME)
                        .mode(Browser.Mode.HEADLESS)
                        .build())
        .startYojaWeb()                           // load built-in HTML page
        .test("check title", ctx -> {
            String title = ctx.seleniumService()
                              .executeScript("return document.title");
            assertEquals("Yoja", title);
        })
        .stream();
}
```

### Standalone execution (no JUnit)

```java
TestBuilder.builder()
           .browser(Browser.builder(Browser.FIREFOX).build())
           .startYojaWeb()
           .test("my test", ctx -> {
               // ...
           })
           .execute();
```

---

## TestBuilder: Fluent Test Runner

`TestBuilder` orchestrates the full test lifecycle:
1. Starts an **embedded HTTP server** on an auto-assigned port
2. Opens a **browser** via Selenium
3. Runs the declared **steps** sequentially
4. Closes the browser and server when done

```java
TestBuilder builder = TestBuilder.builder();
```

### Browser configuration

```java
builder.browser(Browser.builder(Browser.FIREFOX)
                       .mode(Browser.Mode.HEADLESS)
                       .timeout(Duration.ofSeconds(15))
                       .build())
       .browser(Browser.builder(Browser.CHROME)
                       .mode(Browser.Mode.HEADLESS)
                       .build());
// Each registered browser runs the full test suite independently
```

Custom test server host (default: `"localhost"`):

```java
builder.host("127.0.0.1");
```

### Serving pages and resources

**Start with the built-in yojaWeb page** (requires the yoja-web JS library):

```java
builder.startYojaWeb();
// With options (enable log capture + assertion helpers)
builder.startYojaWeb(ScriptOption.apply()
                                 .saveLogs()
                                 .loadYwAssert());
```

The built-in page loads `YojaWeb-1.0.0.js` with `yw-config-path="/YojaWeb.conf.js"`. To configure yoja-web in tests, serve a `/YojaWeb.conf.js` file from the embedded server:

```java
builder.webService(new WebService(HttpMethod.GET, "/YojaWeb.conf.js", routing ->
    routing.response().sendFile("/path/to/YojaWeb.conf.js")));
```

**Start with a minimal HTML page** (no yoja-web dependency):

```java
builder.startJavascript();

builder.startJavascript(ScriptOption.apply().saveLogs());
```

**Serve static resources from a JAR (classpath):**

```java
// All resources under the package "com.example.frontend" → served at /*
builder.webResource("com.example.frontend");
// With a context path
builder.webResource("com.example.frontend", "/app");
// With a context path and a specific URL pattern
builder.webResource("com.example.frontend", "/app", "/app/*.js");
```

**Serve static resources from a `WebApp` object:**

```java
WebApp webApp = WebApp.builder(WebApp.Type.folder, "/var/www/test")
                      .contextPath("/assets")
                      .build();
builder.webResource(webApp, "/assets/*");
```

**Register REST endpoints:**

```java
builder.webService(new WebService(HttpMethod.GET, "/api/data", routing -> {
    routing.response().send(new JsonObject().put("key", "value"));
}));
```

**Custom content-type mappings:**

```java
builder.contentType("js",  "application/javascript")
       .contentType("css", "text/css");
```

### Navigation steps

Navigate to a path on the embedded server:

```java
builder.getPage("/index.html");
builder.getPage("/app/dashboard");
```

### Test steps

```java
builder.test("verify heading", ctx -> {
    WebElement h1 = ctx.seleniumService().firstTag("h1");
    assertNotNull(h1);
    assertEquals("Welcome", h1.getText());
});

builder.test("check API response", ctx -> {
    String json = ctx.seleniumService().executeScript("""
        const res = await fetch('/api/data')
        return await res.text()
    """);
    assertTrue(json.contains("key"));
});
```

### JS module tests

Execute an ES6 module's `default` export as a test step.

**Synchronous module** — `module.default(arguments)` is called:

```java
// Module file: /path/to/myTest.js
// export default function(arguments) { /* ... */ }
builder.testModule("/path/to/myTest.js");
// With arguments passed to the module
builder.testModule("/path/to/myTest.js", "arg1", 42);
```

**Async module** — `module.default(args, resolve, reject)` must call `resolve()` or `reject(error)`:

```java
// export default async function(args, resolve, reject) {
//     const result = await fetch('/api/test')
//     if (result.ok) resolve()
//     else reject('API failed')
// }
builder.testAsyncModule("/path/to/asyncTest.js");
// With custom timeout
builder.testAsyncModule(Duration.ofSeconds(30), "/path/to/asyncTest.js");
// With arguments
builder.testAsyncModule("/path/to/asyncTest.js", "param1", "param2");
```

**Load module without assertions** (just execute side effects):

```java
builder.loadModule("/path/to/setup.js");
```

**Repeat async module until it resolves** (polling with timeout):

```java
// export default function(args, resolve, repeat) {
//     if (conditionMet()) resolve()
//     else repeat()   // retry
// }
builder.repeatTestModuleUntil(Duration.ofSeconds(20), "/path/to/waitFor.js");
```

> 📂 **Runnable example** — [`TestModuleDemoTest.java`](../yoja-blueprint-kanban/src/test/java/com/easygoingapi/yoja/example/TestModuleDemoTest.java) in `yoja-blueprint-kanban` chains a `testModule` and a `testAsyncModule` against the live demo app. Companion JS modules: [`moduleSync.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/moduleSync.js) (sync default export) and [`moduleAsync.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/moduleAsync.js) (async default export with `await fetch`).

### JS unit tests

Load an ES6 module and call **named exported functions** as individual test steps. Each function receives the arguments array. A thrown exception or returned error string causes the test to fail.

**The JS module** — one named export per test, using `ywAssert` for assertions:

```js
// /test/formValidation.js
export function testRequiredFields(arguments) {
    const email = document.querySelector('input[name="email"]')
    ywAssert.assertNotNull(email, 'email field missing')
    ywAssert.assertEquals('', email.value, 'email should be empty on load')
}

export function testEmailFormat(arguments) {
    const [validEmail] = arguments
    const input = document.querySelector('input[name="email"]')
    input.value = validEmail
    ywAssert.assertEquals(validEmail, input.value, 'email value not set')
}

export function testSubmitButton(arguments) {
    const btn = document.querySelector('button[type="submit"]')
    ywAssert.assertNotNull(btn, 'submit button missing')
    ywAssert.assertFalse(btn.disabled, 'submit should be enabled')
}
```

**Wired from Java:**

```java
builder.testJsUnit(
    "/test/formValidation.js",
    List.of("testRequiredFields", "testEmailFormat", "testSubmitButton")
);

// With arguments passed to each function
builder.testJsUnit(
    "/test/formValidation.js",
    List.of("testRequiredFields", "testEmailFormat", "testSubmitButton"),
    "alice@example.com"
);
```

Each named function is executed as a separate test step — it appears as its own entry in the JUnit report.

> 📂 **Runnable example** — [`JsUnitDemoTest.java`](../yoja-blueprint-kanban/src/test/java/com/easygoingapi/yoja/example/JsUnitDemoTest.java) in `yoja-blueprint-kanban` shows `testJsUnit` (sync, multiple named exports) followed by `testAsyncModule` against the live demo app. Companion JS modules: [`jsUnitSyncTest.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/jsUnitSyncTest.js) and [`jsUnitAsyncTest.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/jsUnitAsyncTest.js).

### Utility steps

```java
builder
    .saveLogs()              // activate browser log capture
    .printLogs()             // flush captured logs to SLF4J
    .loadYwAssert()          // inject ywAssert.js into the page
    .reload()                // reload page and wait for readiness
    .reload(ScriptOption.apply().saveLogs())
    .resizeWindow(1280, 720) // resize browser window
    .await(Duration.ofSeconds(2))  // pause between steps
    .debugger();             // anchor line for an IDE Java breakpoint (see "debugger()" below)
```

> **`debugger()` is not a JS `debugger;` statement.** It throws and immediately
> catches a `Debugger` exception inside `Debugger.debugger()` so the JVM
> actually executes a real instruction at that line. Set an **IDE breakpoint**
> on `throw new Debugger()` in `Debugger.java` (or configure your IDE to break
> on caught `Debugger` exceptions) — the test thread will pause there and you
> can inspect state, evaluate expressions, then resume.
> Combined with `Browser.Mode.DEBUGGER`, all Selenium timeouts are stretched to
> 1 hour so the browser side does not abort while you are paused. See
> [Browser.Mode.DEBUGGER](#browser-and-browserconfig) below.

### WebSocket support

Register WebSocket endpoints accessible from the browser during tests:

```java
WebSocket ws = new WebSocket("/ws/events");
ws.onTextMessage(e -> ws.send("ack: " + e.message()));

builder.webSocket(ws);
```

### Execution modes

**JUnit 6 `@TestFactory`** — returns one `DynamicTest` per step per browser:

```java
@TestFactory
Stream<DynamicNode> myTests() {
    return TestBuilder.builder()
        .browser(Browser.builder(Browser.CHROME).mode(Browser.Mode.HEADLESS).build())
        .startYojaWeb()
        .test("step 1", ctx -> { /* ... */ })
        .test("step 2", ctx -> { /* ... */ })
        .stream();
}
```

**Standalone** — runs all steps in sequence, errors are logged but execution continues:

```java
TestBuilder.builder()
    .browser(...)
    .test("step 1", ctx -> { /* ... */ })
    .execute();

// Keep the server running after tests (for manual inspection)
builder.execute(true);
```

---

## TestContext: In-test API

`TestContext` is passed to every `.test(...)` lambda. It gives access to the browser, the server, and navigation helpers.

```java
builder.test("my test", ctx -> {

    // Navigate
    ctx.getHttpPage("/dashboard");
    ctx.getHttpPage(Duration.ofSeconds(10), "/dashboard");

    // Build a URL for the embedded server
    HttpUrl url = ctx.httpUrlBuilder()
                     .path("/api/items")
                     .query("page=1&size=10")
                     .fragment("section")
                     .build();
    ctx.getHttpPage(url);

    // Which browser is running
    Browser browser = ctx.browser(); // CHROME, FIREFOX, EDGE

    // Direct access to SeleniumService
    SeleniumService selenium = ctx.seleniumService();

    // Shortcut to browser logs
    List<Log> logs = ctx.logs();

    // Server context
    HttpServerContext server = ctx.httpServerContext();
    System.out.println("port: " + server.port());
    System.out.println("host: " + server.host());
});
```

---

## SeleniumService: Browser Control

Obtained from `TestContext.seleniumService()` or created directly:

```java
Browser.Config config = Browser.builder(Browser.CHROME)
                                .mode(Browser.Mode.HEADLESS)
                                .timeout(Duration.ofSeconds(10))
                                .build();

SeleniumService selenium = SeleniumService.newInstance(config);
// implements AutoCloseable
try (selenium) {
    selenium.getHttpPage("http://localhost:8080");
    // ...
}
```

### JavaScript execution

All methods accept an optional `Duration` as first argument to override the default timeout for that call.

**Synchronous script** — returns the value of the last `return` statement:

```java
// Inline script
String title = selenium.executeScript("return document.title");
Long count = selenium.executeScript("return document.querySelectorAll('li').length");

// With arguments (accessed as `arguments[0]`, `arguments[1]`, ...)
String text = selenium.executeScript("""
    return document.querySelector(arguments[0]).textContent
""", ".my-class");

// With custom timeout
String val = selenium.executeScript(Duration.ofSeconds(5), "return someSlowFunction()");

// From a .js file on disk
Object result = selenium.executeScript(Path.of("/path/to/script.js"), "arg1");
```

**Async script** — the last argument is the callback; call it to signal completion:

```java
String response = selenium.executeAsyncScript("""
    const callback = arguments[arguments.length - 1]
    fetch('/api/data')
      .then(r => r.text())
      .then(t => callback(t))
      .catch(e => callback('ERROR: ' + e))
""");

// With timeout
String result = selenium.executeAsyncScript(Duration.ofSeconds(20), myScript);

// From a file
Object r = selenium.executeAsyncScript(Path.of("/path/to/async.js"), arg1, arg2);
```

**Repeat script until non-null** (polling with `WebDriverWait`):

```java
// Retries until the script returns a non-null / non-false value
WebElement el = selenium.repeatScript(Duration.ofSeconds(10), """
    return document.querySelector('.loaded')
""");
```

**Repeat async script until resolved:**

```java
Boolean done = selenium.repeatAsyncScript(Duration.ofSeconds(15), """
    const callback = arguments[arguments.length - 1]
    if (window.appReady) callback(true)
    else callback(null)
""");
```

**Load a JavaScript library into the page:**

```java
// Appends a <script> tag and waits for it to execute
selenium.loadJavascript("window.myLib = { greet: () => 'hello' }");
```

**Direct access to the underlying Selenium objects:**

```java
WebDriver         driver = selenium.webDriver();
JavascriptExecutor exec  = selenium.javascriptExecutor();
```

### DOM querying

Uses the `yojaWeb` JavaScript library (requires `startYojaWeb()`).

```java
// Find first element matching a CSS selector
WebElement btn = selenium.firstTag("button.submit");

// With custom timeout
WebElement el = selenium.firstTag(Duration.ofSeconds(5), "#main-title");

// Find within a parent element
WebElement form = selenium.firstTag("form#login");
WebElement input = selenium.firstTagFrom(form, "input[name='email']");
WebElement input = selenium.firstTagFrom(Duration.ofSeconds(3), form, "input[name='email']");

// Find all matching elements
List<WebElement> items = selenium.findTags("ul.menu > li");
List<WebElement> items = selenium.findTags(Duration.ofSeconds(5), ".card");

// Find all within a parent
List<WebElement> cells = selenium.findTagsFrom(row, "td");
List<WebElement> cells = selenium.findTagsFrom(Duration.ofSeconds(3), row, "td");
```

### Page navigation

```java
// Navigate and wait for page to be ready (waits for yojaWeb.isReady())
selenium.getHttpPage("http://localhost:8080/app");
selenium.getHttpPage(Duration.ofSeconds(15), "http://localhost:8080/app");

// With HttpUrl
selenium.getHttpPage(httpUrl);
selenium.getHttpPage(Duration.ofSeconds(10), httpUrl);

// Reload current page
selenium.reload();
selenium.reload(ScriptOption.apply().saveLogs());

// Resize browser window
selenium.resizeWindow(1920, 1080);

// Pause execution
selenium.await(Duration.ofSeconds(2));
```

### Browser storage

Read values from `localStorage` or `sessionStorage`.

**Raw access:**

```java
String raw = selenium.localStorage("my-key");
String raw = selenium.sessionStorage("my-key");
```

**Structured access** (for items stored with the `yojaWebItemKey__` prefix as JSON `{date, type, value}`):

```java
// Storage.date  → the storage timestamp
// Storage.type  → the stored type name
// Storage.value → the stored value

String value = selenium.localStorage("user-token", SeleniumService.Storage.value);
String date  = selenium.localStorage("user-token", SeleniumService.Storage.date);
String type  = selenium.sessionStorage("cart",     SeleniumService.Storage.type);
```

### Browser logs

Capture `console.log / .info / .warn / .error` from the browser into Java.

```java
// Activate log capture on the page (injects ywLogger.js)
selenium.saveLogs();

// Retrieve captured logs
List<Log> logs = selenium.logs();
for (Log log : logs) {
    System.out.printf("[%s] [%s] %s%n", log.date(), log.level(), log.message());
}

// Print to SLF4J (each level → corresponding slf4j method), then clear
selenium.printLogs();

// Clear without printing
selenium.clearLogs();
```

**Log levels:**

| `Log.Level` | Maps to |
|---|---|
| `LOG` | `LOGGER.info` |
| `TRACE` | `LOGGER.trace` |
| `DEBUG` | `LOGGER.debug` |
| `INFO` | `LOGGER.info` |
| `WRAN` | `LOGGER.warn` |
| `ERROR` | `LOGGER.error` |

### Utilities

```java
// Load ywAssert.js assertion helpers into the page
selenium.loadYwAssert();

// Anchor line for an IDE Java breakpoint. The implementation throws and
// catches a Debugger exception inside Debugger.debugger() — set a breakpoint
// on that throw line (or break-on-caught Debugger) and the test thread pauses
// there. Pair with Browser.Mode.DEBUGGER to stretch Selenium timeouts to 1h.
selenium.debugger();

// Close the browser
selenium.close();
```

---

## Browser and BrowserConfig

Configure which browser to use and how to launch it.

```java
Browser.Config config = Browser.builder(Browser.FIREFOX)   // FIREFOX | CHROME | EDGE
    .mode(Browser.Mode.HEADLESS)                            // see table below
    .timeout(Duration.ofSeconds(10))                        // default: 10s
    .build();
```

**Available browsers:**

| `Browser` | Driver |
|---|---|
| `FIREFOX` | `FirefoxDriver` |
| `CHROME` | `ChromeDriver` |
| `EDGE` | `EdgeDriver` |

**Launch modes:**

| `Browser.Mode` | Effect |
|---|---|
| `HEADFUL` | Opens a visible browser window *(default)* |
| `HEADLESS` | Runs without a visible window (CI-friendly) |
| `DEBUGGER` | Visible window with DevTools auto-opened, maximized (Firefox / Edge), and Selenium timeouts stretched to **1 hour** |

> ### `DEBUGGER` mode in detail
>
> Concretely the mode flips three switches at browser-launch time:
>
> | Switch | Firefox | Chrome | Edge |
> |---|---|---|---|
> | DevTools opened on startup | `-devtools` arg | `--auto-open-devtools-for-tabs` arg | `--auto-open-devtools-for-tabs` arg |
> | Window maximized | yes (after 1 s warm-up) | no (currently disabled) | yes (after 2 s warm-up) |
> | Selenium `scriptTimeout` / `pageLoadTimeout` / `implicitlyWait` | `Duration.ofHours(1)` | same | same |
>
> The 1-hour timeouts matter most: without them, pausing at a Java breakpoint
> for more than the default 10 s would cause Selenium to abort the in-flight
> script and break the session. They give you the room to inspect state at
> length without losing the browser context.
>
> `DEBUGGER` mode does **not** by itself pause execution — it only prepares the
> environment. To actually pause, either set a Java breakpoint in your IDE on
> the line of interest, or insert `selenium.debugger()` / `builder.debugger()`
> as a marker (see `debugger()` notes above). Once paused, you can use the
> browser's auto-opened DevTools to inspect the live DOM in parallel.

```java
// Typical CI setup
Browser.Config ci = Browser.builder(Browser.CHROME)
    .mode(Browser.Mode.HEADLESS)
    .build();

// Local interactive debugging
Browser.Config debug = Browser.builder(Browser.FIREFOX)
    .mode(Browser.Mode.DEBUGGER)
    .build();
```

---

## ScriptOption

Controls which helper scripts are injected when loading a page.

```java
ScriptOption options = ScriptOption.apply()
    .saveLogs()      // inject ywLogger.js → captures console output into Java
    .loadYwAssert(); // inject ywAssert.js → assertion helpers usable from JS tests
```

Used with `startJavascript`, `startYojaWeb`, and `reload`:

```java
builder.startYojaWeb(ScriptOption.apply().saveLogs().loadYwAssert());
builder.reload(ScriptOption.apply().saveLogs());
```

---

## ywAssert: Browser-side Assertions

`ywAssert.js` injects a small JUnit-style assertion library into the page, exposed as the global `window.ywAssert`. It is designed to be called from JavaScript test modules executed via [`testModule`](#js-module-tests), [`testAsyncModule`](#js-module-tests), or [`testJsUnit`](#js-unit-tests) — every failed assertion throws a JavaScript `Error`, which Selenium propagates back to the Java side and surfaces as a failing test step.

### Loading

`ywAssert` is **not** injected by default. Three equivalent ways to make it available on the page:

```java
// 1. As a script option when starting the page
builder.startYojaWeb(ScriptOption.apply().loadYwAssert());

// 2. As a dedicated TestBuilder step (before any test that needs it)
builder.loadYwAssert();

// 3. On demand from inside a test step
builder.test("...", ctx -> ctx.seleniumService().loadYwAssert());
```

Once loaded, `window.ywAssert` is reachable from any inline script, async script, or ES6 module run on the page.

### API

All methods hang off the global `window.ywAssert` singleton. The optional `message` argument is prepended to the actual error with `" -> "` to give context — useful when a test calls the same assertion several times.

| Method | Behavior on failure |
|---|---|
| `ywAssert.fail(error)` | Always throws `Error(error)` |
| `ywAssert.assertEquals(expected, actual, message?)` | Throws if `JSON.stringify(expected) !== JSON.stringify(actual)` — deep equality for primitives, plain objects and arrays |
| `ywAssert.assertTrue(value, message?)` | Throws unless `value === true` (strict identity, not truthiness) |
| `ywAssert.assertFalse(value, message?)` | Throws unless `value === false` (strict) |
| `ywAssert.assertNull(value, message?)` | Throws unless `value === null` |
| `ywAssert.assertUndefined(value, message?)` | Throws unless `value === undefined` |
| `ywAssert.assertNotNull(value, message?)` | Throws if `value === null` |
| `ywAssert.assertNotUndefined(value, message?)` | Throws if `value === undefined` |
| `ywAssert.assertArrayEquals(expected, actual, message?)` | Both arguments must be arrays of the same length; each element is compared via `JSON.stringify`. On mismatch, the error includes the failing index plus full dumps of both arrays |

### Behavior notes

- **Strict equality only.** `assertTrue` / `assertFalse` use `===`, not truthiness. `ywAssert.assertTrue(1)` **fails** — only the literal `true` passes. Same for `null` / `undefined` checks.
- **`JSON.stringify` based deep equality.** Object key order matters (`{a:1,b:2}` is not equal to `{b:2,a:1}`). Values that don't survive JSON serialization (`undefined`, functions, `Date`, `Map`, `Set`, `Symbol`) are compared as their JSON form — usually stripped or stringified to `"null"`. Convert them yourself before asserting if precision matters.
- **Failure propagation.** Each failure throws a plain `Error`. From an async test module, catch it and forward via `reject(error.message)` so Selenium can lift it into a Java `SeleniumException` and fail the right step.

### Example — async test module

```js
// /test/checkUserList.js
export default async function(args, resolve, reject) {
    try {
        const res   = await fetch('/api/users')
        const users = await res.json()

        ywAssert.assertEquals(200, res.status, 'wrong HTTP status')
        ywAssert.assertTrue(Array.isArray(users), 'response should be an array')
        ywAssert.assertArrayEquals(
            [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }],
            users,
            'user list payload'
        )
        resolve()
    }
    catch (e) {
        reject(e.message)
    }
}
```

Wired from Java:

```java
TestBuilder.builder()
    .browser(Browser.builder(Browser.CHROME).mode(Browser.Mode.HEADLESS).build())
    .startYojaWeb(ScriptOption.apply().loadYwAssert())
    .testAsyncModule("/test/checkUserList.js")
    .stream();
```

If any assertion fails, the JUnit `DynamicTest` for `testAsyncModule` is reported as failing with a message of the form:
```
user list payload -> index 1 expected '{"id":2,"name":"Bob"}' but it was '{"id":2,"name":"Bib"}'
expected:
[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]
actual:
[{"id":1,"name":"Alice"},{"id":2,"name":"Bib"}]
```

> 📂 **Runnable examples using `ywAssert`** — every JS demo module in `yoja-blueprint-kanban` calls `ywAssert.*` for its assertions: [`jsUnitSyncTest.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/jsUnitSyncTest.js), [`jsUnitAsyncTest.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/jsUnitAsyncTest.js), [`moduleSync.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/moduleSync.js), [`moduleAsync.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/moduleAsync.js).

---

## Log

Represents a single browser console entry captured by `saveLogs`.

```java
List<Log> logs = ctx.seleniumService().logs();

for (Log log : logs) {
    Instant    date    = log.date();    // when the log was emitted
    Log.Level  level   = log.level();  // LOG | TRACE | DEBUG | INFO | WRAN | ERROR
    String     message = log.message();
}
```

---

## HttpServerContext and HttpUrlBuilder

The embedded HTTP server used during tests. Accessible from `TestContext`.

```java
HttpServerContext server = ctx.httpServerContext();

String host = server.host();  // e.g. "localhost"
int    port = server.port();  // auto-assigned, e.g. 8888
```

### HttpUrlBuilder

Builds URLs pointing to the embedded test server:

```java
HttpUrl url = ctx.httpUrlBuilder()
    .path("/api/users")
    .query("role=admin&active=true")      // parsed as HttpParameter
    .query(new HttpParameter().addEntry("page", "1"))
    .fragment("list")
    .build();

// → http://localhost:8888/api/users?role=admin&active=true#list
```

---

## SeleniumException

Unchecked exception thrown by the module.

```java
try {
    selenium.executeScript("invalid {{");
} catch (SeleniumException e) {
    System.err.println(e.getMessage());
}
```

---

## Full Example

> 📂 **See also — runnable demos in `yoja-blueprint-kanban`**
>
> | Demo class | Mechanisms exercised |
> |---|---|
> | [`TaskAppTest.java`](../yoja-blueprint-kanban/src/test/java/com/easygoingapi/yoja/example/TaskAppTest.java) | Full end-to-end Selenium suite using `SeleniumService` directly: login, task CRUD, task detail navigation against the live demo app |
> | [`JsUnitDemoTest.java`](../yoja-blueprint-kanban/src/test/java/com/easygoingapi/yoja/example/JsUnitDemoTest.java) | `testJsUnit` (sync, named exports) + `testAsyncModule`. JS modules: [`jsUnitSyncTest.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/jsUnitSyncTest.js), [`jsUnitAsyncTest.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/jsUnitAsyncTest.js) |
> | [`TestModuleDemoTest.java`](../yoja-blueprint-kanban/src/test/java/com/easygoingapi/yoja/example/TestModuleDemoTest.java) | `testModule` (sync default export) + `testAsyncModule` (async default export). JS modules: [`moduleSync.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/moduleSync.js), [`moduleAsync.js`](../yoja-blueprint-kanban/src/test/resources/com/easygoingapi/yoja/example/webapp/moduleAsync.js) |

```java
@TestFactory
Stream<DynamicNode> loginFlow() {

    WebSocket ws = new WebSocket("/ws/notify");
    ws.onTextMessage(e -> ws.send("ack"));

    return TestBuilder.builder()
        // Browsers
        .browser(Browser.builder(Browser.CHROME).mode(Browser.Mode.HEADLESS).build())
        .browser(Browser.builder(Browser.FIREFOX).mode(Browser.Mode.HEADLESS).build())
        // Serve the frontend from the classpath
        .webResource("com.example.app.frontend", "/")
        // REST mock endpoint
        .webService(new WebService(HttpMethod.GET, "/api/config", routing ->
            routing.response().send(new JsonObject().put("env", "test"))))
        // WebSocket
        .webSocket(ws)
        // Start page with log capture
        .startYojaWeb(ScriptOption.apply().saveLogs())
        // Resize to desktop
        .resizeWindow(1280, 800)
        // Navigate
        .getPage("/login.html")
        // Test: fill form and submit
        .test("fill login form", ctx -> {
            SeleniumService s = ctx.seleniumService();

            WebElement email = s.firstTag("input[name='email']");
            WebElement pwd   = s.firstTag("input[name='password']");
            WebElement btn   = s.firstTag("button[type='submit']");

            assertNotNull(email, "email input not found");
            email.sendKeys("alice@example.com");
            pwd.sendKeys("secret");
            btn.click();
        })
        // Test: run async JS module
        .testAsyncModule(Duration.ofSeconds(10), "/test/checkSession.js", "alice@example.com")
        // Test: run JS unit tests
        .testJsUnit("/test/formValidation.js", List.of("testRequired", "testEmailFormat"))
        // Print browser logs
        .printLogs()
        .stream();
}
```

---

## Dependencies

| Library | Usage |
|---|---|
| [Selenium WebDriver](https://www.selenium.dev/) | Browser automation (Firefox, Chrome, Edge) |
| [JUnit 6](https://junit.org/junit5/) | `DynamicTest`, `@TestFactory` integration |
| [SLF4J](https://www.slf4j.org/) | Logging |
| yoja-core | `YojaApp`, `FutureUtil`, `HttpUrl`, `ResourceUtil` |
| yoja-http-server | Embedded test HTTP server, `WebService`, `WebSocket` |
