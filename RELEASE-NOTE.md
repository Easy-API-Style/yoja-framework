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

