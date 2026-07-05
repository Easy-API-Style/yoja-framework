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
package com.easygoingapi.yoja.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.easygoingapi.yoja.core.util.ProcessUtil;
import com.easygoingapi.yoja.core.util.SslUtil;
import com.easygoingapi.yoja.core.util.SslUtil.SslCertificate;

public class TestSslUtil {

    /** Skips the whole class when {@code openssl} is not available on the machine. */
    @BeforeAll
    public static void requireOpenssl() {
        boolean available;
        try {
            final Integer exitCode = ProcessUtil.execute(List.of("openssl", "version"),
                                                         line -> {},
                                                         line -> {},
                                                         true);
            available = exitCode != null && exitCode == 0;
        }
        catch (final Exception e) {
            available = false;
        }
        assumeTrue(available, "openssl is not installed");
    }

    /**
     * Returns (and cleans) the per-test output folder
     * {@code build/<TestClass>/<testMethod>}, relative to the module build dir.
     */
    private static Path testFolder(final TestInfo testInfo) throws IOException {
        final String className = testInfo.getTestClass().orElseThrow().getSimpleName();
        final String methodName = testInfo.getTestMethod().orElseThrow().getName();
        final Path folder = Path.of("build", className, methodName);
        deleteRecursively(folder);
        Files.createDirectories(folder);
        return folder;
    }

    private static void deleteRecursively(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    }
                    catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    private static X509Certificate readCertificate(final Path certPath) throws Exception {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        try (InputStream in = Files.newInputStream(certPath)) {
            return (X509Certificate) factory.generateCertificate(in);
        }
    }

    @Test
    public void test_creates_key_and_cert_files(final TestInfo testInfo) throws Exception {
        final Path folder = testFolder(testInfo);
        final SslCertificate certificate = SslUtil.createSslCertificate(folder);

        assertEquals(folder.resolve("ssl-key.pem"), certificate.sslKeyPath());
        assertEquals(folder.resolve("ssl-cert.pem"), certificate.sslCertPath());
        assertTrue(Files.isRegularFile(certificate.sslKeyPath()), "key file should exist");
        assertTrue(Files.isRegularFile(certificate.sslCertPath()), "cert file should exist");
        assertTrue(Files.size(certificate.sslKeyPath()) > 0, "key file should not be empty");
        assertTrue(Files.size(certificate.sslCertPath()) > 0, "cert file should not be empty");

        final String key = Files.readString(certificate.sslKeyPath());
        final String cert = Files.readString(certificate.sslCertPath());
        assertTrue(key.contains("PRIVATE KEY"), "key file should be a PEM private key");
        assertTrue(cert.contains("BEGIN CERTIFICATE"), "cert file should be a PEM certificate");
    }

    @Test
    public void test_creates_destination_folder_when_missing(final TestInfo testInfo) throws Exception {
        final Path missing = testFolder(testInfo).resolve("nested/does/not/exist");

        final SslCertificate certificate = SslUtil.createSslCertificate(missing);

        assertTrue(Files.isDirectory(missing), "missing folder should have been created");
        assertTrue(Files.isRegularFile(certificate.sslCertPath()), "cert file should exist");
    }

    @Test
    public void test_generated_certificate_is_valid_self_signed(final TestInfo testInfo) throws Exception {
        final SslCertificate certificate = SslUtil.createSslCertificate(testFolder(testInfo));

        final X509Certificate x509 = readCertificate(certificate.sslCertPath());
        assertTrue(x509.getSubjectX500Principal()
                       .getName()
                       .contains("CN=localhost"),
                   "subject should be CN=localhost");
        // Self-signed: subject == issuer, and the cert verifies with its own key.
        assertEquals(x509.getSubjectX500Principal(), x509.getIssuerX500Principal());
        x509.verify(x509.getPublicKey());
        // Should be valid right now.
        x509.checkValidity();
    }

    @Test
    public void test_validity_days_are_applied(final TestInfo testInfo) throws Exception {
        final int validityDays = 5;
        final SslCertificate certificate = SslUtil.createSslCertificate(testFolder(testInfo), "/CN=localhost", validityDays);

        final X509Certificate x509 = readCertificate(certificate.sslCertPath());
        final long days = Duration.between(x509.getNotBefore().toInstant(),
                                           x509.getNotAfter().toInstant())
                                  .toDays();
        assertTrue(days >= validityDays - 1 && days <= validityDays,
                   "certificate validity should be ~" + validityDays + " days, was " + days);
    }

    @Test
    public void test_custom_subject_is_applied(final TestInfo testInfo) throws Exception {
        final SslCertificate certificate = SslUtil.createSslCertificate(testFolder(testInfo), 
                                                                        "/C=FR/O=EasyAPI/CN=easygoingapi.com",
                                                                        30);

        final X509Certificate x509 = readCertificate(certificate.sslCertPath());
        final String subject = x509.getSubjectX500Principal().getName();
        assertTrue(subject.contains("CN=easygoingapi.com"), "unexpected subject: " + subject);
        assertTrue(subject.contains("O=EasyAPI"), "unexpected subject: " + subject);
    }

    @Test
    public void test_fails_when_destination_is_a_file(final TestInfo testInfo) throws Exception {
        // A regular file where a directory is expected: folder creation fails,
        // which must surface as a YojaAppException rather than a silent return.
        final Path file = testFolder(testInfo).resolve("not-a-folder");
        Files.writeString(file, "x");

        assertThrows(YojaAppException.class,
                     () -> SslUtil.createSslCertificate(file));
    }

    @Test
    public void test_big_validity_days_are_applied(final TestInfo testInfo) throws Exception {
        final int validityDays = 365000;
        final SslCertificate certificate = SslUtil.createSslCertificate(testFolder(testInfo), 
                                                                        "/CN=localhost", 
                                                                        validityDays);

        final X509Certificate x509 = readCertificate(certificate.sslCertPath());
        final long days = Duration.between(x509.getNotBefore().toInstant(),
                                           x509.getNotAfter().toInstant())
                                  .toDays();
        assertTrue(days >= validityDays - 1 && days <= validityDays,
                   "certificate validity should be ~" + validityDays + " days, was " + days);
    }

}
