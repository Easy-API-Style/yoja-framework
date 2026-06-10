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

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.http.server.WebApp.Type;

import io.vertx.core.Future;

public class TestWebResource_folder {

    @Test
    public void test_01() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.folder("src/test/resources/com/easygoingapi/yoja/http/server/test/web/resource");
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contentType("xml", "application/xml")
                      .contentType("js", "application/javascript")
                      .contentType("txt", "plain/text")
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webResource_1.txt"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(200, httpResponseClient.statusCode());
            assertEquals("OK", httpResponseClient.statusMessage());
            assertEquals("webResource_1", httpResponseClient.bodyAsText());
            assertEquals("plain/text", httpResponseClient.header("content-type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_02() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.folder("src/test/resources/com/easygoingapi/yoja/http/server/test/web");
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/resource/webResource_1.txt"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(200, httpResponseClient.statusCode());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_03() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.folder("src/test/resources/com/easygoingapi/yoja/http/server/test/web");
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webResource_1.txt"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(404, httpResponseClient.statusCode());
            assertEquals("Not Found", httpResponseClient.statusMessage());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_04() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.folder("src/test/resources/com/easygoingapi/yoja/http/server/test/web/resource");
        
        final WebResource webResource_1 = new WebResource(webApp, "/");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webResource_1.txt"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(404, httpResponseClient.statusCode());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_05() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.folder, "src/test/resources/com/easygoingapi/yoja/http/server/test/web/resource")
                                    .contextPath("/root/path")
                                    .build();
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webResource_1.txt"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(404, httpResponseClient.statusCode());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_06() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.folder, "src/test/resources/com/easygoingapi/yoja/http/server/test/web/resource")
                                    .contextPath("/root/path")
                                    .build();
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/root/path/webResource_1.txt"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(200, httpResponseClient.statusCode());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_07() {
        Exception exception = null;
        try {
            final WebApp webApp = WebApp.builder(Type.folder, "src/test/resources/com/easygoingapi/yoja/http/server/test/web/resource")
                                        .contextPath("root/path")
                                        .build();
            final WebResource webResource_1 = new WebResource(webApp, "/*");
            final HttpRouter httpRouter = HttpRouter.builder()
                                                    .webResource(webResource_1)
                                                    .build();
        }
        catch (Exception e) {
            exception = e;
        }
        assertEquals("context path must begin with '/'", exception.getMessage());
    }

    @Test
    public void test_08() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.folder, "src/test/resources/com/easygoingapi/yoja/http/server/test/web/resource")
                                    .contextPath("/root/path")
                                    .build();
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("root/path/webResource_1.txt"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(404, httpResponseClient.statusCode());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

    @Test
    public void test_09() {
        final HttpClient httpClient = newHttpClient();
        
        final WebApp webApp = WebApp.builder(Type.folder, "src/test/resources/com/easygoingapi/yoja/http/server/test/web/resource")
                                    .contextPath("/root/path")
                                    .build();
        
        final WebResource webResource_1 = new WebResource(webApp, "/*");
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .contentTypes(Map.of("xml", "application/xml", 
                                           "js", "application/javascript", 
                                           "txt", "plain/text"))
                      .webResource(webResource_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/root/path/webResource_2.js"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(200, httpResponseClient.statusCode());
            assertEquals("application/javascript", httpResponseClient.header("Content-Type"));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
}
