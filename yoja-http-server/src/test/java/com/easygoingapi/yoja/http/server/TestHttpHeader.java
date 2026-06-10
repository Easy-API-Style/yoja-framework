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

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;

import io.vertx.core.Future;
import io.vertx.core.Handler;

public class TestHttpHeader {

    @Test
    public void test_01() {
        final AtomicReference<com.easygoingapi.yoja.http.server.HttpRequest> httpRequest = new AtomicReference<>();
        final AtomicReference<com.easygoingapi.yoja.http.server.HttpResponse> httpResponse = new AtomicReference<>();
        final Handler<HttpRouting> httpRouting_1 = v -> {
            httpRequest.set(v.request());
            httpResponse.set(v.response());
            v.response().putHeader("name_3", "value_3");
            v.response().putHeader(null, null);
            v.response().putHeader("name_4", "value_4");
            v.response().send("OK");
        };
        
        final AtomicReference<String> headerFromRequest = new AtomicReference<>();
        final AtomicReference<String> headerFromResponse = new AtomicReference<>();
        final WebService webService_1 = new WebService(HttpMethod.GET, "/webService_1", httpRouting_1);
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .webService(webService_1)
                      .onRequest(h -> {
                          headerFromRequest.set(h.header("name_1"));
                      })
                      .onResponse(h -> {
                          headerFromResponse.set(h.header("name_3"));
                          h.putHeader("name_2", "value_2");
                          h.putHeader("name_4", null);
                      })
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpGet httpGet = HttpGet.builder("/webService_1")
                                           .putHeader("name_1", "value_1")
                                           .build();
            
            final HttpClient httpClient = newHttpClient();
            final Future<HttpResponse> httpResponseFuture = httpClient.send(httpGet);
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(false, httpResponseFuture.failed());
            assertEquals(200, httpResponseClient.statusCode());
            assertEquals(null, httpResponse.get().header(null));
            assertEquals(true, httpResponse.get().hasHeader());
            assertEquals(true, httpResponse.get().hasHeader("name_2"));
            assertEquals(true, httpResponse.get().hasHeader("Name_2"));
            assertEquals("value_2", httpResponse.get().header("name_2"));
            assertEquals("value_3", httpResponse.get().header("name_3"));
            assertEquals(false, httpResponse.get().hasHeader("name_4"));
            assertEquals(null, httpResponse.get().header("name_4"));
            assertEquals("value_1", headerFromRequest.get());
            assertEquals("value_3", headerFromResponse.get());
            assertEquals(Set.of("name_1", "user-agent"), httpRequest.get().headerNames());
            assertEquals("value_1", httpRequest.get().header("name_1"));
            assertEquals(null, httpRequest.get().header(null));
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
}
