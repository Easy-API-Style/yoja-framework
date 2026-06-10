# yoja-core

[![Website](https://img.shields.io/badge/website-easygoingapi.com%2Fyoja--core-blue)](https://easygoingapi.com/modules/core.html)

[![Release](https://img.shields.io/badge/release-1.0.0-brightgreen)](https://github.com/Easy-API-Style/yoja-framework/releases/tag/1.0.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](https://github.com/Easy-API-Style/yoja-framework/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange)](https://openjdk.org/projects/jdk/25/)
[![Maven Central](https://img.shields.io/badge/com.easygoingapi%3Ayoja--core%3A1.0.0-blue)](https://central.sonatype.com/artifact/com.easygoingapi/yoja-core/1.0.0)

Core module of the Yoja Framework. Provides application lifecycle management, worker thread pools, scheduling, and HTTP utilities built on top of [Vert.x](https://vertx.io/).

## Installation

```groovy
dependencies {
    implementation 'com.easygoingapi:yoja-core:VERSION'
}
```

---

## Table of Contents

- [YojaApp — Application Lifecycle](#yojaapp-application-lifecycle)
- [Worker — Background Thread Execution](#worker-background-thread-execution)
  - [singleThread](#worker-singlethread)
  - [parallelThread](#worker-parallelthread)
- [Timer — Task Scheduling](#timer-task-scheduling)
- [FutureUtil — Async Helpers](#futureutil-async-helpers)
- [HTTP Utilities](#http-utilities)
  - [HttpUrl](#httpurl)
  - [HttpParameter](#httpparameter)
  - [HttpHeader](#httpheader)
  - [HttpCookie](#httpcookie)
  - [HttpEncoding](#httpencoding)
- [Other Utilities](#other-utilities)
  - [StringUtil](#stringutil)
  - [TimeUtil](#timeutil)
  - [ResourceUtil](#resourceutil)
  - [PathUtil](#pathutil)
  - [ProcessUtil](#processutil)
  - [JavaReflectUtil](#javareflectutil)
- [YojaAppException](#yojaappexception)

---

## YojaApp: Application Lifecycle

`YojaApp` manages the Vert.x instance used by the entire framework. It must be started before using workers or any async feature.

### Quick start

```java
// Start with default options
YojaApp.start();

// Access the underlying Vert.x instance
Vertx vertx = YojaApp.vertx();

// Stop cleanly (returns Future<Void>)
YojaApp.stop();
```

### Custom configuration via Builder

```java
YojaApp.builder()
    .eventLoopPoolSize(4)
    .workerPoolSize(8)
    .start();
```

### Custom VertxOptions

```java
VertxOptions options = new VertxOptions()
    .setEventLoopPoolSize(4)
    .setWorkerPoolSize(16);

YojaApp.start(options);
```

### Restart

```java
// Async restart — stops Vert.x, then restarts it
YojaApp.restart()
       .onSuccess(v -> System.out.println("restarted"))
       .onFailure(Throwable::printStackTrace);
```

### Lifecycle checks

```java
boolean running        = YojaApp.isRunning();
boolean isEventLoop    = YojaApp.isEventLoopThread();
boolean isWorker       = YojaApp.isWorkerThread();

// Block the current thread until YojaApp is stopped
YojaApp.awaitStop();
```

### Log Vert.x configuration

```java
YojaApp.logOptions(); // logs all VertxOptions to SLF4J
```

### Default timeouts

| Setting | Default |
|---|---|
| Max event loop execute time | 4 seconds |
| Max worker execute time | 5 minutes |
| Worker pool size | `Runtime.availableProcessors()` |

---

## Worker: Background Thread Execution

`Worker` offloads blocking work to background threads, keeping the Vert.x event loop free. Vert.x implements the **Multi-Reactor pattern**: a small number of event loop threads (by default `2 × CPU cores`) dispatches all I/O events without ever blocking. `Worker` provides the escape hatch for the cases where blocking is unavoidable. Two strategies are available:

> **Threading model:** handlers registered via `onSuccess`, `onFailure`, and `onComplete` on a `Future` returned by a `Worker` are executed on the **event loop thread** that was active when the handler was registered. Never perform blocking operations inside these handlers — submit a new task to a `Worker` instead.

| Type | Class | Description |
|---|---|---|
| `singleThread` | `Worker.singleThread` | One thread, tasks are queued sequentially |
| `parallelThread` | `Worker.parallelThread` | Thread pool, tasks run concurrently |

### Worker singleThread

Execute a `Runnable` on a dedicated single thread identified by a string ID. The thread is reused across calls.

```java
// Fire-and-forget
Worker.singleThread.once("my-task", () -> {
    System.out.println("runs once in a single thread");
});

// With a return value (Future)
Future<String> result = Worker.singleThread.once("my-task", () -> {
    return "computed value";
});

// With explicit Promise control
Future<String> result = Worker.singleThread.once("my-task", promise -> {
    try {
        String value = computeSomething();
        promise.complete(value);
    } catch (Exception e) {
        promise.fail(e);
    }
});
```

Retrieve or create a persistent worker and submit multiple tasks:

```java
Worker worker = Worker.singleThread.get("import-worker");

worker.execute(() -> step1());
worker.execute(() -> step2());

// Check status
boolean active = worker.isActive();

// Remove when done
worker.remove().onSuccess(v -> System.out.println("removed"));
```

### Worker parallelThread

Execute tasks on a shared thread pool. Useful for CPU-intensive or I/O-bound work that can run concurrently.

```java
// Execute on the default main worker pool
Worker.parallelThread.execute(() -> {
    System.out.println("runs in parallel");
});

// With a return value
Future<Integer> result = Worker.parallelThread.execute(() -> {
    return expensiveComputation();
});

// Create a named pool with custom size
Worker pool = Worker.parallelThread.create("batch-processor", 4);

pool.execute(() -> processBatch(batch1));
pool.execute(() -> processBatch(batch2));

// Listen to removal
pool.onRemove(event -> System.out.println("pool removed: " + event.id()));

// Remove the pool
pool.remove();
```

### Worker lifecycle

```java
// Check all active workers
Worker.log();

// Remove all workers at once
Worker.removeWorkers();

// Remove multiple workers
Worker.remove(worker1, worker2, worker3);

// Check if current thread is interrupted (worker closed)
boolean closed = Worker.isClosed();
```

---

## Timer: Task Scheduling

`Timer` schedules recurring or one-shot tasks. Tasks run on the Vert.x thread pool or on a specific `Worker`.

```java
Timer timer = new Timer();
```

### One-shot task with delay

```java
timer.schedule(task -> {
        System.out.println("runs once after 5 seconds");
    })
    .delay(Duration.ofSeconds(5))
    .build();
```

### Recurring task with period

```java
timer.schedule(task -> {
        System.out.println("runs every 10 seconds");
    })
    .delay(Duration.ofSeconds(10))
    .period(Duration.ofSeconds(10))
    .build();
```

### Task at a fixed rate (compensates for drift)

```java
timer.schedule(task -> {
        System.out.println("fixed rate");
    })
    .period(Duration.ofMinutes(1))
    .atFixedRate(true)
    .build();
```

### Task starting at a specific date

```java
Date startAt = Date.from(Instant.now().plusSeconds(60));

timer.schedule(task -> {
        System.out.println("started at " + task.firstTime());
    })
    .firstTime(startAt)
    .period(Duration.ofHours(1))
    .build();
```

### Named task (identified by String ID)

```java
// Schedule with a named ID — re-scheduling the same ID replaces the previous task
timer.schedule("sync-job", task -> {
        System.out.println("sync job: " + task.id());
    })
    .delay(Duration.ofSeconds(30))
    .period(Duration.ofMinutes(5))
    .build();

// Check if task exists
boolean exists = timer.has("sync-job");

// Inspect scheduled task
Timer.TaskEntity entity = timer.task("sync-job");
System.out.println(entity.delay() + " / " + entity.period());

// Cancel a named task
timer.cancel("sync-job");
```

### Task pinned to a specific Worker

```java
Worker worker = Worker.singleThread.get("report-worker");

timer.schedule("report", task -> {
        generateReport(); // runs on "report-worker" singleThread
    })
    .worker(Worker.Type.singleThread, "report-worker")
    .delay(Duration.ofHours(1))
    .period(Duration.ofHours(1))
    .build();
```

### Auto-sequenced tasks (Long ID)

```java
Timer.Task<Long> task1 = timer.schedule(t -> doWork())
    .period(Duration.ofSeconds(30))
    .build();

Timer.Task<Long> task2 = timer.schedule(t -> doOtherWork())
    .period(Duration.ofMinutes(2))
    .build();

timer.cancel(task1.id());

// List all sequence IDs
Set<Long> ids = timer.sequenceIds();
```

### Cancel all tasks

```java
timer.cancel(); // cancels everything and resets the timer
```

---

## FutureUtil: Async Helpers

`FutureUtil` bridges async Vert.x `Future`s with synchronous blocking code running inside worker threads.

> **Important:** `await` and `sleep` only work inside worker threads (`YojaApp.isWorkerThread() == true`). Never call them on the event loop.

> **Threading model:** handlers registered via `onSuccess`, `onFailure`, and `onComplete` are executed on the **event loop thread** that was active when the handler was registered. Never perform blocking operations inside these handlers — use `Worker.singleThread` or `Worker.parallelThread` to offload blocking work.

### Await a single Future

```java
Worker.singleThread.once("blocking-task", () -> {
    Future<String> future = someAsyncOperation();
    FutureUtil.await(future);
    // future is complete here
    String value = future.result();
});
```

### Await and get value

```java
Worker.singleThread.once("blocking-task", () -> {
    Future<String> future = someAsyncOperation();
    String value = FutureUtil.awaitValue(future);
    System.out.println("Got: " + value);
});
```

### Await multiple Futures

```java
Worker.singleThread.once("multi-await", () -> {
    Future<Void> f1 = operation1();
    Future<Void> f2 = operation2();
    Future<Void> f3 = operation3();

    FutureUtil.await(f1, f2, f3); // waits for all three

    // or with a list
    List<Future<?>> futures = List.of(f1, f2, f3);
    FutureUtil.await(futures);
});
```

### Sleep (worker-thread safe)

```java
Worker.singleThread.once("retry-task", () -> {
    FutureUtil.sleep(2000); // sleeps 2 seconds, no-op on event loop
    retryOperation();
});
```

---

## HTTP Utilities

### HttpUrl

Builder for constructing HTTP/HTTPS/WebSocket URLs.

```java
// Simple URL
HttpUrl url = HttpUrl.builder("api.example.com")
    .protocol(HttpProtocole.https)
    .path("/v1/users")
    .build();

System.out.println(url.url(Format.encoded));
// → https://api.example.com/v1/users
```

```java
// URL with port and query parameters
HttpParameter params = new HttpParameter()
    .addEntry("page", "1")
    .addEntry("size", "20");

HttpUrl url = HttpUrl.builder("api.example.com")
    .protocol(HttpProtocole.https)
    .port(8443)
    .path("/v1/search")
    .parameter(params)
    .fragment("results")
    .build();

System.out.println(url.url(Format.encoded));
// → https://api.example.com:8443/v1/search?page=1&size=20#results
```

```java
// Parse query string directly
HttpUrl url = HttpUrl.builder("api.example.com")
    .parameterQuery("q=hello+world&lang=fr")
    .build();

System.out.println(url.pathAndQuery(Format.decoded));
// → /?q=hello world&lang=fr
```

```java
// WebSocket URL
HttpUrl ws = HttpUrl.builder("ws.example.com")
    .protocol(HttpProtocole.wss)
    .path("/events")
    .build();
```

**Available protocols:** `http`, `https`, `ws`, `wss`

### HttpParameter

Represents query string parameters. Supports multiple values per name.

```java
HttpParameter params = new HttpParameter();

// Add entries (allows duplicates)
params.addEntry("tag", "java")
      .addEntry("tag", "vertx")
      .addEntry("sort", "date");

// Put entry (replaces existing)
params.putEntry("sort", "name");

// Add multiple values at once
params.addEntries("tag", List.of("spring", "gradle"));

// Read
String sort         = params.firstValue("sort");       // "name"
List<String> tags   = params.values("tag");            // ["java", "vertx", "spring", "gradle"]
Set<String> names   = params.names();                  // ["tag", "sort"]
boolean hasTag      = params.hasName("tag");           // true

// Serialize
String encoded  = params.parameterQuery(Format.encoded);  // tag=java&tag=vertx&...
String decoded  = params.parameterQuery(Format.decoded);

// Remove
params.removeEntries("tag");

// Parse from query string
HttpParameter parsed = HttpParameter.parse("name=Alice&role=admin&role=user");
```

### HttpHeader

HTTP request/response headers (case-sensitive names, one value per name).

```java
HttpHeader headers = new HttpHeader();

headers.put("Authorization", "Bearer eyJ...")
       .put(ContentType.key, ContentType.jsonObject.value())
       .put("X-Request-Id", UUID.randomUUID().toString());

// Read
String auth    = headers.value("Authorization");
boolean hasAuth = headers.has("Authorization");
Set<String> names = headers.names();
Map<String, String> all = headers.values();

// Remove
headers.remove("X-Request-Id");

// Copy constructor (shallow copy)
HttpHeader copy = new HttpHeader(headers);
```

**Built-in `ContentType` constants:**

```java
ContentType.text.value()        // "text/plain"
ContentType.jsonObject.value()  // "application/json"
ContentType.jsonArray.value()   // "application/array-json"
ContentType.key                 // "content-type"
```

### HttpCookie

```java
// Simple cookie
HttpCookie simple = HttpCookie.of("session", "abc123");

// Full cookie via builder
HttpCookie cookie = HttpCookie.builder("token", "eyJ...")
    .domain("example.com")
    .path("/")
    .maxAge(3600)
    .sameSite(CookieSameSite.STRICT)
    .httpOnly(true)
    .secure(true)
    .build();

String name   = cookie.getName();
String value  = cookie.getValue();
long maxAge   = cookie.getMaxAge();
boolean http  = cookie.isHttpOnly();
```

### HttpEncoding

URL-encode and decode strings.

```java
String encoded = HttpEncoding.urlEncode("hello world & more"); // "hello+world+%26+more"
String decoded = HttpEncoding.urlDecode("hello+world+%26+more"); // "hello world & more"

// Format-aware helper
String result = HttpEncoding.url(Format.encoded, "hello world");
String result = HttpEncoding.url(Format.decoded, "hello+world");
```

---

## Other Utilities

### StringUtil

```java
StringUtil.isNullOrBlank(null);    // true
StringUtil.isNullOrBlank("  ");   // true
StringUtil.isNullOrBlank("ok");   // false

StringUtil.blankToNull("  ");     // null
StringUtil.blankToNull("ok");     // "ok"
```

### TimeUtil

```java
// Delay from now until a target Date
Date target = Date.from(Instant.now().plusSeconds(3600));
Duration delay = TimeUtil.delayFromNow(target); // ~PT1H

// Human-readable duration
String pretty = TimeUtil.prettyPrint(Duration.ofSeconds(3725));
// e.g. "1h 2m 5s"
```

### ResourceUtil

Reads a classpath resource as a `String`.

```java
String sql    = ResourceUtil.read("queries/find-user.sql");
String config = ResourceUtil.read("config/defaults.json");
```

### PathUtil

Normalizes file paths to forward slashes.

```java
String path = PathUtil.formatPath(Path.of("C:\\Users\\alice\\file.txt"));
// → "C:/Users/alice/file.txt"

String path = PathUtil.formatPath("/var/log/app.log");
// → "/var/log/app.log"
```

### ProcessUtil

Executes an external OS process.

```java
List<String> command = List.of("git", "describe", "--tags");

Integer exitCode = ProcessUtil.execute(
    command,
    line  -> System.out.println("OUT: " + line),   // stdout consumer
    error -> System.err.println("ERR: " + error),  // stderr consumer
    true  // wait for process to complete
);
```

### JavaReflectUtil

Access private fields via reflection.

```java
// Get a Field (searches superclasses)
Field field = JavaReflectUtil.getField(MyClass.class, "secretField");

// Get field value from an object instance
String value = JavaReflectUtil.getFieldValue(myObject, "secretField");
```

---

## YojaAppException

Unchecked exception thrown by the framework.

```java
// Throw with message
throw new YojaAppException("something went wrong");

// Throw wrapping a cause
throw new YojaAppException("operation failed", e);

// Catch
try {
    YojaApp.start();
} catch (YojaAppException e) {
    System.err.println(e); // includes class name + message
}
```

---

## Dependencies

| Library | Usage |
|---|---|
| [Vert.x Core](https://vertx.io/) | Event loop, workers, Futures |
| [SLF4J](https://www.slf4j.org/) | Logging |
| [Guava](https://github.com/google/guava) | Utilities |
