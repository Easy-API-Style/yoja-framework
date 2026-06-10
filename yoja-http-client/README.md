# yoja-http-client

[![Website](https://img.shields.io/badge/website-easygoingapi.com%2Fyoja--http--client-blue)](https://easygoingapi.com/modules/http-client.html)

[![Release](https://img.shields.io/badge/release-1.0.0-brightgreen)](https://github.com/Easy-API-Style/yoja-framework/releases/tag/1.0.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](https://github.com/Easy-API-Style/yoja-framework/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange)](https://openjdk.org/projects/jdk/25/)
[![Maven Central](https://img.shields.io/badge/com.easygoingapi%3Ayoja--http--client%3A1.0.0-blue)](https://central.sonatype.com/artifact/com.easygoingapi/yoja-http-client/1.0.0)

HTTP client module of the Yoja Framework. Provides a fluent API to send GET/POST requests and manage WebSocket connections — built on top of [Vert.x Web Client](https://vertx.io/docs/vertx-web-client/java/).

## Installation

```groovy
dependencies {
    implementation 'com.easygoingapi:yoja-http-client:VERSION'
}
```

Requires `yoja-core` to be started beforehand (`YojaApp.start()`).

---

## Table of Contents

- [Quick Start](#quick-start)
- [HttpEngine — HTTP Infrastructure](#httpengine-http-infrastructure)
- [HttpClient — Send Requests](#httpclient-send-requests)
- [HttpGet — GET Requests](#httpget-get-requests)
  - [Query parameters](#query-parameters)
  - [Headers and cookies](#headers-and-cookies)
- [HttpPost — POST Requests](#httppost-post-requests)
  - [Body types](#body-types)
- [HttpResponse — Read Responses](#httpresponse-read-responses)
  - [Status](#status)
  - [Headers](#headers)
  - [Cookies](#cookies)
  - [Body](#body)
- [HttpOption — Reusable Options](#httpoption-reusable-options)
- [WebSocketEngine — WebSocket Infrastructure](#websocketengine-websocket-infrastructure)
- [WebSocketClient — WebSocket Connection](#websocketclient-websocket-connection)
  - [Connect](#connect)
  - [Send messages](#send-messages)
  - [Receive events](#receive-events)
  - [Close](#close)
- [HttpException](#httpexception)
- [Full Example](#full-example)
- [Dependencies](#dependencies)

---

## Quick Start

```java
YojaApp.start();

HttpEngine engine = new HttpEngine();

HttpClient client = HttpClient.builder(engine)
    .host("api.example.com")
    .port(443)
    .ssl(true)
    .build();

client.send(HttpGet.of("/api/users"))
      .onSuccess(res -> System.out.println(res.bodyAsJsonArray()))
      .onFailure(Throwable::printStackTrace);
```

---

## HttpEngine: HTTP Infrastructure

`HttpEngine` manages the underlying Vert.x `WebClient`. Create one instance and share it across all `HttpClient` instances of your application. It implements `AutoCloseable`.

```java
// Default options (SSL + HTTP/2 + trust all)
HttpEngine engine = new HttpEngine();

// Custom Vert.x WebClientOptions
HttpEngine engine = new HttpEngine(new WebClientOptions()
    .setSsl(false)
    .setProtocolVersion(HttpVersion.HTTP_1_1));

// Log all active options to SLF4J (useful for debugging)
engine.logOptions();

// Inspect options
WebClientOptions opts = engine.options();
String host = engine.defaultHost(); // "localhost"
int    port = engine.defaultPort(); // 80

// Release resources
engine.close();
```

**Default options applied automatically:**

| Option | Value |
|---|---|
| SSL | `true` |
| ALPN | `true` |
| Protocol versions | HTTP/2 + HTTP/1.1 |
| Verify host | `true` |
| Trust all certificates | `true` |

---

## HttpClient: Send Requests

`HttpClient` targets a specific host/port and sends `HttpGet` or `HttpPost` requests. All calls return a Vert.x `Future<HttpResponse>`.

```java
HttpClient client = HttpClient.builder(engine)
    .host("api.example.com")
    .port(443)
    .ssl(true)
    .timeout(Duration.ofSeconds(30))
    .build();
```

| Builder method | Description |
|---|---|
| `host(String)` | Target host (default: engine's default host) |
| `port(int)` | Target port (default: engine's default port) |
| `ssl(boolean)` | Enable or disable SSL |
| `timeout(Duration)` | Per-request timeout |

```java
// GET
Future<HttpResponse> res = client.send(HttpGet.of("/api/items"));

// POST
Future<HttpResponse> res = client.send(HttpPost.of("/api/items", payload));

// Inspect the client
String   host    = client.host();
int      port    = client.port();
boolean  ssl     = client.isSsl();
Duration timeout = client.timeout();
```

---

## HttpGet: GET Requests

### Query parameters

```java
// Simple GET (no parameters)
HttpGet req = HttpGet.of("/api/users");

// GET from a query string
HttpGet req = HttpGet.of("/api/users", "page=1&limit=20");

// Builder — add parameters one by one
HttpGet req = HttpGet.builder("/api/users")
    .addParameter("page",  "1")
    .addParameter("limit", "20")
    .addParameter("tag",   List.of("java", "vertx"))  // multi-value
    .build();

// putParameter replaces; addParameter appends
HttpGet req = HttpGet.builder("/api/users")
    .addParameter("tag", "java")
    .addParameter("tag", "vertx")   // → tag=java&tag=vertx
    .putParameter("page", "2")      // replace
    .build();
```

Read parameters back from the object:

```java
boolean has  = req.hasParameter("page");           // true
String  page = req.firstParameter("page");         // "1"
List<String> tags = req.parameters("tag");         // ["java", "vertx"]
Set<String>  names = req.parameterNames();
int size = req.parameterSize();
```

### Headers and cookies

Available on both `HttpGet` and `HttpPost` via the shared builder:

```java
HttpGet req = HttpGet.builder("/api/users")
    .putHeader("Authorization", "Bearer token123")
    .putHeader("Accept-Language", "en")
    .putCookie("session", "abc123")
    .build();

// Remove a header or cookie (pass null as value)
HttpGet req = HttpGet.builder("/api/users")
    .putHeader("X-Old-Header", null)
    .build();

// Inspect
boolean has   = req.hasHeader("Authorization");
String  auth  = req.headers().get("Authorization");
Set<String> names = req.headerNames();

boolean hasCookies = req.hasCookie();
String  session    = req.cookie("session");
Map<String, String> cookies = req.cookies();
```

---

## HttpPost: POST Requests

### Body types

```java
// JsonObject → Content-Type: application/json
HttpPost req = HttpPost.of("/api/users",
    new JsonObject().put("name", "Alice").put("email", "alice@example.com"));

// JsonArray → Content-Type: application/json
HttpPost req = HttpPost.of("/api/tags",
    new JsonArray().add("java").add("vertx"));

// String → Content-Type: text/plain
HttpPost req = HttpPost.of("/api/log", "plain text message");

// byte[] → raw binary (no Content-Type set)
HttpPost req = HttpPost.of("/api/upload", fileBytes);

// No body
HttpPost req = HttpPost.of("/api/ping");

// Builder form
HttpPost req = HttpPost.builder("/api/users")
    .body(new JsonObject().put("name", "Alice"))
    .putHeader("Authorization", "Bearer token123")
    .putCookie("session", "abc123")
    .build();
```

Read the body back from the object:

```java
JsonObject obj   = req.bodyAsJsonObject();
JsonArray  arr   = req.bodyAsJsonArray();
String     text  = req.bodyAsText();
byte[]     bytes = req.bodyAsBinary();
MyDto      dto   = req.body(MyDto.class);   // maps JsonObject via mapTo()
```

---

## HttpResponse: Read Responses

### Status

```java
Future<HttpResponse> future = client.send(HttpGet.of("/api/users"));

future.onSuccess(res -> {
    int    code    = res.statusCode();    // 200, 404, 500…
    String message = res.statusMessage(); // "OK", "Not Found"…
    HttpVersion version = res.version();  // HTTP_1_1 or HTTP_2
});
```

### Headers

```java
boolean has   = res.hasHeader("Content-Type");
String  ct    = res.header("Content-Type");
Set<String> names = res.headerNames();
```

### Cookies

```java
boolean hasCookies = res.hasCookie();
boolean hasToken   = res.hasCookie("token");

// All cookies with a given name
Set<HttpCookie> tokens = res.cookies("token");

// Specific cookie by name + domain + path
HttpCookie cookie = res.cookie("token", "example.com", "/");

// All cookies
Set<HttpCookie> all = res.cookies();
```

### Body

```java
JsonObject obj   = res.bodyAsJsonObject();
JsonArray  arr   = res.bodyAsJsonArray();
String     text  = res.bodyAsText();
byte[]     bytes = res.bodyAsBinary();

// Map JSON body to a POJO (via JsonObject.mapTo())
MyDto dto = res.body(MyDto.class);
```

---

## HttpOption: Reusable Options

`HttpOption` is a configuration value object (SSL + timeout) that can be shared or stored independently of a client instance.

```java
HttpOption option = HttpOption.builder()
    .ssl(true)
    .timeout(Duration.ofSeconds(30))
    .build();

HttpOption noTimeout = HttpOption.builder()
    .ssl(true)
    .noTimeout()
    .build();

boolean  ssl     = option.isSsl();
Duration timeout = option.timeout(); // null if noTimeout()
```

| Builder method | Default | Description |
|---|---|---|
| `ssl(boolean)` | `true` | Enable or disable SSL |
| `timeout(Duration)` | `null` | Per-request timeout |
| `noTimeout()` | — | Explicitly set timeout to null |

---

## WebSocketEngine: WebSocket Infrastructure

`WebSocketEngine` manages the underlying Vert.x `WebSocketClient`. Create one instance and share it across all `WebSocketClient` connections.

```java
// Default options (SSL + trust all + ALPN)
WebSocketEngine engine = new WebSocketEngine();

// Custom options
WebSocketEngine engine = new WebSocketEngine(
    new WebSocketClientOptions().setSsl(false));

// Debug
engine.logOptions();

// Inspect
WebSocketClientOptions opts = engine.options();
String host = engine.defaultHost(); // "localhost"
int    port = engine.defaultPort(); // 80

// Release resources (async)
engine.close().onSuccess(v -> System.out.println("closed"));
```

**Default options applied automatically:**

| Option | Value |
|---|---|
| SSL | `true` |
| Verify host | `true` |
| Trust all certificates | `true` |
| ALPN | `true` |

---

## WebSocketClient: WebSocket Connection

### Connect

```java
WebSocketEngine engine = new WebSocketEngine();

WebSocketClient.builder(engine, "/ws/chat")
    .host("api.example.com")
    .port(443)
    .ssl(true)
    .timeout(Duration.ofSeconds(10))
    .connect()
    .onSuccess(ws -> System.out.println("connected to " + ws.getPath()))
    .onFailure(Throwable::printStackTrace);
```

| Builder method | Description |
|---|---|
| `host(String)` | Target host |
| `port(int)` | Target port |
| `ssl(boolean)` | Enable or disable SSL |
| `timeout(Duration)` | Connection timeout |
| `connect()` | Returns `Future<WebSocketClient>` |

### Send messages

```java
// Text message
ws.send("Hello").onSuccess(v -> System.out.println("sent"));

// Binary message
ws.send(new byte[]{0x01, 0x02, 0x03});
```

### Receive events

Multiple handlers can be registered per event type.

```java
// Text message received
ws.onTextMessage(event -> {
    System.out.println("path:    " + event.path());
    System.out.println("message: " + event.message());
});

// Binary message received
ws.onBinaryMessage(event -> {
    System.out.println("path: " + event.path());
    byte[] data = event.message();
});

// Connection closed
ws.onClose(event -> {
    System.out.println("closed: " + event.path());
});
```

**Event record types:**

| Type | Fields |
|---|---|
| `TextMessageEvent` | `path`, `message` (String) |
| `BinaryMessageEvent` | `path`, `message` (byte[]) |
| `CloseEvent` | `path` |

### Close

```java
ws.close().onSuccess(v -> System.out.println("connection closed"));
```

---

## HttpException

Unchecked exception thrown by the HTTP client module. Extends `YojaAppException`.

```java
try {
    HttpResponse res = awaitValue(client.send(HttpGet.of("/api/data")));
} catch (HttpException e) {
    System.err.println(e.getMessage());
}
```

---

## Full Example

```java
YojaApp.start();

// --- HTTP client ---
HttpEngine httpEngine = new HttpEngine();

HttpClient client = HttpClient.builder(httpEngine)
    .host("api.example.com")
    .port(443)
    .ssl(true)
    .timeout(Duration.ofSeconds(30))
    .build();

// GET with parameters and auth header
HttpGet getRequest = HttpGet.builder("/api/users")
    .addParameter("page", "1")
    .addParameter("limit", "10")
    .putHeader("Authorization", "Bearer token123")
    .build();

client.send(getRequest)
      .onSuccess(res -> {
          System.out.println("Status: " + res.statusCode());
          JsonArray users = res.bodyAsJsonArray();
          System.out.println("Users: " + users.size());
      });

// POST with JSON body
HttpPost postRequest = HttpPost.builder("/api/users")
    .body(new JsonObject()
        .put("name", "Alice")
        .put("email", "alice@example.com"))
    .putHeader("Authorization", "Bearer token123")
    .build();

client.send(postRequest)
      .onSuccess(res -> {
          if (res.statusCode() == 201) {
              MyDto created = res.body(MyDto.class);
              System.out.println("Created: " + created.name());
          }
      });

// --- WebSocket client ---
WebSocketEngine wsEngine = new WebSocketEngine();

WebSocketClient.builder(wsEngine, "/ws/notifications")
    .host("api.example.com")
    .port(443)
    .ssl(true)
    .connect()
    .onSuccess(ws -> {
        ws.onTextMessage(event ->
            System.out.println("Notification: " + event.message()));

        ws.onClose(event ->
            System.out.println("Disconnected from: " + event.path()));

        ws.send("subscribe:news");
    });
```

---

## Dependencies

| Library | Usage |
|---|---|
| [Vert.x Web Client](https://vertx.io/docs/vertx-web-client/java/) | HTTP/2 client |
| [Vert.x WebSocket](https://vertx.io/docs/vertx-core/java/#_websockets) | WebSocket client |
| [Netty](https://netty.io/) | Cookie decoding (`ClientCookieDecoder`) |
| [SLF4J](https://www.slf4j.org/) | Logging |
| yoja-core | `YojaApp`, `HttpCookie`, `HttpHeader`, `YojaAppException` |
