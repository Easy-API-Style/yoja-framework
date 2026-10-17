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

