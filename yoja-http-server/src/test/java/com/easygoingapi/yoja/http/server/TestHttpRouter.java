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
package com.easygoingapi.yoja.http.server;

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static com.easygoingapi.yoja.http.server.util.TestUtil.newHttpClient;
import static com.easygoingapi.yoja.http.server.util.TestUtil.newHttpServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.ContentType;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpPost;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.http.server.WebApp.Type;

import io.vertx.core.Future;
import io.vertx.core.Handler;

public class TestHttpRouter {

    @Test
    public void test_01() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .build();
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contextPath("/path")
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/path/webResource_1.txt")
                                             .build();
            final Future<HttpResponse> httpResponseFuture_1 = httpClient.send(httpGet_1);
            final HttpResponse httpResponseClient_1 = awaitValue(httpResponseFuture_1);
            assertEquals(false, httpResponseFuture_1.failed());
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("no-store", httpResponseClient_1.header("Cache-Control"));
            assertEquals("text/plain", httpResponseClient_1.header("content-type"));
            final HttpGet httpGet_2 = HttpGet.builder("/webResource_1.txt")
                                             .build();
            final HttpResponse httpResponseClient_2 = awaitValue(httpClient.send(httpGet_2));
            assertEquals(404, httpResponseClient_2.statusCode());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_02() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .contextPath("/resource")
                                    .build();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("OK");
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contextPath("/path")
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webService(webService_1)
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/webResource_1.txt").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(404, httpResponseClient_1.statusCode());

            final HttpGet httpGet_2 = HttpGet.builder("/path/resource/webResource_1.txt").build();
            final HttpResponse httpResponseClient_2 = awaitValue(httpClient.send(httpGet_2));
            assertEquals(200, httpResponseClient_2.statusCode());
            
            final HttpGet httpGet_3 = HttpGet.builder("/webService_1").build();
            final HttpResponse httpResponseClient_3 = awaitValue(httpClient.send(httpGet_3));
            assertEquals(404, httpResponseClient_3.statusCode());
            
            final HttpGet httpGet_4 = HttpGet.builder("/path/webService_1").build();
            final HttpResponse httpResponseClient_4 = awaitValue(httpClient.send(httpGet_4));
            assertEquals(200, httpResponseClient_4.statusCode());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_03() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .build();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("OK");
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contextPath("/path")
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webService(webService_1)
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/webResource_1.txt").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(404, httpResponseClient_1.statusCode());

            final HttpGet httpGet_2 = HttpGet.builder("/path/webResource_1.txt").build();
            final HttpResponse httpResponseClient_2 = awaitValue(httpClient.send(httpGet_2));
            assertEquals(200, httpResponseClient_2.statusCode());
            
            final HttpGet httpGet_3 = HttpGet.builder("/webService_1").build();
            final HttpResponse httpResponseClient_3 = awaitValue(httpClient.send(httpGet_3));
            assertEquals(404, httpResponseClient_3.statusCode());
            
            final HttpGet httpGet_4 = HttpGet.builder("/path/webService_1").build();
            final HttpResponse httpResponseClient_4 = awaitValue(httpClient.send(httpGet_4));
            assertEquals(200, httpResponseClient_4.statusCode());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_04() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .build();
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            final boolean has = v.hasResource(webApp, "/webResource_1.txt");
            v.response().send(String.valueOf(has));
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            final byte[] resource = v.loadResource(webApp, "/webResource_1.txt");
            v.response().send(resource);
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contextPath("/path")
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webService(webService_1)
                      .webService(webService_2)
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/path/webService_1").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("true", httpResponseClient_1.bodyAsText());
            
