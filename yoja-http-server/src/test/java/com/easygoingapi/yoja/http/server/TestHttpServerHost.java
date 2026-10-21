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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.http.HttpMethod;

/** A server listens on the host of its options: every interface by default, the loopback only when asked. */
public class TestHttpServerHost {

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    /** A server set to 127.0.0.1 answers there, and is not reached through the machine's network address. */
    @Test
    public void test_01() throws Exception {
        final Optional<InetAddress> lan = lanAddress();
        Assumptions.assumeTrue(lan.isPresent(), "no network address other than the loopback");
        final int port = freePort();
        final HttpServer httpServer = start(port, HttpServer.defaultOptions(HttpCertificate.NONE).setHost("127.0.0.1"));
        try {
            assertEquals(200, get("127.0.0.1", port).statusCode());
            assertThrows(IOException.class, () -> get(lan.get().getHostAddress(), port),
                         "not reached through " + lan.get().getHostAddress());
        }
        finally {
            await(httpServer.stop());
        }
    }

    /** By default, a server listens on every interface: reached through the machine's network address too. */
    @Test
    public void test_02() throws Exception {
        final Optional<InetAddress> lan = lanAddress();
        Assumptions.assumeTrue(lan.isPresent(), "no network address other than the loopback");
        final int port = freePort();
        final HttpServer httpServer = start(port, HttpServer.defaultOptions(HttpCertificate.NONE));
        try {
            assertEquals(200, get(lan.get().getHostAddress(), port).statusCode());
        }
        finally {
            await(httpServer.stop());
        }
    }

    private static HttpServer start(final int port,
                                    final io.vertx.core.http.HttpServerOptions options) {
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(HttpMethod.GET, "/ping", v -> v.response().send("pong"))
                                                .build();
        return awaitValue(HttpServer.builder(httpRouter, port).options(options).start());
    }

    private static HttpResponse<String> get(final String host,
                                            final int port) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + "/ping")).GET().build(),
                           HttpResponse.BodyHandlers.ofString());
    }

    private static int freePort() throws IOException {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /** @return an IPv4 address of this machine other than the loopback, if any */
    private static Optional<InetAddress> lanAddress() throws Exception {
        for (final NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }
            for (final InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                if (address instanceof Inet4Address) {
                    return Optional.of(address);
                }
            }
        }
        return Optional.empty();
    }

}
