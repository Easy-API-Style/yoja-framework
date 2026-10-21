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
import static com.easygoingapi.yoja.http.server.util.TestUtil.newHttpClient;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpPost;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.util.TestUtil;

import io.vertx.core.Future;

/**
 * A failed exchange never throws: it fails the future, with the Vert.x error itself (a caller tells a refused
 * connection from a timeout). (A request that cannot be built fails it too, with an HttpException.)
 */
public class TestHttpClientFailure {

    @Test
    public void test_01() throws IOException {
        final HttpClient httpClient = newHttpClient(TestUtil.ssl, closedPort(), TestUtil.host);

        final Future<HttpResponse> response = httpClient.send(HttpGet.of("/nobody"));   // nothing thrown here
        await(response);

        assertTrue(response.failed(), "nobody listens: the exchange fails, in the future");
        assertInstanceOf(java.net.ConnectException.class, response.cause(), "the transport's own error");
    }

    @Test
    public void test_02() throws IOException {
        final HttpClient httpClient = newHttpClient(TestUtil.ssl, closedPort(), TestUtil.host);

        final Future<HttpResponse> response = httpClient.send(HttpPost.of("/nobody", "body"));   // nothing thrown here
        await(response);

        assertTrue(response.failed(), "nobody listens: the exchange fails, in the future");
        assertInstanceOf(java.net.ConnectException.class, response.cause(), "the transport's own error");
    }

    /** @return a port nobody listens on (free a moment ago) */
    private static int closedPort() throws IOException {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

}
