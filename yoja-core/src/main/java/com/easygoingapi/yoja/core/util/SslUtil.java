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
package com.easygoingapi.yoja.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import com.easygoingapi.yoja.core.YojaAppException;

/**
 * Generates a self-signed X.509 certificate and its private key by shelling
 * out to the {@code openssl} command-line tool.
 * <p>
 * This is the replacement for the deprecated
 * {@code io.vertx.core.net.SelfSignedCertificate}: the produced PEM files can
 * be fed to a {@code PemKeyCertOptions} (key / cert paths). It relies on
 * {@code openssl} being installed and available on the {@code PATH}.
 */
public class SslUtil {

    /** File name of the generated private key. */
    private static final String KEY_FILE_NAME = "ssl-key.pem";
    /** File name of the generated certificate. */
    private static final String CERT_FILE_NAME = "ssl-cert.pem";

    /** Subject of the generated certificate. */
    private static final String SUBJECT = "/CN=localhost";
    /** Validity of the generated certificate, in days. */
    private static final int VALIDITY_DAYS = 3650;
    /** How long to wait for {@code openssl} to complete before giving up. */
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /** Not instantiable. */
    private SslUtil() {}

    /**
     * Paths to the generated private key and certificate.
     *
     * @param sslKeyPath  path to the PEM private key
     * @param sslCertPath path to the PEM certificate
     */
    public static record SslCertificate(Path sslKeyPath, Path sslCertPath) {}

    /**
     * Generates a self-signed certificate with the default subject
     * ({@value #SUBJECT}) and validity ({@value #VALIDITY_DAYS} days).
     *
     * @param destinationFolder folder the key and certificate are written to
     * @return the paths to the generated key and certificate
     * @throws YojaAppException when the folder cannot be created, or when
     *                          {@code openssl} is missing or exits with a
     *                          non-zero status
     */
    public static SslCertificate createSslCertificate(final Path destinationFolder) {
        return createSslCertificate(destinationFolder, SUBJECT, VALIDITY_DAYS);
    }

    /**
     * Generates a self-signed certificate and its private key in
     * {@code destinationFolder}, creating the folder if needed.
     *
     * @param destinationFolder folder the {@code ssl-key.pem} and
     *                          {@code ssl-cert.pem} files are written to
     * @param subject           certificate subject as an openssl DN, e.g.
     *                          {@code "/CN=localhost"} or {@code "/O=EasyAPI/CN=example.com"}
     * @param validityDays      number of days the certificate stays valid
     * @return the paths to the generated key and certificate
     * @throws YojaAppException when the folder cannot be created, or when
     *                          {@code openssl} is missing or exits with a
     *                          non-zero status
     */
    public static SslCertificate createSslCertificate(final Path destinationFolder,
                                                      final String subject,
                                                      final int validityDays) {
        final Path keyPath = destinationFolder.resolve(KEY_FILE_NAME);
        final Path certPath = destinationFolder.resolve(CERT_FILE_NAME);
        try {
            Files.createDirectories(destinationFolder);
        }
        catch (final IOException e) {
            throw new YojaAppException("create ssl folder failed: " + destinationFolder, e);
        }

        // openssl req -x509 -newkey rsa:2048 -sha256 -days N -nodes
        //             -keyout <key> -out <cert> -subj "/CN=localhost"
        final List<String> command = List.of("openssl", "req",
                                              "-x509",
                                              "-newkey", "rsa:2048",
                                              "-sha256",
                                              "-days", String.valueOf(validityDays),
                                              "-nodes",
                                              "-keyout", keyPath.toString(),
                                              "-out", certPath.toString(),
                                              "-subj", subject);
        final StringBuilder output = new StringBuilder();
        final Consumer<String> sink = line -> output.append(line).append(System.lineSeparator());
        final int exitCode = ProcessUtil.execute(command, sink, sink, TIMEOUT);
        if (exitCode != 0) {
            throw new YojaAppException("openssl failed (exit " + exitCode + "): " + output);
        }
        return new SslCertificate(keyPath, certPath);
    }

}
