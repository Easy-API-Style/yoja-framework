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
package com.easygoingapi.yoja.reverse.proxy;

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static com.easygoingapi.yoja.reverse.proxy.util.TestUtil.newHttpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpPost;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;
import com.easygoingapi.yoja.reverse.proxy.util.TestUtil;

/**
 * What the proxy forwards -- a large body, streamed -- and what it refuses:
 * an upstream it can't reach or whose certificate it doesn't trust
 * ({@code 502}), the cause being logged, never sent to the client.
 */
public class TestReverseProxySecurity {

    private static final int PROXY_PORT = 7781;
    private static final int UPSTREAM_PORT = 1131;
    private static final int NO_UPSTREAM_PORT = 1132;

    /** @return a rule forwarding {@code /up} to {@code localhost:port} */
    private static ReverseProxyRule rule(final boolean ssl,
                                         final int port) {
        return new ReverseProxyRule(Url.from("localhost", "/up"),
                                    Url.to(ssl, "localhost")
                                       .cutsPathWith("/up")
                                       .port(port)
                                       .build());
    }

    @Test
    public void test_large_body_streamed() {
        // a bare Vert.x upstream: counts the bytes it receives as they stream in, answers as many
        final io.vertx.core.http.HttpServer upstream =
            YojaApp.vertx()
                   .createHttpServer()
                   .requestHandler(request -> {
                       final AtomicLong received = new AtomicLong();
                       request.handler(buffer -> received.addAndGet(buffer.length()));
                       request.endHandler(v -> request.response()
                                                      .putHeader("received", String.valueOf(received.get()))
                                                      .end("y".repeat((int) received.get())));
                   });
        awaitValue(upstream.listen(UPSTREAM_PORT));
        final ReverseProxyServer proxy = awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                                                      .rule(rule(false, UPSTREAM_PORT))
                                                                      .start());
        try {
            // larger than what the former WebClient-based proxy held in memory (10 MB), both ways
            final int size = 24 * 1024 * 1024;
            final HttpClient httpClient = newHttpClient(false, PROXY_PORT, "localhost");
            final HttpResponse httpResponse = awaitValue(httpClient.send(HttpPost.of("/up/upload", "x".repeat(size))));
            assertEquals(200, httpResponse.statusCode());
            assertEquals(String.valueOf(size), httpResponse.header("received"));
            assertEquals(size, httpResponse.bodyAsBinary().length);
        }
        finally {
            await(proxy.stop());
            await(upstream.close());
        }
    }

    @Test
    public void test_upstream_down() {
        final ReverseProxyServer proxy = awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                                                      .rule(rule(false, NO_UPSTREAM_PORT))
                                                                      .start());
        try {
            final HttpClient httpClient = newHttpClient(false, PROXY_PORT, "localhost");
            final HttpResponse httpResponse = awaitValue(httpClient.send(HttpGet.of("/up/hello/forest", "name=Paul")));
            assertEquals(502, httpResponse.statusCode());
            // the cause is logged, never sent to the client
            final String body = String.valueOf(httpResponse.bodyAsText());
            assertFalse(body.contains("Exception"), body);
            assertFalse(body.contains("at "), body);
        }
        finally {
            await(proxy.stop());
        }
    }

    @Test
    public void test_upstream_certificate_verified() {
        // the upstream serves a self-signed certificate
        final HttpServer upstream = awaitValue(TestReverseProxyServer.server(1, UPSTREAM_PORT, null, true));
        try {
            // not trusted: refused
            ReverseProxyServer proxy = awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                                                    .rule(rule(true, UPSTREAM_PORT))
                                                                    .sslProxy(true)
                                                                    .start());
            try {
                final HttpClient httpClient = newHttpClient(false, PROXY_PORT, "localhost");
                final HttpResponse httpResponse = awaitValue(httpClient.send(HttpGet.of("/up/hello/forest", "name=Paul")));
                assertEquals(502, httpResponse.statusCode());
            }
            finally {
                await(proxy.stop());
            }
            // trusted explicitly: forwarded
            proxy = awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                                 .rule(rule(true, UPSTREAM_PORT))
                                                 .sslProxyTrust(TestUtil.sslFolder.resolve("ssl-cert.pem"))
                                                 .start());
            try {
                final HttpClient httpClient = newHttpClient(false, PROXY_PORT, "localhost");
                final HttpResponse httpResponse = awaitValue(httpClient.send(HttpGet.of("/up/hello/forest", "name=Paul")));
                assertEquals(200, httpResponse.statusCode());
                assertEquals("server 1 hello Paul", httpResponse.bodyAsText());
            }
            finally {
                await(proxy.stop());
            }
        }
        finally {
            await(upstream.stop());
        }
    }

}
