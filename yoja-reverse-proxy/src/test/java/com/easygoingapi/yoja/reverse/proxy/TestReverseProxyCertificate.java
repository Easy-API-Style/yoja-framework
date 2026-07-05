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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.util.SslUtil;
import com.easygoingapi.yoja.core.util.SslUtil.SslCertificate;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.reverse.proxy.util.TestUtil;

public class TestReverseProxyCertificate {

    private static final int PROXY_PORT = 9981;
    private static final int ADMIN_PORT = 9982;

    @Test
    public void test_certificate_rotation_updates_paths() {
        final ReverseProxyServer server =
            awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                         .admin(ADMIN_PORT, "token")
                                         .ssl(TestUtil.sslFolder.resolve("ssl-key.pem"),
                                              TestUtil.sslFolder.resolve("ssl-cert.pem"))
                                         .rules(Set.of())
                                         .start());
        try {
            await(server.startAdmin());

            // getters reflect the configured PEM material
            assertEquals(HttpCertificate.SSL, server.certificate());
            assertEquals(PROXY_PORT, server.proxyPort());
            assertEquals(ADMIN_PORT, (int) server.adminPort());
            assertEquals(TestUtil.sslFolder.resolve("ssl-key.pem"), server.keyPath());
            assertEquals(TestUtil.sslFolder.resolve("ssl-cert.pem"), server.certificatePath());

            // hot-swap a distinct certificate on both proxy and admin servers
            final SslCertificate rotated = generateCertificate();
            final Boolean updated = awaitValue(server.updateCertificate(rotated.sslKeyPath(),
                                                                        rotated.sslCertPath()));
            assertEquals(Boolean.TRUE, updated);

            // getters now reflect the rotated certificate
            assertEquals(rotated.sslKeyPath(), server.keyPath());
            assertEquals(rotated.sslCertPath(), server.certificatePath());
        }
        finally {
            await(server.stop());
            assertEquals(State.stopped, server.proxyState());
        }
    }

    @Test
    public void test_stopAdmin_keeps_proxy_running() {
        final ReverseProxyServer server =
            awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                         .admin(ADMIN_PORT, "token")
                                         .rules(Set.of())
                                         .start());
        try {
            await(server.startAdmin());
            assertEquals(State.started, server.adminState());
            assertEquals(State.started, server.proxyState());

            await(server.stopAdmin());
            assertEquals(State.stopped, server.adminState());
            // stopping the admin must not stop the proxy
            assertEquals(State.started, server.proxyState());
        }
        finally {
            await(server.stop());
            assertEquals(State.stopped, server.proxyState());
        }
    }

    /** Generates a fresh self-signed certificate; skips the test when {@code openssl} is missing. */
    private static SslCertificate generateCertificate() {
        try {
            return SslUtil.createSslCertificate(Path.of("build", "TestReverseProxyCertificate"));
        }
        catch (final Exception e) {
            Assumptions.abort("openssl unavailable to generate the rotation certificate: " + e.getMessage());
            throw new AssertionError("unreachable");
        }
    }

}
