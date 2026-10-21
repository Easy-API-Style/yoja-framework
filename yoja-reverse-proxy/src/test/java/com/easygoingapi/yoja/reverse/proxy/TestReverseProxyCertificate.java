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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpCertificate;
import com.easygoingapi.yoja.core.util.SslUtil;
import com.easygoingapi.yoja.core.util.SslUtil.SslCertificate;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.reverse.proxy.util.TestUtil;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;

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

    @Test
    public void test_certificate_rotation_before_admin_start() throws Exception {
        final ReverseProxyServer server =
            awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                         .admin(ADMIN_PORT, "token")
                                         .ssl(TestUtil.sslFolder.resolve("ssl-key.pem"),
                                              TestUtil.sslFolder.resolve("ssl-cert.pem"))
                                         .rules(Set.of())
                                         .start());
        try {
            // the admin server is not listening: only the proxy is updated...
            final SslCertificate rotated = generateCertificate();
            assertEquals(Boolean.TRUE, awaitValue(server.updateCertificate(rotated.sslKeyPath(),
                                                                           rotated.sslCertPath())));
            assertEquals(State.stopped, server.adminState());
            // ... and the admin server started afterwards serves the rotated certificate too
            await(server.startAdmin());
            final Certificate expected = readCertificate(rotated.sslCertPath());
            assertEquals(expected, servedCertificate(PROXY_PORT));
            assertEquals(expected, servedCertificate(ADMIN_PORT));
        }
        finally {
            await(server.stop());
            assertEquals(State.stopped, server.proxyState());
            assertEquals(State.stopped, server.adminState());
        }
    }

    @Test
    public void test_certificate_rotation_without_tls() {
        final ReverseProxyServer server =
            awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                         .rules(Set.of())
                                         .start());
        try {
            final Future<Boolean> future = server.updateCertificate(TestUtil.sslFolder.resolve("ssl-key.pem"),
                                                                    TestUtil.sslFolder.resolve("ssl-cert.pem"));
            await(future);
            assertTrue(future.failed());
            assertInstanceOf(ReverseProxyException.class, future.cause());
        }
        finally {
            await(server.stop());
        }
    }

    @Test
    public void test_start_stop_idempotent() {
        final ReverseProxyServer server =
            awaitValue(ReverseProxyServer.builder(PROXY_PORT)
                                         .admin(ADMIN_PORT, "token")
                                         .options(new HttpServerOptions().setHost("127.0.0.1"))
                                         .rules(Set.of())
                                         .start());
        try {
            // a second start() doesn't try to bind the port again
            final Future<Void> started = server.start();
            await(started);
            assertTrue(started.succeeded(), String.valueOf(started.cause()));
            assertEquals(State.started, server.proxyState());
            // stopping an admin server never started is a no-op
            final Future<Void> stopped = server.stopAdmin();
            await(stopped);
            assertTrue(stopped.succeeded(), String.valueOf(stopped.cause()));
            assertEquals(State.stopped, server.adminState());
            // started twice, stopped twice
            await(server.startAdmin());
            await(server.startAdmin());
            assertEquals(State.started, server.adminState());
            await(server.stopAdmin());
            await(server.stopAdmin());
            assertEquals(State.stopped, server.adminState());
        }
        finally {
            await(server.stop());
            assertEquals(State.stopped, server.proxyState());
        }
    }

    /** @return the certificate a TLS server presents on {@code localhost:port} (not verified: it is self-signed) */
    private static Certificate servedCertificate(final int port) throws Exception {
        final TrustManager trustAll = new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {}
            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) {}
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { trustAll }, null);
        try (final SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket("localhost", port)) {
            socket.startHandshake();
            return socket.getSession().getPeerCertificates()[0];
        }
    }

    /** @return the certificate of a PEM file */
    private static Certificate readCertificate(final Path pemFile) throws Exception {
        try (final InputStream inputStream = Files.newInputStream(pemFile)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
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
