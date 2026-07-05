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

import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.core.util.SslUtil;
import com.easygoingapi.yoja.core.util.SslUtil.SslCertificate;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.http.server.util.TestUtil;

import io.vertx.core.Handler;

public class TestHttpsCertificate {

    private static final int PORT = 9972;
    private static final String HOST = "localhost";

    @Test
    public void test_https_getters_and_certificate_rotation() {
        final HttpClient client = newHttpClient(true, PORT, HOST);
        final Handler<HttpRouting> handler = v -> v.response().send("ok");
        final HttpRouter router = HttpRouter.builder()
                                            .webService(new WebService(HttpMethod.GET, "/ping", handler))
                                            .build();
        final HttpServer server = newHttpServer(true, PORT, router);
        try {
            // --- getters reflect the configured PEM material ---
            assertEquals(HttpCertificate.SSL, server.certificate());
            assertEquals(TestUtil.sslFolder.resolve("ssl-key.pem"), server.keyPath());
            assertEquals(TestUtil.sslFolder.resolve("ssl-cert.pem"), server.certificatePath());

            // sanity: the HTTPS server serves before rotation
            assertEquals(200, awaitValue(client.send(HttpGet.of("/ping"))).statusCode());

            // --- generate a distinct certificate and hot-swap it ---
            final SslCertificate rotated = generateCertificate();
            final Boolean updated = awaitValue(server.updateCertificate(rotated.sslKeyPath(),
                                                                        rotated.sslCertPath()));
            assertEquals(Boolean.TRUE, updated);

            // getters now reflect the rotated certificate
            assertEquals(rotated.sslKeyPath(), server.keyPath());
            assertEquals(rotated.sslCertPath(), server.certificatePath());

            // the server keeps serving on the same listener after rotation
            final HttpResponse afterRotation = awaitValue(client.send(HttpGet.of("/ping")));
            assertEquals(200, afterRotation.statusCode());
            assertEquals("ok", afterRotation.bodyAsText());
        }
        finally {
            await(server.stop());
            assertTrue(server.is(State.stopped));
        }
    }

    /** Generates a fresh self-signed certificate; skips the test when {@code openssl} is missing. */
    private static SslCertificate generateCertificate() {
        try {
            return SslUtil.createSslCertificate(Path.of("build", "TestHttpsCertificate"));
        }
        catch (final Exception e) {
            Assumptions.abort("openssl unavailable to generate the rotation certificate: " + e.getMessage());
            throw new AssertionError("unreachable");
        }
    }

}
