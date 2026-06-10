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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
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

public class TestHttpSession {
	
    @Test
    public void test_01() {
        final HttpClient httpClient = newHttpClient();
        final AtomicReference<HttpSession> httpSession = new AtomicReference<>();
        final AtomicReference<String> dataValue = new AtomicReference<>();
        final AtomicReference<String> value_5 = new AtomicReference<>();
        
        final Handler<HttpRouting> httpRouting_1 = h -> {
            final HttpSession session = h.session();
            httpSession.set(session);
            session.computeIfAbsent("key_1", value -> "value_1");
            session.computeIfAbsent("key_1", value -> "value_11");
            session.put("key_2", "value_2");
            session.put("key_3", "value_3");
            h.putData("key_4", "value_4");
            session.put("key_5", "value_5");
            h.nextHandler();
        };
        final Handler<HttpRouting> httpRouting_2 = h -> {
            final HttpSession session = h.session();
            session.remove("key_2");
            dataValue.set(h.getData("key_4"));
            value_5.set(session.get("key_5"));
            h.response().send();
        };
        
        final WebService webService_1 = new WebService(HttpMethod.GET, 
                                                       "/webService_1", 
                                                       httpRouting_1,
                                                       httpRouting_2);
        final HttpSessionStore httpSessionStore = new HttpSessionStore("session.yoja", Duration.ofDays(365));
        final AtomicReference<HttpSession> onHttpSession = new AtomicReference<>();
        httpSessionStore.onSession(s -> {
            onHttpSession.set(s);
        });
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .session(httpSessionStore)
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webService_1"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(200, httpResponseClient.statusCode());
            assertEquals("value_1", httpSession.get().get("key_1"));
            assertNull(httpSession.get().get("key_2"));
            assertEquals("value_3", httpSession.get().get("key_3"));
            assertNull(httpSession.get().get("key_4"));
            assertEquals("value_4", dataValue.get());
            assertEquals("value_5", value_5.get());
            
            assertEquals(httpSessionStore, httpServer.httpSessionStore());
            
            final String id = httpSession.get().id();
            
            Set<HttpSession> httpSessions = awaitValue(httpSessionStore.sessions());
            assertEquals(1, httpSessions.size());
            
            HttpSession _httpSession = awaitValue(httpSessionStore.get(id));
            assertEquals(httpSession.get(), _httpSession);
            assertEquals(httpSession.get(), onHttpSession.get());
            assertEquals("value_3", _httpSession.get("key_3"));
            
            String oldId = httpSession.get().oldId();
            assertEquals(null, oldId);
            assertEquals(false, onHttpSession.get().isRegenerated());
            
            httpSession.get().regenerateId();
            oldId = httpSession.get().oldId();
            assertEquals(id, oldId);
            assertEquals(true, httpSession.get().isRegenerated());
            
            _httpSession = awaitValue(httpSessionStore.get(oldId));
            assertEquals(httpSession.get(), _httpSession);
            
            _httpSession = awaitValue(httpSessionStore.get(httpSession.get().id()));
            assertEquals(null, _httpSession);
            
            _httpSession = awaitValue(httpSessionStore.get(oldId));
            assertEquals(httpSession.get(), _httpSession);
            
            httpSession.get().regenerateId();
            assertEquals(oldId, httpSession.get().oldId());
            
            httpSessionStore.delete(httpSession.get().id());
            httpSessions = awaitValue(httpSessionStore.sessions());
            assertEquals(1, httpSessions.size());
            
            httpSessionStore.delete(oldId);
            httpSessions = awaitValue(httpSessionStore.sessions());
            assertEquals(0, httpSessions.size());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }
    
    @Test
    public void test_02() {
        final HttpClient httpClient = newHttpClient();
        final Handler<HttpRouting> httpRouting_1 = h -> {
            h.response().send();
        };
        final WebService webService_1 = new WebService(HttpMethod.GET, 
                                                       "/webService_1", 
                                                       httpRouting_1);
        final HttpSessionStore httpSessionStore = new HttpSessionStore("session.yoja", Duration.ofDays(365));
        final HttpRouter httpRouter =
            HttpRouter.builder()
                      .session(httpSessionStore)
                      .webService(webService_1)
                      .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final Future<HttpResponse> httpResponseFuture = httpClient.send(HttpGet.of("/webService_1"));
            final HttpResponse httpResponseClient = awaitValue(httpResponseFuture);
            assertEquals(200, httpResponseClient.statusCode());
            Set<HttpSession> httpSessions = awaitValue(httpSessionStore.sessions());
            assertEquals(1, httpSessions.size());
            httpSessionStore.clear();
            httpSessions = awaitValue(httpSessionStore.sessions());
            assertEquals(0, httpSessions.size());
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    
    }
    
}
