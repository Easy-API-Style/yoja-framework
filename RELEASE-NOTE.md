## 1.7.0
* every yoja-web `onXXX(action)` returns its `off()` -> called, the action is no more told (no `removeXXX`): `yojaWeb`, `httpClient`, `languageService`, `urlParameterService`, `storageService`, `webSocketService`, `responsiveService`, `eventService.on(...)`
* add `section.track(off)` -> the `off` runs once the section has left the page: the action of a section gone is never told
* every listener isolated -> an action throwing is logged and told to `yojaWeb.onError(...)`, the others told all the same; one removed while an event is told is not told
* `responsiveService.onMedia(...)` / `onResize(...)` use the common listeners -> return their `off`
* `eventService.trigger(...)` no longer throws an action's error -> told to `yojaWeb.onError(...)`; `eventService.remove(...)` removes the events' actions too
* remove `webSocketService` keyed listeners (`put...`, `has...`, `remove(eventType, url, key)`) -> use the `off` of `on...(...)` and `section.track(off)`
* a section's resource failing to load (controler, `yw-css`, `yw-slot`, `yw-include`) -> the section built without it, the others with theirs, the error told to `yojaWeb.onError(...)`; `append`/`prepend`/`appendTag`/`prependTag` no longer rejected for it
* `yojaWeb.onDocumentReady(...)` given once the document is ready -> told all the same, just after the call
* `section.controlerReady(...)` / `pageReady(...)` given once the section is ready -> told all the same, just after the call; the section gone: never told (no more `TypeError`)
* fix `onTagReady` documentation -> the action is given the tags added
* `sectionService` queries take `({tag, predicate})` too, no tag: the document -> as `controlerService`
* fix `closestControler`/`parentControler` (and `sectionService.closest`/`parent`) nothing found up to the top -> `null`, no more `TypeError`
* the body's own `style.display` kept once the page is built (no more reset to `''`)
* `prepend(...)` errors say `prepend`
* `yojaWeb.version()` read from the file name (`YojaWeb-<version>.js`) -> no more version hard-coded in the script
* controler `className()` defined on its class -> available in its constructor, a controler's own `className()` kept
* add `WebSocket.asyncAccept(path, accept)` -> a handshake accepted asynchronously (a session looked up from its cookie)
* `WebSocketParameter` gives the headers and cookies of the upgrade request -> `header(name)`, `headers(name)`, `cookie(name)`, `cookieNames()`
* `HttpSessionStore.get(null)` -> a `null` session (no session cookie), no more error
* dev build version is the next patch's snapshot (`1.6.0` -> `1.6.1-SNAPSHOT`)

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