            final HttpGet httpGet_2 = HttpGet.builder("/path/webService_2").build();
            final HttpResponse httpResponseClient_2 = awaitValue(httpClient.send(httpGet_2));
            assertEquals(200, httpResponseClient_2.statusCode());
            assertEquals("webResource_1", new String(httpResponseClient_2.bodyAsBinary()));
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_05() {
        final HttpClient httpClient = newHttpClient();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.redirect("/webService_2");
            v.response().send("OK 1");
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().send("OK 2");
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contextPath("/path")
                      .webService(webService_1)
                      .webService(webService_2)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/path/webService_1").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("OK 2", httpResponseClient_1.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_06() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp_1 = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .contextPath("/resource")
                                    .build();
        
        final WebApp webApp_2 = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                      .build();
        
        final WebResource webResource_1 = new WebResource(webApp_1, "/*");
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send(v.contextPath());
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().send(v.contextPath(webApp_1) + ";" + v.contextPath(webApp_2));
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contextPath("/path")
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webService(webService_1)
                      .webService(webService_2)
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/path/webService_1").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("/path", httpResponseClient_1.bodyAsText());
            
            final HttpGet httpGet_2 = HttpGet.builder("/path/webService_2").build();
            final HttpResponse httpResponseClient_2 = awaitValue(httpClient.send(httpGet_2));
            assertEquals(200, httpResponseClient_2.statusCode());
            assertEquals("/path/resource;/path", httpResponseClient_2.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_07() {
        final HttpClient httpClient = newHttpClient();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.fail(555);
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.fail(new RuntimeException("ERROR 2"));
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .webService(webService_2)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/webService_1").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(555, httpResponseClient_1.statusCode());
            assertEquals("Server Error (555)", httpResponseClient_1.bodyAsText());
            
            final HttpGet httpGet_2 = HttpGet.builder("/webService_2").build();
            final HttpResponse httpResponseClient_2 = awaitValue(httpClient.send(httpGet_2));
            assertEquals(500, httpResponseClient_2.statusCode());
            assertEquals("Internal Server Error", httpResponseClient_2.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_08() {
        final HttpClient httpClient = newHttpClient();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().statusCode(666);
            v.nextHandler();
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().send("OK 2");
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, 
                                                       "/webService_1",
                                                       httpRouting_1,
                                                       httpRouting_2);
        
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/webService_1").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(666, httpResponseClient_1.statusCode());
            assertEquals("OK 2", httpResponseClient_1.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_09() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .build();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("OK");
        };
        
        final WebResource webResource_1 = new WebResource(webApp, "/*", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/webResource_1.txt").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("OK", httpResponseClient_1.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_10() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .build();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.nextHandler();
        };
        
        final WebResource webResource_1 = new WebResource(webApp, "/*", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/webResource_1.txt").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("webResource_1", httpResponseClient_1.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_11() {
        final HttpClient httpClient = newHttpClient();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.nextHandler();
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().send("OK 2");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, 
                                                       "/webService_1",
                                                       httpRouting_1,
                                                       httpRouting_2);
        final AtomicReference<String> body = new AtomicReference<>();
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .onRequest(h -> {
                          h.abort();
                          body.set(h.bodyAsText());
                      })
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost_1 = HttpPost.builder("/webService_1")
                                                .body("body")
                                                .build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpPost_1));
            assertEquals(444, httpResponseClient_1.statusCode());
            assertEquals(null, httpResponseClient_1.bodyAsText());
            assertEquals("body", body.get());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_12() {
        final HttpClient httpClient = newHttpClient();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.nextHandler();
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, 
                                                       "/webService_1",
                                                       httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost_1 = HttpPost.builder("/webService_1")
                                                .body("body")
                                                .build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpPost_1));
            assertEquals(404, httpResponseClient_1.statusCode());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_13() {
        final HttpClient httpClient = newHttpClient();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.nextHandler();
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            v.response().send("OK 2");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, 
                                                       "/webService_1",
                                                       httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.POST, 
                                                       "/webService_1",
                                                       httpRouting_2);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .webService(webService_2)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost_1 = HttpPost.builder("/webService_1")
                                                .body("body")
                                                .build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpPost_1));
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("OK 2", httpResponseClient_1.bodyAsText());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_14() {
        final HttpClient httpClient = newHttpClient();
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            v.response().send("OK 1");
        };
        final WebService webService_1 = new WebService(HttpMethod.POST, 
                                                       "/webService_1",
                                                       httpRouting_1);
        final AtomicReference<String> requestBody = new AtomicReference<>();
        final AtomicReference<String> responseBody = new AtomicReference<>();
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .onResponse(h -> {
                          h.abort();
                          requestBody.set(h.request().bodyAsText());
                          responseBody.set(h.bodyAsText());
                      })
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpPost httpPost_1 = HttpPost.builder("/webService_1")
                                                .body("requestBody")
                                                .build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpPost_1));
            assertEquals(444, httpResponseClient_1.statusCode());
            assertEquals("abort sending", httpResponseClient_1.bodyAsText());
            assertEquals("requestBody", requestBody.get());
            assertEquals("OK 1", responseBody.get());
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_15() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                    .build();
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        
        final Handler<HttpRouting> httpRouting_1 = v -> {
            final boolean has = v.hasResource(webApp, "/webResource_1.txt");
            v.response().send(String.valueOf(has));
        };
        final Handler<HttpRouting> httpRouting_2 = v -> {
            final byte[] resource = v.loadResource(webApp, "/webResource_1.txt");
            v.response().send(resource);
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final WebService webService_2 = new WebService(HttpMethod.GET, "/webService_2", httpRouting_2);
        
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contentTypes(Map.of("txt", ContentType.text.value(), 
                                           "js", ContentType.jsonObject.value()) )
                      .webService(webService_1)
                      .webService(webService_2)
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet_1 = HttpGet.builder("/webService_1").build();
            final HttpResponse httpResponseClient_1 = awaitValue(httpClient.send(httpGet_1));
            assertEquals(200, httpResponseClient_1.statusCode());
            assertEquals("true", httpResponseClient_1.bodyAsText());
            
            final HttpGet httpGet_2 = HttpGet.builder("/webService_2").build();
            final HttpResponse httpResponseClient_2 = awaitValue(httpClient.send(httpGet_2));
            assertEquals(200, httpResponseClient_2.statusCode());
            assertEquals("webResource_1", new String(httpResponseClient_2.bodyAsBinary()));
        }
        catch (final Exception e) {
			fail(e);
		}
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
}
