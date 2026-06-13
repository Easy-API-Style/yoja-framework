# yoja-reverse-proxy

[![Website](https://img.shields.io/badge/website-easygoingapi.com%2Fyoja--reverse--proxy-blue)](https://easygoingapi.com/modules/reverse-proxy.html) [![Email](https://img.shields.io/badge/email-easy.api.contact%40gmail.com-red)](mailto:easy.api.contact@gmail.com)

[![Release](https://img.shields.io/badge/release-1.0.1-brightgreen)](https://github.com/Easy-API-Style/yoja-framework/releases/tag/1.0.1)
[![License](https://img.shields.io/badge/license-Apache%202.0-green)](https://github.com/Easy-API-Style/yoja-framework/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange)](https://openjdk.org/projects/jdk/25/)
[![Maven Central](https://img.shields.io/badge/maven--central-com.easygoingapi%3Ayoja--reverse--proxy%3A1.0.1-blue)](https://central.sonatype.com/artifact/com.easygoingapi/yoja-reverse-proxy/1.0.1)

Reverse proxy module of the Yoja Framework. Routes incoming HTTP and WebSocket traffic to backend services based on host/path rules, with optional load balancing, path rewriting, SSL termination, and a live admin API.

## Installation

```groovy
dependencies {
    implementation 'com.easygoingapi:yoja-reverse-proxy:VERSION'
}
```

Requires `yoja-core` to be started beforehand (`YojaApp.start()`).

---

## Table of Contents

- [Quick Start](#quick-start)
- [ReverseProxyServer — Server Lifecycle](#reverseproxyserver-server-lifecycle)
- [ReverseProxyRule — Routing Rules](#reverseproxyrule-routing-rules)
  - [Simple host routing](#simple-host-routing)
  - [Path-based routing](#path-based-routing)
  - [Path rewriting](#path-rewriting)
  - [Load balancing](#load-balancing)
  - [SSL upstream](#ssl-upstream)
- [elseRule — Fallback Resolver](#elserule-fallback-resolver)
- [onResolve — Observe routing decisions](#onresolve-observe-routing-decisions)
- [Admin API — Live rule management](#admin-api-live-rule-management)
  - [Admin REST endpoints](#admin-rest-endpoints)
  - [Rule JSON format](#rule-json-format)
- [SSL / TLS Termination](#ssl-tls-termination)
- [Silent mode](#silent-mode)
- [ReverseProxyResult](#reverseproxyresult)
- [ReverseProxyException](#reverseproxyexception)

---

## Quick Start

```java
YojaApp.start();

ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("api.example.com"),
    ReverseProxyRule.Url.to(false, "localhost").port(8080).build()
);

ReverseProxyServer.builder(80)
                  .rule(rule)
                  .start()
                  .onSuccess(proxy -> {
                    System.out.println("Proxy started on port " + proxy.proxyPort());
                  });
```

Any request arriving on port `80` with host `api.example.com` is forwarded to `localhost:8080`.

---

## ReverseProxyServer: Server Lifecycle

### Start

```java
Future<ReverseProxyServer> future = ReverseProxyServer.builder(80)
                                                      .rule(rule1)
                                                      .rule(rule2)
                                                      .start();

future.onSuccess(proxy -> {
    System.out.println("proxy port:  " + proxy.proxyPort());
    System.out.println("proxy state: " + proxy.proxyState()); // started
});
```

### Stop

```java
proxy.stop()
     .onSuccess(v -> System.out.println("proxy stopped"));
```

### State

State follows the same lifecycle as `HttpServer`:

```java
// stopping | stopped | starting | started
proxy.proxyState();
proxy.adminState();
```

### Admin server (optional)

The admin server exposes a REST API to update rules at runtime without restarting. It is bound to a separate port and protected by a token.

```java
ReverseProxyServer.builder(80)
                  .admin(9090, "my-secret-token")
                  .rule(rule)
                  .start();
```

Start/stop the admin server independently:

```java
proxy.startAdmin().onSuccess(v -> System.out.println("admin started on " + proxy.adminPort()));
proxy.stopAdmin();
```

---

## ReverseProxyRule: Routing Rules

A rule maps an incoming `from` (host + optional path prefix) to a `to` (host + ports).

```java
new ReverseProxyRule(from, to)
```

### Simple host routing

Forward all traffic from `app.example.com` to `localhost:3000`:

```java
ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("app.example.com"),
    ReverseProxyRule.Url.to(false, "localhost").port(3000).build()
);
```

### Path-based routing

Forward only traffic whose path starts with `/api` on host `example.com` to `localhost:8080`:

```java
ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("example.com", "/api"),
    ReverseProxyRule.Url.to(false, "localhost").port(8080).build()
);
```

Rules are matched **longest-path-first**: `/api/v2` is tested before `/api` before `/`.

### Path rewriting

Two optional path modifiers can be applied on the `to` side:

| Option | Effect |
|---|---|
| `cutsPathWith(prefix)` | Strips the given prefix from the incoming path |
| `startsPathWith(prefix)` | Prepends the given prefix to the (possibly cut) path |

**Example — strip `/api` prefix before forwarding:**

```
Incoming: example.com/api/users
Forward to: localhost:8080/users
```

```java
ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("example.com", "/api"),
    ReverseProxyRule.Url.to(false, "localhost")
                        .port(8080)
                        .cutsPathWith("/api")   // /api/users → /users
                        .build()
);
```

**Example — strip `/api` and prepend `/v2`:**

```
Incoming: example.com/api/orders
Forward to: localhost:8080/v2/orders
```

```java
ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("example.com", "/api"),
    ReverseProxyRule.Url.to(false, "localhost")
                        .port(8080)
                        .cutsPathWith("/api")      // /api/orders → /orders
                        .startsPathWith("/v2")     // /orders → /v2/orders
                        .build()
);
```

**Example — only prepend a prefix (no cut):**

```
Incoming: example.com/items
Forward to: localhost:8080/internal/items
```

```java
ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("example.com"),
    ReverseProxyRule.Url.to(false, "localhost")
                        .port(8080)
                        .startsPathWith("/internal")
                        .build()
);
```

> **Constraint:** if both `from.path()` and `cutsPathWith` are set, `from.path()` must start with `cutsPathWith`.

### Load balancing

Declare multiple ports on the `to` side. The proxy uses a **least-connections** algorithm to pick the port with the fewest active requests:

```java
ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("api.example.com"),
    ReverseProxyRule.Url.to(false, "localhost")
                        .port(8081, 8082, 8083)   // or .ports(Set.of(8081, 8082, 8083))
                        .build()
);
```

### SSL upstream

Set `ssl = true` on `to` when the backend uses HTTPS/WSS:

```java
ReverseProxyRule rule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("secure.example.com"),
    ReverseProxyRule.Url.to(true, "backend.internal")  // ssl=true
                        .port(443)
                        .build()
);

// Also tell the proxy engine to use SSL when connecting upstream
ReverseProxyServer.builder(80)
                  .rule(rule)
                  .sslProxy(true) // enables SSL on the outbound WebClient / WebSocket client
                  .start();
```

### Register multiple rules

```java
ReverseProxyServer.builder(80)
    .rule(new ReverseProxyRule(
        ReverseProxyRule.Url.from("api.example.com", "/auth"),
        ReverseProxyRule.Url.to(false, "localhost").port(8081).build()))
    .rule(new ReverseProxyRule(
        ReverseProxyRule.Url.from("api.example.com", "/data"),
        ReverseProxyRule.Url.to(false, "localhost").port(8082).build()))
    .rule(new ReverseProxyRule(
        ReverseProxyRule.Url.from("api.example.com"),
        ReverseProxyRule.Url.to(false, "localhost").port(8080).build()))
    .start();
```

Or pass a set:

```java
Set<ReverseProxyRule> rules = Set.of(rule1, rule2, rule3);

ReverseProxyServer.builder(80)
                  .rules(rules)
                  .start();
```

---

## elseRule: Fallback Resolver

For requests that don't match any registered rule, provide a custom `Function<HttpUrl, HttpUrl>` that computes the target URL dynamically. Return `null` to leave the request unresolved.

```java
ReverseProxyServer.builder(80)
    .rule(apiRule)
    .elseRule(fromUrl -> {
        // Dynamic routing based on subdomain
        String host = fromUrl.host(); // e.g. "tenant1.example.com"
        String tenant = host.split("\\.")[0]; // "tenant1"

        return HttpUrl.builder("localhost")
                      .protocol(HttpProtocole.http)
                      .port(tenantPortMap.get(tenant))
                      .path(fromUrl.path())
                      .build();
    })
    .start();
```

---

## onResolve: Observe routing decisions

Register one or more handlers called for every request (resolved or not). Useful for logging, metrics, or monitoring.

```java
ReverseProxyServer.builder(80)
    .rule(rule)
    .onResolve(result -> {
        if (result.isResolved()) {
            System.out.println(result.fromUrl() + " → " + result.toUrl());

            // Check if it was matched by a rule
            if (result instanceof ReverseProxyRuleResult ruleResult) {
                System.out.println("matched rule: " + ruleResult.reverseProxyRule());
            }
        } 
        else {
            System.out.println("unresolved: " + result.fromUrl());
        }
    })
    .onResolve(result -> metrics.record(result)) // multiple handlers allowed
    .start();
```

---

## Admin API: Live rule management

When started with `.admin(port, token)`, the proxy exposes a REST API on the admin port to manage rules without restarting.

All requests must include the token in the `Reserve-Proxy-Token` header.  
To rotate the token, send the new token in the `New-Reserve-Proxy-Token` header.

### Admin REST endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/load/rules` | List all active rules |
| `POST` | `/put/rule` | Add or replace a single rule |
| `POST` | `/put/rules` | Add or replace rules in bulk |
| `POST` | `/remove/rule` | Remove a single rule |
| `POST` | `/remove/rules` | Remove rules in bulk |
| `POST` | `/update/token` | Rotate the admin token |

### Rule JSON format

A rule is serialized as:

```json
{
  "from": {
    "host": "api.example.com",
    "path": "/api"
  },
  "to": {
    "ssl": false,
    "host": "localhost",
    "ports": [8080, 8081],
    "cutsPathWith": "/api",
    "startsPathWith": "/v1"
  }
}
```

`path`, `cutsPathWith`, and `startsPathWith` are optional.

### Example: add a rule via HTTP

```bash
curl -X POST http://localhost:9090/put/rule \
  -H "Reserve-Proxy-Token: my-secret-token" \
  -H "Content-Type: application/json" \
  -d '{
    "from": { "host": "new.example.com" },
    "to":   { "ssl": false, "host": "localhost", "ports": [9000] }
  }'
```

### Example: list active rules

```bash
curl http://localhost:9090/load/rules \
  -H "Reserve-Proxy-Token: my-secret-token"
```

### Example: remove a rule

```bash
curl -X POST http://localhost:9090/remove/rule \
  -H "Reserve-Proxy-Token: my-secret-token" \
  -H "Content-Type: application/json" \
  -d '{ "host": "new.example.com" }'
```

### Example: rotate the admin token

```bash
curl -X POST http://localhost:9090/update/token \
  -H "Reserve-Proxy-Token: my-secret-token" \
  -H "New-Reserve-Proxy-Token: new-secret-token"
```

### Header names

```java
String tokenHeader    = ReverseProxyServer.adminTokenHeaderKey();    // "Reserve-Proxy-Token"
String newTokenHeader = ReverseProxyServer.adminNewTokenHeaderKey(); // "New-Reserve-Proxy-Token"
```

---

## SSL TLS Termination

The proxy can terminate SSL, exposing HTTPS/WSS to clients while forwarding plain HTTP/WS to backends.

### PEM certificate

```java
ReverseProxyServer.builder(443)
                  .ssl(Path.of("/etc/ssl/key.pem"), Path.of("/etc/ssl/cert.pem"))
                 .rule(rule)
                 .start();
```

### Self-signed certificate (dev/test)

```java
ReverseProxyServer.builder(8443)
                  .sslSelfSigned()
                  .rule(rule)
                  .start();
```

### Hot-reload certificate (zero downtime)

Updates both the proxy server and the admin server simultaneously:

```java
proxy.updateCertificate(
    Path.of("/new/key.pem"),
    Path.of("/new/cert.pem")
).onSuccess(ok -> System.out.println("Both servers reloaded: " + ok));
```

Inspect current paths:

```java
Path key  = proxy.keyPath();
Path cert = proxy.certificatePath();
```

---

## Silent mode

Controls how the proxy reacts when no rule (and no `elseRule` resolver) matches an incoming request. Configured on the builder via `.silent(boolean)`. **Default: `true`.**

### Behavior

| Mode | HTTP | WebSocket |
|---|---|---|
| `silent(true)` *(default)* | The handler returns **without writing a response**. The connection stays open until the client times out. | The handshake is **neither accepted nor rejected**. The client waits until its own timeout. |
| `silent(false)` | Returns `404 Not Found` with `Content-Type: text/plain`. | The handshake is **actively rejected** by the server. |

In both modes:
- Resolved requests are forwarded normally.
- Registered `onResolve` handlers are still invoked — silent mode only affects what is sent back to the client, not internal observability.

### When to use silent mode

- **Stealth / security**: avoids confirming to a scanner whether a host is a proxy. An unresolved request looks like a host that simply doesn't respond, rather than a host that says "no".
- **Reduce noise from automated probes**: bots scanning for vulnerable endpoints get nothing back, no error page to fingerprint.
- **Behind another front-end**: when the proxy sits behind another router that already handles 404s, silent mode avoids generating a competing response.

### When to disable silent mode

- During development — you want to see explicit failures.
- When the proxy is the only public entry point and clients expect proper HTTP semantics (a real 404 is friendlier to debug than a hung connection).
- For monitoring tools that rely on HTTP status codes.

### Example

```java
ReverseProxyServer.builder(80)
    .rule(rule)
    .silent(false)   // explicit 404 for unmatched requests
    .start();
```

> **Note:** silent mode does *not* swallow real errors. If an upstream backend is unreachable or the request fails after a rule matched, the proxy still returns `500 Internal Server Error` with the stack trace. Silent mode only governs the **unresolved** case.

---

## ReverseProxyResult

Returned in `onResolve` handlers for every incoming request.

```java
proxy.onResolve(result -> {
    boolean resolved  = result.isResolved();
    HttpUrl from      = result.fromUrl();   // original incoming URL
    HttpUrl to        = result.toUrl();     // target URL, null if unresolved

    // Downcast when matched by a rule
    if (result instanceof ReverseProxyRuleResult ruleResult) {
        ReverseProxyRule rule = ruleResult.reverseProxyRule();
        System.out.println("from rule: " + rule.from().path());
        System.out.println("to host:   " + rule.to().host());
        System.out.println("to ports:  " + rule.to().ports());
    }
});
```

### `ReverseProxyResult`

| Method | Return | Description |
|---|---|---|
| `isResolved()` | `boolean` | `true` if a target URL was found |
| `fromUrl()` | `HttpUrl` | The original incoming URL |
| `toUrl()` | `HttpUrl` | The resolved target URL (`null` if unresolved) |

### `ReverseProxyRuleResult` (extends `ReverseProxyResult`)

| Method | Return | Description |
|---|---|---|
| `reverseProxyRule()` | `ReverseProxyRule` | The rule that matched |

---

## ReverseProxyException

Unchecked exception thrown by the proxy module.

```java
// Thrown on invalid rule configuration
new ReverseProxyRule(
    ReverseProxyRule.Url.from("example.com", "/api"),
    ReverseProxyRule.Url.to(false, "localhost")
                        .port(8080)
                        .cutsPathWith("/other")  // ERROR: from.path() must start with cutsPathWith
                        .build()
); // → ReverseProxyException

// Thrown when registering duplicate rules
ReverseProxyServer.builder(80)
    .rule(rule)
    .rule(rule);    // same from → ReverseProxyException

// Thrown when admin port is not configured
proxy.startAdmin(); // → ReverseProxyException if .admin() was not called
```

---

## Full Example

```java
YojaApp.start();

// Route /api traffic, strip prefix, load balance across 3 backends
ReverseProxyRule apiRule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("example.com", "/api"),
    ReverseProxyRule.Url.to(false, "localhost")
                        .port(8081, 8082, 8083)
                        .cutsPathWith("/api")
                        .build()
);

// Route /static traffic to a dedicated asset server
ReverseProxyRule staticRule = new ReverseProxyRule(
    ReverseProxyRule.Url.from("example.com", "/static"),
    ReverseProxyRule.Url.to(false, "localhost").port(9000).build()
);

ReverseProxyServer.builder(80)
    // Named rules
    .rule(apiRule)
    .rule(staticRule)
    // Fallback: anything else → main app on port 3000
    .elseRule(fromUrl -> HttpUrl.builder("localhost")
        .protocol(HttpProtocole.http)
        .port(3000)
        .path(fromUrl.path())
        .build())
    // Admin API on port 9090
    .admin(9090, "super-secret-token")
    // Logging
    .onResolve(result -> {
        if (result.isResolved()) {
            System.out.printf("[proxy] %s → %s%n", result.fromUrl().url(Format.decoded),
                                                   result.toUrl().url(Format.decoded));
        } 
        else {
            System.out.printf("[proxy] unresolved: %s%n", result.fromUrl().url(Format.decoded));
        }
    })
    .silent(false)   // return 404 for truly unresolved requests
    .start()
    .onSuccess(proxy -> {
        System.out.println("Proxy running: " + proxy);
        proxy.startAdmin(); // start the admin server
    });
```

---

## Dependencies

| Library | Usage |
|---|---|
| [Vert.x Core](https://vertx.io/) | HTTP server, WebSocket, event loop |
| [Vert.x Web](https://vertx.io/docs/vertx-web/java/) | Router, body handling |
| [Vert.x Web Client](https://vertx.io/docs/vertx-web-client/java/) | Outbound HTTP requests to backends |
| [Guava](https://github.com/google/guava) | Utilities |
| [Jackson](https://github.com/FasterXML/jackson) | Rule JSON serialization/deserialization |
| [SLF4J](https://www.slf4j.org/) | Logging |
| yoja-core | `YojaApp`, `Worker`, `HttpUrl`, `Certificatable` |
| yoja-http-client | `HttpEngine`, `WebSocketEngine` |
