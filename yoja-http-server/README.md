# yoja-http-server

[![Website](https://img.shields.io/badge/website-easygoingapi.com%2Fyoja--http--server-blue)](https://easygoingapi.com/modules/http-server.html)

[![Release](https://img.shields.io/badge/release-1.0.0-brightgreen)](https://github.com/Easy-API-Style/yoja-framework/releases/tag/1.0.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](https://github.com/Easy-API-Style/yoja-framework/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange)](https://openjdk.org/projects/jdk/25/)
[![Maven Central](https://img.shields.io/badge/com.easygoingapi%3Ayoja--http--server%3A1.0.0-blue)](https://central.sonatype.com/artifact/com.easygoingapi/yoja-http-server/1.0.0)

HTTP server module of the Yoja Framework. Provides a declarative API to build REST services, serve static web resources, manage sessions, and handle WebSocket connections — built on top of [Vert.x Web](https://vertx.io/docs/vertx-web/java/).

## Installation

```groovy
dependencies {
    implementation 'com.easygoingapi:yoja-http-server:VERSION'
}
```

Requires `yoja-core` to be started beforehand (`YojaApp.start()`).

---

## Table of Contents

- [Quick Start](#quick-start)
- [HttpServer — Server Lifecycle](#httpserver-server-lifecycle)
- [HttpRouter — Route Configuration](#httprouter-route-configuration)
  - [REST Endpoints — WebService](#rest-endpoints-webservice)
  - [Static Resources: WebResource and WebApp](#static-resources-webresource-and-webapp)
  - [Context Path](#context-path)
  - [Content-Type mapping](#content-type-mapping)
- [HttpRouting — Handler Context](#httprouting-handler-context)
  - [HttpRequest](#httprequest)
  - [HttpResponse](#httpresponse)
- [Cookies](#cookies)
  - [Read cookies from a request](#read-cookies-from-a-request)
  - [Write cookies in a response](#write-cookies-in-a-response)
  - [HttpCookie — builder](#httpcookie-builder)
- [Sessions](#sessions)
- [Request and Response Interceptors](#request-and-response-interceptors)
- [WebSocket](#websocket)
- [JSON Utilities](#json-utilities)
  - [JsonWriter](#jsonwriter)
  - [JsonReader](#jsonreader)
  - [Mapper](#mapper)
- [SSL / TLS](#ssl-tls)
- [HttpServerException](#httpserverexception)

---

## Quick Start

```java
// 1. Start the Yoja runtime (default options)
YojaApp.start();

// Or increase the event-loop thread count before starting:
YojaApp.vertxOptions()
       .setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2)
       .setWorkerPoolSize(40);
YojaApp.start();

// 2. Build the router
HttpRouter router = HttpRouter.builder()
    .webService(HttpMethod.GET, "/hello", routing -> {
        routing.response().send("Hello, World!");
    })
    .webService(HttpMethod.GET, "/api/users", routing -> {
        JsonArray users = fetchUsers();
        routing.response().send(users);
    })
    .build();

// 3. Start the server on port 8080
HttpServer.builder(router, 8080)
    .start()
    .onSuccess(server -> System.out.println("Started on port " + server.port()))
    .onFailure(Throwable::printStackTrace);
```

---

## HttpServer: Server Lifecycle

`HttpServer` is the entry point for the HTTP server. Created via its `Builder`.

### Start

```java
HttpServer.builder(router, 8080)
    .start()
    .onSuccess(server -> {
        System.out.println("port:  " + server.port());
        System.out.println("state: " + server.state()); // started
    });
```

### Stop

```java
server.stop()
      .onSuccess(s -> System.out.println("stopped"));
```

### State

```java
HttpServer.State state = server.state();
// stopping | stopped | starting | started

boolean isStarted = server.is(HttpServer.State.started);
```

### SSL with PEM certificate

```java
HttpServer.builder(router, 443)
    .ssl(Path.of("/etc/ssl/key.pem"), Path.of("/etc/ssl/cert.pem"))
    .start();
```

### Self-signed certificate (dev/test)

```java
HttpServer.builder(router, 8443)
    .sslSelfSigned()
    .start();
```

### Custom server options

```java
HttpServerOptions options = new HttpServerOptions()
    .setMaxWebSocketFrameSize(1024 * 1024)
    .setIdleTimeout(30);

HttpServer.builder(router, 8080)
    .options(options)
    .start();
```

### Hot-reload SSL certificate

```java
// HttpServer implements Certificatable
server.updateCertificate(
    Path.of("/new/key.pem"),
    Path.of("/new/cert.pem")
).onSuccess(ok -> System.out.println("Certificate reloaded: " + ok));
```

### Log server options

```java
server.logOptions(); // logs all HttpServerOptions to SLF4J
```

---

## HttpRouter: Route Configuration

`HttpRouter` wires together all routes, resources, session store, and event hooks.

```java
HttpRouter router = HttpRouter.builder()
    .contextPath("/app")
    .session(new HttpSessionStore("JSESSIONID", Duration.ofMinutes(30)))
    .webService(HttpMethod.GET,  "/api/items",    this::getItems)
    .webService(HttpMethod.POST, "/api/items",    this::createItem)
    .webResource(WebApp.jar("com.example.webapp"), "/assets/*")
    .onRequest(this::logRequest)
    .onResponse(this::transformResponse)
    .build();
```

### REST Endpoints: WebService

Register HTTP handlers for a method + path. Multiple handlers are chained (middleware pattern).

```java
HttpRouter.builder()
    // Single handler
    .webService(HttpMethod.GET, "/api/products", routing -> {
        routing.response().send(loadProducts());
    })
    // Multiple chained handlers (auth → business logic)
    .webService(HttpMethod.GET, "/api/admin", authHandler, adminHandler)
    // Path parameters
    .webService(HttpMethod.GET, "/api/users/:id", routing -> {
        String id = routing.request().firstParameter("id");
        routing.response().sendJson(findUser(id));
    })
    // Wildcard
    .webService(HttpMethod.GET, "/api/*", routing -> {
        routing.response().statusCode(404);
        routing.response().send("Not found");
    })
    .build();
```

Register as a `WebService` object:

```java
WebService service = new WebService(HttpMethod.POST, "/api/orders",
    routing -> validateOrder(routing),
    routing -> persistOrder(routing)
);

HttpRouter.builder()
    .webService(service)
    .build();
```

Call the next handler in the chain:

```java
Handler<HttpRouting> authHandler = routing -> {
    if (!isAuthenticated(routing.request())) {
        routing.fail(401);
        return;
    }
    routing.nextHandler(); // proceed to next handler
};
```

### Static Resources: WebResource and WebApp

Serve static files from a JAR (classpath) or a filesystem folder.

**From a JAR (classpath resource):**

```java
// Serves files from the package "com/example/webapp/" inside the JAR
WebApp jar = WebApp.jar("com.example.webapp");

HttpRouter.builder()
    .webResource(jar, "/assets/*")
    .build();
```

**From a filesystem folder:**

```java
WebApp folder = WebApp.folder("/var/www/html");

HttpRouter.builder()
    .webResource(folder, "/*")
    .build();
```

**With a context path on the WebApp:**

```java
WebApp app = WebApp.builder(WebApp.Type.folder, "/var/www/html")
    .contextPath("/static")
    .build();

HttpRouter.builder()
    .webResource(app, "/static/*")
    .build();
```

**With custom response headers:**

```java
HttpHeader cacheHeaders = new HttpHeader()
    .put("Cache-Control", "public, max-age=86400")
    .put("X-Content-Type-Options", "nosniff");

HttpRouter.builder()
    .webResource(WebApp.folder("/var/www"), "/*", cacheHeaders)
    .build();
```

**With an interceptor handler (e.g. auth before serving):**

```java
HttpRouter.builder()
    .webResource(WebApp.jar("com.example.webapp"), "/private/*", authHandler)
    .build();
```

**Load a resource manually from a handler:**

```java
routing -> {
    boolean exists = routing.hasResource(webApp, "/images/logo.png");
    byte[] bytes   = routing.loadResource(webApp, "/images/logo.png");
    routing.response().send(bytes);
}
```

### Context Path

All routes are prefixed with the context path:

```java
HttpRouter.builder()
    .contextPath("/api/v1")
    // effective path: /api/v1/users
    .webService(HttpMethod.GET, "/users", handler)
    .build();
```

### Content-Type mapping

Map file extensions to MIME types for static resources:

```java
HttpRouter.builder()
    .contentType("js",   "application/javascript")
    .contentType("css",  "text/css")
    .contentType("woff2","font/woff2")
    .contentTypes(Map.of("svg", "image/svg+xml", "ico", "image/x-icon"))
    .build();
```

---

## HttpRouting: Handler Context

`HttpRouting` is passed to every handler. It gives access to the request, the response, and routing utilities.

```java
Handler<HttpRouting> handler = routing -> {
    HttpRequest  req  = routing.request();
    HttpResponse resp = routing.response();

    // Session (if configured)
    HttpSession session = routing.session();

    // Shared data across handlers in the same request
    routing.putData("userId", 42);
    int userId = routing.getData("userId");

    // Fail with HTTP status code
    routing.fail(403);
    routing.fail(new RuntimeException("unexpected error"));

    // Redirect
    routing.redirect("/login");

    // Context path
    String ctx = routing.contextPath();
};
```

### HttpRequest

Full access to the incoming HTTP request.

```java
HttpRequest req = routing.request();

// Method and path
HttpMethod   method = req.method();   // GET, POST
String       path   = req.path();     // "/api/users"
boolean      ssl    = req.ssl();
String       host   = req.host();
HttpVersion  ver    = req.version();

// Headers
boolean hasAuth   = req.hasHeader("Authorization");
String  authValue = req.header("Authorization");
Set<String> names = req.headerNames();

// Query parameters
boolean hasPage   = req.hasParameter("page");
String  page      = req.firstParameter("page");
List<String> tags = req.parameters("tag");     // multi-value
Set<String> pNames = req.parameterNames();
List<HttpParameter.Entry> allParams = req.parameters();

// Cookies
boolean hasCookie  = req.hasCookie("session");
String  cookieVal  = req.cookie("session");
Map<String, String> cookies = req.cookies();

// Body (POST / PUT)
boolean      empty  = req.isEmptyBody();
JsonObject   json   = req.bodyAsJsonObject();
JsonArray    array  = req.bodyAsJsonArray();
String       text   = req.bodyAsText();
byte[]       bytes  = req.bodyAsByteArray();
MyDto        dto    = req.body(MyDto.class);   // Jackson deserialization
```

### HttpResponse

Build and send the HTTP response.

**Send body:**

```java
HttpResponse resp = routing.response();

resp.send("plain text");
resp.send(new JsonObject().put("ok", true));
resp.send(new JsonArray().add("a").add("b"));
resp.send(new byte[]{0x01, 0x02});
resp.send(); // empty 200

// Serialize a POJO with Jackson
resp.sendJson(myDto);

// Serialize with a Jackson @JsonView
resp.sendJson(myDto, PublicView.class);
```

**Headers and cookies:**

```java
resp.putHeader("X-Custom", "value");
resp.putHeader(ContentType.key, ContentType.jsonObject.value());

resp.statusCode(201);

resp.addCookie(HttpCookie.builder("token", "abc123")
    .httpOnly(true)
    .secure(true)
    .maxAge(3600)
    .build());

resp.removeCookies("old-cookie");
```

**Check if already sent:**

```java
if (!resp.sent()) {
    resp.send("fallback");
}
```

---

## Cookies

### Read cookies from a request

```java
HttpRequest req = routing.request();

boolean has      = req.hasCookie("session");          // check presence
String  value    = req.cookie("session");              // get value by name
Map<String, String> all = req.cookies();               // get all cookies
```

### Write cookies in a response

```java
HttpResponse resp = routing.response();

// Add a cookie
resp.addCookie(HttpCookie.builder("token", "abc123")
    .httpOnly(true)
    .secure(true)
    .maxAge(3600)
    .path("/")
    .domain("example.com")
    .sameSite(CookieSameSite.STRICT)
    .build());

// Remove a cookie (sends Set-Cookie with maxAge=0)
resp.removeCookies("token");
```

### HttpCookie: builder

`HttpCookie` is the cookie value object shared by request reading and response writing.

```java
// Minimal
HttpCookie cookie = HttpCookie.of("name", "value");

// Full configuration
HttpCookie cookie = HttpCookie.builder("token", "abc123")
    .domain("example.com")          // Scope to domain
    .path("/api")                   // Scope to path
    .maxAge(3600)                   // TTL in seconds (0 = delete)
    .httpOnly(true)                 // Not accessible from JavaScript
    .secure(true)                   // HTTPS only
    .sameSite(CookieSameSite.STRICT) // STRICT | LAX | NONE
    .build();
```

**`CookieSameSite` values:**

| Value | Behaviour |
|---|---|
| `STRICT` | Cookie sent only on same-site requests |
| `LAX` | Cookie sent on same-site requests and top-level navigations |
| `NONE` | Cookie sent on all requests (requires `secure(true)`) |

**`HttpCookie` properties:**

| Property | Type | Description |
|---|---|---|
| `getName()` | `String` | Cookie name |
| `getValue()` | `String` | Cookie value |
| `getDomain()` | `String` | Scope domain (`null` = current) |
| `getPath()` | `String` | Scope path (`null` = current) |
| `getMaxAge()` | `long` | TTL in seconds (`0` = delete) |
| `getSameSite()` | `CookieSameSite` | SameSite policy |
| `isHttpOnly()` | `boolean` | Inaccessible from JavaScript |
| `isSecure()` | `boolean` | HTTPS only |

> `HttpCookie` equality is based on `name + domain + path`, not on `value`. Two cookies with the same name, domain and path are considered identical regardless of their value.

---

## Sessions

Enable server-side sessions with a cookie.

```java
// Configure session store (cookie name + session TTL)
HttpSessionStore store = new HttpSessionStore("JSESSIONID", Duration.ofMinutes(30));

// Hook on session creation
store.onSession(session -> {
    System.out.println("New session: " + session.id());
});

HttpRouter router = HttpRouter.builder()
    .session(store)
    .webService(HttpMethod.GET, "/login", routing -> {
        HttpSession session = routing.session();
        session.put("user", "alice");
        routing.response().send("logged in");
    })
    .webService(HttpMethod.GET, "/profile", routing -> {
        HttpSession session = routing.session();
        String user = session.get("user");
        routing.response().send("Hello, " + user);
    })
    .build();
```

### HttpSession API

```java
HttpSession session = routing.session();

String id = session.id();

// Store / retrieve data
session.put("key", value);
session.putIfAbsent("key", defaultValue);
session.computeIfAbsent("key", k -> computeValue(k));

String val = session.get("key");
session.remove("key");

Map<String, Object> all = session.data();
boolean empty = session.isEmpty();

// Timeout
Duration timeout = session.timeout();

// ID rotation (security best practice after login)
session.regenerateId();
boolean regenerated = session.isRegenerated();
String oldId = session.oldId();

// Destroy
session.destroy();
boolean destroyed = session.isDestroyed();
```

### HttpSessionStore API

```java
HttpSessionStore store = server.httpSessionStore();

store.size().onSuccess(n -> System.out.println(n + " active sessions"));

store.sessions().onSuccess(sessions -> sessions.forEach(s -> System.out.println(s.id())));

store.get("session-id-123").onSuccess(s -> System.out.println(s.data()));

store.delete("session-id-123");

store.clear(); // wipe all sessions
```

---

## Request and Response Interceptors

Global hooks called for every request or response.

### onRequest — abort or enrich requests

```java
HttpRouter.builder()
    .onRequest(event -> {
        // Read request info
        String path   = event.path();
        String method = event.method().name();

        // Abort the request (responds 444, handler is skipped)
        if (!isAuthorized(event)) {
            event.abort();
        }

        // Share data with downstream handlers
        event.routingContext().putData("requestId", UUID.randomUUID().toString());
    })
    .build();
```

### onResponse — inspect or rewrite responses

```java
HttpRouter.builder()
    .onResponse(event -> {
        // Read response body
        if (event.hasBody()) {
            HttpBodyType type = event.bodyType();

            if (HttpBodyType.JsonObject == type) {
                JsonObject body = event.bodyAsJsonObject();
                // Wrap every JSON response in an envelope
                event.updateBody(new JsonObject()
                    .put("data", body)
                    .put("ts", System.currentTimeMillis()));
            }
        }

        // Force a response header
        event.putHeader("X-Powered-By", "yoja");

        // Suppress the response entirely
        event.abort();
    })
    .build();
```

`HttpResponseEvent` update methods:

```java
event.updateJsonBody(myDto);               // serialize POJO
event.updateJsonBody(myDto, PublicView.class); // with Jackson view
event.updateBody(new JsonObject(...));
event.updateBody(new JsonArray(...));
event.updateBody("plain text");
event.updateBody(new byte[]{...});
event.clearBody();                         // send empty response
```

---

## WebSocket

### Declare and register WebSocket endpoints

```java
// Simple WebSocket on /ws/chat
WebSocket chat = new WebSocket("/ws/chat");

// With custom accept logic (token validation)
WebSocket secure = new WebSocket("/ws/feed", params -> {
    String token = params.firstValue("token");
    return isValidToken(token);
});
```

### Register with the server via WebSocketService

```java
WebSocketService wsService = new WebSocketService();
wsService.add(chat);
wsService.add(secure);

HttpServer.builder(router, 8080)
    .webSocketService(wsService)
    .start();
```

### Listen to events

```java
chat.onOpen(event -> {
    System.out.println("Client connected on " + event.path());

    // Access query parameters
    String room = event.webSocketParameter().firstValue("room");
});

chat.onClose(event -> {
    System.out.println("Closed: " + event.statusCode() + " - " + event.reason());
});

chat.onTextMessage(event -> {
    System.out.println("Received: " + event.message());

    // Echo back to all connected clients
    chat.send("Echo: " + event.message());
});

chat.onBinaryMessage(event -> {
    byte[] data = event.message();
    chat.send(data);
});
```

### Send messages (broadcast to all connected clients)

```java
chat.send("Server broadcast");
chat.send(new byte[]{0x01, 0x02});
```

### Close connections

```java
chat.close();                          // close all connections
chat.close((short) 1001);             // with status code
chat.close((short) 1001, "Shutting down"); // with reason
```

### WebSocket query parameters

```java
// Client connects to: ws://host/ws/feed?token=abc&channel=news
chat.onOpen(event -> {
    WebSocketParameter params = event.webSocketParameter();

    String token   = params.firstValue("token");
    String channel = params.firstValue("channel");
    boolean hasCh  = params.hasName("channel");

    List<String> all = params.values("tag"); // multi-value
    Set<String>  names = params.names();
});
```

### Check status and manage handlers

```java
boolean open = chat.isOpened(); // true if at least one client connected

// Remove all handlers for specific events
chat.clear(WebSocket.On.textMessage, WebSocket.On.binaryMessage);
chat.clear(WebSocket.On.open, WebSocket.On.close);
```

### WebSocketService — manage multiple endpoints

```java
WebSocketService service = server.webSocketService();

boolean hasFeed = service.hasWebSocket("/ws/feed");

WebSocket feed = service.getWebSocket("/ws/feed");

// Get all open / closed paths
Set<String> opened = service.getWebSocketPaths(WebSocketService.Status.open);
Set<String> closed = service.getWebSocketPaths(WebSocketService.Status.close);

// Get all paths
Set<String> all = service.getWebSocketPaths();

// Remove a WebSocket endpoint
service.remove("/ws/feed");
```

---

## JSON Utilities

### JsonWriter

Serialize Java objects to JSON strings, `JsonObject`, or `JsonArray`.

**Static helpers (no configuration needed):**

```java
// Serialize to String
String json = JsonWriter.writeValue(myDto);

// Serialize with a Jackson @JsonView
String json = JsonWriter.writeValue(myDto, PublicView.class);

// Serialize to JsonObject
JsonObject obj = JsonWriter.writeValueAsJsonObject(myDto, PublicView.class);

// Serialize a collection to JsonArray
JsonArray arr = JsonWriter.writeValueAsJsonArray(myList, PublicView.class);
```

**Builder (reusable, configured writer):**

```java
JsonWriter writer = JsonWriter.builder()
    .view(PublicView.class) // Jackson @JsonView filter
    .pretty(true)           // pretty-print
    .build();

String json      = writer.write(myDto);
JsonObject obj   = writer.writeAsJsonObject(myDto);
JsonArray  arr   = writer.writeAsJsonArray(myList);
```

### JsonReader

Deserialize JSON strings to POJOs.

**Static helper:**

```java
MyDto dto = JsonReader.readValue(jsonString, MyDto.class);
```

**Builder (with Jackson @JsonView):**

```java
JsonReader reader = JsonReader.builder()
    .view(InternalView.class)
    .build();

MyDto dto = reader.read(jsonString, MyDto.class);
```

### Mapper

Replace the global Jackson `ObjectMapper` used by the module.

```java
// Customize the shared mapper (e.g. add modules, configure date formats)
ObjectMapper mapper = JsonMapper.builder()
    .addModule(new JavaTimeModule())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .build();

Mapper.jsonMapper(mapper);

// Retrieve the current mapper
ObjectMapper current = Mapper.jsonMapper();
```

---

## SSL TLS

### PEM certificate from filesystem

```java
HttpServer.builder(router, 443)
    .ssl(Path.of("/etc/letsencrypt/live/example.com/privkey.pem"),
         Path.of("/etc/letsencrypt/live/example.com/fullchain.pem"))
    .start();
```

### Self-signed (development only)

```java
HttpServer.builder(router, 8443)
    .sslSelfSigned()
    .start();
```

### Hot-reload certificate (zero downtime)

```java
server.updateCertificate(
    Path.of("/new/privkey.pem"),
    Path.of("/new/fullchain.pem")
).onSuccess(reloaded -> System.out.println("Certificate updated: " + reloaded));
```

Inspect current certificate paths:

```java
Path key  = server.keyPath();
Path cert = server.certificatePath();
```

---

## HttpServerException

Unchecked exception thrown by the HTTP server module.

```java
try {
    HttpServer.builder(router, 8080).start();
} catch (HttpServerException e) {
    System.err.println(e.getMessage());
}

// Throw manually in handlers
routing -> {
    if (invalid) {
        throw new HttpServerException("invalid request");
    }
}
```

---

## Full Example

```java
// --- Session store ---
HttpSessionStore sessions = new HttpSessionStore("SID", Duration.ofHours(1));

// --- WebSockets ---
WebSocket notifications = new WebSocket("/ws/notifications");
notifications.onOpen(e  -> System.out.println("WS connected"));
notifications.onClose(e -> System.out.println("WS closed"));
notifications.onTextMessage(e -> notifications.send("ack: " + e.message()));

WebSocketService wsService = new WebSocketService();
wsService.add(notifications);

// --- Router ---
HttpRouter router = HttpRouter.builder()
    .contextPath("/api")
    .session(sessions)
    .contentType("js",  "application/javascript")
    .contentType("css", "text/css")
    // Static resources from classpath
    .webResource(WebApp.jar("com.example.frontend"), "/ui/*")
    // REST endpoints
    .webService(HttpMethod.GET,  "/users",     this::getUsers)
    .webService(HttpMethod.POST, "/users",     this::createUser)
    .webService(HttpMethod.GET,  "/users/:id", this::getUser)
    // Global request/response hooks
    .onRequest(event -> {
        if (StringUtil.isNullOrBlank(event.header("X-Api-Key"))) {
            event.abort();
        }
    })
    .onResponse(event -> {
        event.putHeader("X-Powered-By", "yoja");
    })
    .build();

// --- Server ---
YojaApp.start();

HttpServer.builder(router, 8080)
    .webSocketService(wsService)
    .start()
    .onSuccess(server -> {
        System.out.println("Server started on port " + server.port());

        // Push a notification to all WS clients
        notifications.send("Server ready");
    });
```

---

## Dependencies

| Library | Usage |
|---|---|
| [Vert.x Web](https://vertx.io/docs/vertx-web/java/) | Router, sessions, WebSocket |
| [Jackson](https://github.com/FasterXML/jackson) | JSON serialization / deserialization |
| [Guava](https://github.com/google/guava) | Resource loading, collections |
| [SLF4J](https://www.slf4j.org/) | Logging |
| yoja-core | `YojaApp`, `Worker`, `HttpCookie`, `HttpHeader`, etc. |
