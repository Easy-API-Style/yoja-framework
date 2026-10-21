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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;

/**
 * Sends {@link Mail}s over SMTP with the Vert.x mail client: non-blocking, so
 * it can be called from the event loop as well as from a worker.
 * <p>
 * The connections to the server are pooled and reused from one mail to the
 * next; {@link #close()} releases them.
 */
public class MailService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    /** Content type of an attached file whose type can't be guessed from its name. */
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MailConfig mailConfig;
    private final Vertx vertx;
    private final MailClient mailClient;

    /**
     * Creates a mail service sending with the given SMTP server.
     *
     * @param mailConfig the SMTP server settings
     */
    public MailService(final MailConfig mailConfig) {
        super();
        this.mailConfig = mailConfig;
        this.vertx = YojaApp.vertx();
        this.mailClient = MailClient.create(vertx, mailConfig.toVertx());
    }

    /**
     * Returns the SMTP server settings.
     *
     * @return the SMTP server settings
     */
    public MailConfig mailConfig() {
        return mailConfig;
    }

    /**
     * Sends a mail.
     *
     * @param mail the mail
     * @return a future resolving to the id the server gave the mail
     *         ({@code Message-ID}), failed with a {@link MailException} when the
     *         mail has no recipient, an attached file can't be read or the
     *         server refuses the mail
     */
    public Future<String> send(final Mail mail) {
        if (mail.recipients().isEmpty()
                && mail.ccRecipients().isEmpty()
                && mail.bccRecipients().isEmpty()) {
            return Future.failedFuture(new MailException("no recipient: " + mail));
        }
        return attachmentsOf(mail).compose(attachments -> mailClient.sendMail(toMessage(mail, attachments)))
                                  .map(mailResult -> {
                                      LOGGER.info("mail sent to {} from {}: {}",
                                                  mail.recipients(), mail.from(), mail.title());
                                      return mailResult.getMessageID();
                                  })
                                  .recover(e -> Future.failedFuture(e instanceof MailException
                                          ? e
                                          : new MailException("sending the mail failed: " + mail, e)));
    }

    /**
     * Releases the connections to the server; the service must not be used
     * after this call.
     *
     * @return a future completing when the connections are closed
     */
    public Future<Void> shutdown() {
        return mailClient.close();
    }

    /** Releases the connections to the server, without waiting for it; see {@link #shutdown()}. */
    @Override
    public void close() {
        shutdown();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(MailService.class.getSimpleName());
        result.append(" [mailConfig=");
        result.append(mailConfig);
        result.append("]");
        return result.toString();
    }

    /**
     * Reads the attached files of a mail, without blocking.
     *
     * @param mail the mail
     * @return a future resolving to its attachments
     */
    private Future<List<MailAttachment>> attachmentsOf(final Mail mail) {
        final List<Future<MailAttachment>> futures = new ArrayList<>();
        for (final Path file : mail.attachFiles()) {
            futures.add(vertx.fileSystem()
                             .readFile(file.toString())
                             .map(data -> MailAttachment.create()
                                                        .setName(file.getFileName().toString())
                                                        .setContentType(contentTypeOf(file))
                                                        .setData(data))
                             .recover(e -> Future.failedFuture(new MailException("can't read the attached file "
                                                                                 + file, e))));
        }
        return Future.all(futures)
                     .map(v -> futures.stream()
                                      .map(Future::result)
                                      .toList());
    }

    /**
     * Converts a mail to the Vert.x mail client's message.
     *
     * @param mail        the mail
     * @param attachments its attached files, read
     * @return the Vert.x mail message
     */
    private static MailMessage toMessage(final Mail mail,
                                         final List<MailAttachment> attachments) {
        final MailMessage result = new MailMessage();
        result.setFrom(mail.from());
        if (!mail.recipients().isEmpty()) {
            result.setTo(List.copyOf(mail.recipients()));
        }
        if (!mail.ccRecipients().isEmpty()) {
            result.setCc(List.copyOf(mail.ccRecipients()));
        }
        if (!mail.bccRecipients().isEmpty()) {
            result.setBcc(List.copyOf(mail.bccRecipients()));
        }
        result.setSubject(mail.title());
        result.setText(mail.message());
        result.setHtml(mail.html());
        if (!attachments.isEmpty()) {
            result.setAttachment(attachments);
        }
        return result;
    }

    /**
     * Guesses the content type of a file from its name.
     *
     * @param file the file
     * @return its content type, or {@value #DEFAULT_CONTENT_TYPE}
     */
    private static String contentTypeOf(final Path file) {
        String result;
        try {
            result = Files.probeContentType(file);
        }
        catch (final IOException e) {
            result = null;
        }
        return result != null ? result : DEFAULT_CONTENT_TYPE;
    }

}
