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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Strings;

/**
 * A mail sent by {@link MailService}: its sender, its recipients (to, cc,
 * bcc), its title, its body -- plain text, HTML, or both (the reader shows the
 * HTML, the plain text being its fallback) -- and its attached files.
 */
public class Mail {

    private String from;
    private Set<String> recipients;
    private Set<String> ccRecipients;
    private Set<String> bccRecipients;
    private String title;
    private String message;
    private String html;
    private Set<Path> attachFiles;

    /** Creates an empty mail; use {@link #builder(String)}. */
    private Mail() {
    }

    /**
     * Returns the sender's address.
     *
     * @return the sender's address
     */
    public String from() {
        return from;
    }

    /**
     * Returns the recipients' addresses ({@code To}).
     *
     * @return the recipients' addresses
     */
    public Set<String> recipients() {
        return recipients;
    }

    /**
     * Returns the copy recipients' addresses ({@code Cc}).
     *
     * @return the copy recipients' addresses
     */
    public Set<String> ccRecipients() {
        return ccRecipients;
    }

    /**
     * Returns the hidden copy recipients' addresses ({@code Bcc}).
     *
     * @return the hidden copy recipients' addresses
     */
    public Set<String> bccRecipients() {
        return bccRecipients;
    }

    /**
     * Returns the title (subject).
     *
     * @return the title, or {@code null}
     */
    public String title() {
        return title;
    }

    /**
     * Returns the plain text body.
     *
     * @return the plain text body, or {@code null}
     */
    public String message() {
        return message;
    }

    /**
     * Returns the HTML body.
     *
     * @return the HTML body, or {@code null}
     */
    public String html() {
        return html;
    }

    /**
     * Returns the attached files.
     *
     * @return the attached files
     */
    public Set<Path> attachFiles() {
        return attachFiles;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(Mail.class.getSimpleName());
        result.append(" [from=");
        result.append(from);
        result.append(", recipients=");
        result.append(recipients);
        if (!ccRecipients.isEmpty()) {
            result.append(", ccRecipients=");
            result.append(ccRecipients);
        }
        if (!bccRecipients.isEmpty()) {
            result.append(", bccRecipients=");
            result.append(bccRecipients);
        }
        if (!Strings.isNullOrEmpty(title)) {
            result.append(", title=");
            result.append(title);
        }
        if (!Strings.isNullOrEmpty(message)) {
            result.append(", message=");
            result.append(message);
        }
        if (!Strings.isNullOrEmpty(html)) {
            result.append(", html=");
            result.append(html);
        }
        result.append(", attachFiles=");
        result.append(attachFiles);
        result.append("]");
        return result.toString();
    }

    /*
     * 
     * BUILDER
     * 
     */
    /**
     * Creates a new {@link Builder}.
     *
     * @param from the sender's address
     * @return a new builder
     */
    public static Builder builder(final String from) {
        return new Builder(from);
    }

    /** Builder of a {@link Mail}. */
    public static class Builder {

        private final Mail mail = new Mail();

        /**
         * Creates a builder; use {@link Mail#builder(String)}.
         *
         * @param from the sender's address
         */
        private Builder(final String from) {
            super();
            Objects.requireNonNull(from, "need email");
            mail.recipients = new LinkedHashSet<>();
            mail.ccRecipients = new LinkedHashSet<>();
            mail.bccRecipients = new LinkedHashSet<>();
            mail.attachFiles = new LinkedHashSet<>();
            mail.from = from;
        }

        /**
         * Adds a recipient ({@code To}).
         *
         * @param recipient the recipient's address
         * @return this builder
         */
        public Builder addRecipient(final String recipient) {
            mail.recipients.add(Objects.requireNonNull(recipient, "recipient"));
            return this;
        }

        /**
         * Adds a copy recipient ({@code Cc}).
         *
         * @param recipient the recipient's address
         * @return this builder
         */
        public Builder addCc(final String recipient) {
            mail.ccRecipients.add(Objects.requireNonNull(recipient, "recipient"));
            return this;
        }

        /**
         * Adds a hidden copy recipient ({@code Bcc}): the other recipients
         * don't see it.
         *
         * @param recipient the recipient's address
         * @return this builder
         */
        public Builder addBcc(final String recipient) {
            mail.bccRecipients.add(Objects.requireNonNull(recipient, "recipient"));
            return this;
        }

        /**
         * Sets the title (subject).
         *
         * @param title the title
         * @return this builder
         */
        public Builder title(final String title) {
            mail.title = title;
            return this;
        }

        /**
         * Sets the plain text body.
         *
         * @param message the plain text body
         * @return this builder
         */
        public Builder message(final String message) {
            mail.message = message;
            return this;
        }

        /**
         * Sets the plain text body.
         *
         * @param messsage the plain text body
         * @return this builder
         * @deprecated misspelled: use {@link #message(String)}
         */
        @Deprecated
        public Builder messsage(final String messsage) {
            return message(messsage);
        }

        /**
         * Sets the HTML body. With a plain text body too, the reader shows the
         * HTML, the plain text being its fallback.
         *
         * @param html the HTML body
         * @return this builder
         */
        public Builder html(final String html) {
            mail.html = html;
            return this;
        }

        /**
         * Attaches a file; its content type is guessed from its name.
         *
         * @param file the file
         * @return this builder
         * @throws MailException when the file does not exist or is a directory:
         *                       the mail would otherwise go without it
         */
        public Builder addFile(final Path file) {
            if (!Files.isRegularFile(file)) {
                throw new MailException("not a file, can't be attached: " + file);
            }
            mail.attachFiles.add(file);
            return this;
        }

        /**
         * Builds the mail.
         *
         * @return the mail
         */
        public Mail build() {
            mail.recipients = Collections.unmodifiableSet(mail.recipients);
            mail.ccRecipients = Collections.unmodifiableSet(mail.ccRecipients);
            mail.bccRecipients = Collections.unmodifiableSet(mail.bccRecipients);
            mail.attachFiles = Collections.unmodifiableSet(mail.attachFiles);
            return mail;
        }

    }

}
