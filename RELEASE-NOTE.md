## 1.6.0
* reverse proxy streams the traffic with Vert.x `HttpProxy` (HTTP and WebSocket, any body size) -> replaces `ReverseProxySwitcherWebClient`
* reverse proxy verifies the upstreams' certificates (no more trust-all) -> `sslProxyTrust(Path)` for a self-signed upstream; TLS chosen per rule, `sslProxy(true)` forces it for all
* reverse proxy never sends a stack trace to the client: `502` for an unreachable upstream, the cause is logged
* reverse proxy silent mode resets the unresolved request; an unresolved WebSocket handshake is rejected with `404`
* reverse proxy listens on the host of its `HttpServerOptions`; fix certificate rotation of an admin server started later
* fix `ws`/`wss` protocol of a resolved WebSocket upstream
* yoja-reverse-proxy no longer exposes yoja-http-client (`api` -> `testImplementation`)
* implement `MailService` with the Vert.x mail client -> `send(...)` returns `Future<String>`, `MailConfig.builder(host)`, `Mail` cc/bcc/html
* `MailConfig.toString()` hides the password; `Mail.Builder.addFile(...)` fails on a missing file
* http server listens on the host of its `HttpServerOptions`
* encode client http request path
* fix httpClient exception
* fix cookie
* fix `allTrust == true`

## 1.5.0
* session cookie secure by default (HttpOnly, Secure, SameSite=Strict) -> `HttpSessionStore.builder(...)` to configure it
* remove `HttpSessionStore.SameSite` -> use Vert.x `CookieSameSite`
* add `yojaWeb.appendTag(...)` and `yojaWeb.prependTag(...)` -> manage `DocumentFragment`
* fix `style.display` on load
* fix `LanguageService.updateFrom(tags, language)`
* fix `sendJson(Collection<?> body)`
* update java dependencies

## 1.4.0
* fix http response of httpClient.js 
* add after/before browser hooks -> yoja-selenium

## 1.3.0
* use Vert.x 5.1.3
* remove Vert.x's deprecated `SelfSignedCertificate` -> no more `HttpCertificate.SELF_SIGNED`
* create `SslUtil` (yoja-core) -> use openssl to create certificat files
* improve `ProcessUtil` (yoja-core) -> add timeout
* fix update certificate -> file paths updated
* create JsonArrayHelper -> manage encoding
* use selenium-java 4.45.0
* use junit-jupiter-api 6.1.1

## 1.2.1
* fix `SeleniumService` timeouts
* improve `ywAssert.assertEquals(...)` 
* improve getter/setter HttpRoutingContext/HttpSession properties -> keys are java Classes

