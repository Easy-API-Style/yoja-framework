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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;

/** A path sent by the client reaches the server as it was given: encoded on the way, decoded on arrival. */
public class TestWebService_PathEncoding {

    @Test
    public void test_01() {
        assertEquals("été 2026 #1?", roundTrip("/echo/été 2026 #1?"), "a space, an accent, a # and a ? in a segment");
    }

    @Test
    public void test_02() {
        assertEquals("a b", roundTrip("/echo/a%20b"), "a path given encoded: not encoded twice");
    }

    /** @return the {@code :name} parameter the server read from that path; its whole path checked on the way */
    private static String roundTrip(final String path) {
        final HttpClient httpClient = newHttpClient();
        final AtomicReference<String> name = new AtomicReference<>();
        final AtomicReference<String> received = new AtomicReference<>();
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(HttpMethod.GET, "/echo/:name", v -> {
                                                    name.set(v.request().firstParameter("name"));
                                                    received.set(v.request().path());
                                                    v.response().send();
                                                })
                                                .build();
        final HttpServer httpServer = newHttpServer(httpRouter);
        try {
            final HttpResponse response = awaitValue(httpClient.send(HttpGet.of(path)));
            assertEquals(200, response.statusCode());
            assertEquals("/echo/" + name.get(), received.get(), "the whole path, decoded");
            return name.get();
        }
        finally {
            await(httpServer.stop());
            assertTrue(httpServer.is(State.stopped));
        }
    }

}
