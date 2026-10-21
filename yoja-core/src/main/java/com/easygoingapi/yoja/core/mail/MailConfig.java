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
package com.easygoingapi.yoja.core.mail;

import java.time.Duration;
import java.util.Objects;

/**
 * Connection settings of the SMTP server a {@link MailService} sends with.
 * <p>
 * Secure by default: {@link Security#STARTTLS} on port {@value #DEFAULT_PORT},
 * required -- a server refusing TLS is never spoken to in clear. A
 * {@link #userName()} turns the login on.
 */
public class MailConfig {

    /** Default SMTP port: the submission port, with STARTTLS. */
    public static final int DEFAULT_PORT = 587;
    /** Default timeout to connect to the server, and of an idle connection. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /** How the connection to the SMTP server is secured. */
    public enum Security {
        /** Plain connection: a local relay, a test server. */
        NONE,
        /** Plain connection upgraded to TLS, required (port 587). */
        STARTTLS,
        /** TLS from the start (port 465). */
        SSL
    }

    private final String host;
    private final int port;
    private final Security security;
    private final String userName;
    private final String password;
    private final Duration timeout;

    /**
     * Creates the settings of a server reached with {@link Security#STARTTLS}
     * on port {@value #DEFAULT_PORT}, with a login.
     *
     * @param host     SMTP server host
     * @param userName login
     * @param password password
     */
    public MailConfig(final String host,
                      final String userName,
                      final String password) {
        this(builder(host).login(userName, password));
    }

    private MailConfig(final Builder builder) {
        super();
        this.host = builder.host;
        this.port = builder.port;
        this.security = builder.security;
        this.userName = builder.userName;
        this.password = builder.password;
        this.timeout = builder.timeout;
    }

    /**
     * Returns the SMTP server host.
     *
     * @return the SMTP server host
     */
    public String host() {
        return host;
    }

    /**
     * Returns the SMTP server port.
     *
     * @return the SMTP server port
     */
    public int port() {
        return port;
    }

    /**
     * Returns how the connection is secured.
     *
     * @return how the connection is secured
     */
    public Security security() {
        return security;
    }

    /**
     * Returns the login.
     *
     * @return the login, or {@code null} when the server is used without one
     */
    public String userName() {
        return userName;
    }

    /**
     * Returns the password.
     *
     * @return the password, or {@code null}
     */
    public String password() {
        return password;
    }

    /**
     * Returns the timeout to connect to the server, and of an idle connection.
     *
     * @return the timeout
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Converts the settings to the Vert.x mail client's.
     *
     * @return the Vert.x mail client settings
     */
    io.vertx.ext.mail.MailConfig toVertx() {
        final io.vertx.ext.mail.MailConfig result = new io.vertx.ext.mail.MailConfig();
        result.setHostname(host);
        result.setPort(port);
        result.setConnectTimeout((int) timeout.toMillis());
        result.setIdleTimeout((int) timeout.toSeconds());
        switch (security) {
            case NONE -> result.setStarttls(io.vertx.ext.mail.StartTLSOptions.DISABLED);
            case STARTTLS -> result.setStarttls(io.vertx.ext.mail.StartTLSOptions.REQUIRED);
            case SSL -> {
                result.setSsl(true);
                result.setStarttls(io.vertx.ext.mail.StartTLSOptions.DISABLED);
            }
        }
        if (userName == null) {
            result.setLogin(io.vertx.ext.mail.LoginOption.DISABLED);
        }
        else {
            result.setLogin(io.vertx.ext.mail.LoginOption.REQUIRED);
            result.setUsername(userName);
            result.setPassword(password);
        }
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(MailConfig.class.getSimpleName());
        result.append(" [host=");
        result.append(host);
        result.append(", port=");
        result.append(port);
        result.append(", security=");
        result.append(security);
        if (userName != null) {
            result.append(", userName=");
            result.append(userName);
            // the password is never written: a config ends up in the logs
            result.append(", password=***");
        }
        result.append(", timeout=");
        result.append(timeout);
        result.append("]");
        return result.toString();
    }

    /*
     * 
     * BUILDER
     * 
     */
    /**
     * Creates a new {@link Builder}, with secure defaults:
     * {@link Security#STARTTLS} on port {@value #DEFAULT_PORT}, no login and a
     * timeout of {@link #DEFAULT_TIMEOUT}.
     *
     * @param host SMTP server host
     * @return a new builder
     */
    public static Builder builder(final String host) {
        return new Builder(host);
    }

    /** Builder of a {@link MailConfig}. */
    public static class Builder {

        private final String host;
        private int port = DEFAULT_PORT;
        private Security security = Security.STARTTLS;
        private String userName;
        private String password;
        private Duration timeout = DEFAULT_TIMEOUT;

        /**
         * Creates a builder; use {@link MailConfig#builder(String)}.
         *
         * @param host SMTP server host
         */
        private Builder(final String host) {
            this.host = Objects.requireNonNull(host, "host");
        }

        /**
         * Sets the SMTP server port (default {@value MailConfig#DEFAULT_PORT}).
         *
         * @param port the port
         * @return this builder
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets how the connection is secured (default {@link Security#STARTTLS}).
         *
         * @param security the security
         * @return this builder
         */
        public Builder security(final Security security) {
            this.security = Objects.requireNonNull(security, "security");
            return this;
        }

        /**
         * Sets the login the server requires.
         *
         * @param userName the login
         * @param password the password
         * @return this builder
         */
        public Builder login(final String userName,
                             final String password) {
            this.userName = userName;
            this.password = password;
            return this;
        }

        /**
         * Sets the timeout to connect to the server, and of an idle connection
         * (default {@link MailConfig#DEFAULT_TIMEOUT}).
         *
         * @param timeout the timeout
         * @return this builder
         */
        public Builder timeout(final Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Builds the settings.
         *
         * @return the settings
         */
        public MailConfig build() {
            return new MailConfig(this);
        }

    }

}
