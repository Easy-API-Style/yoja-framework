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

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.easygoingapi.yoja.core.mail.Mail;
import com.easygoingapi.yoja.core.mail.MailConfig;
import com.easygoingapi.yoja.core.mail.MailConfig.Security;
import com.easygoingapi.yoja.core.mail.MailException;
import com.easygoingapi.yoja.core.mail.MailService;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import io.vertx.core.Future;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

public class TestMailService {

    private GreenMail greenMail;

    @BeforeEach
    public void startSmtpServer() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterEach
    public void stopSmtpServer() {
        greenMail.stop();
    }

    /** The test SMTP server: plain connection, without login. */
    private static MailConfig.Builder localServer() {
        return MailConfig.builder("localhost")
                         .port(ServerSetupTest.SMTP.getPort())
                         .security(Security.NONE);
    }

    @Test
    public void test_send(@TempDir final Path dir) throws Exception {
        final Path file = Files.writeString(dir.resolve("report.txt"), "the report");
        final Mail mail = Mail.builder("seller@localhost")
                              .addRecipient("customer@localhost")
                              .addCc("accountant@localhost")
                              .addBcc("archive@localhost")
                              .title("Your order été")
                              .message("plain body")
                              .html("<p>html body</p>")
                              .addFile(file)
                              .build();
        try (final MailService mailService = new MailService(localServer().build())) {
            final String messageId = awaitValue(mailService.send(mail));
            assertNotNull(messageId);
        }
        assertTrue(greenMail.waitForIncomingEmail(5000, 3));
        final MimeMessage[] messages = greenMail.getReceivedMessages();
        // one copy per recipient, the same message
        assertEquals(3, messages.length);
        final MimeMessage message = messages[0];
        assertEquals("seller@localhost", message.getFrom()[0].toString());
        assertEquals("customer@localhost", message.getRecipients(RecipientType.TO)[0].toString());
        assertEquals("accountant@localhost", message.getRecipients(RecipientType.CC)[0].toString());
        // Bcc: delivered, but not written in the mail
        assertEquals(null, message.getRecipients(RecipientType.BCC));
        assertEquals("Your order été", message.getSubject());

        final String content = GreenMailUtil.getWholeMessage(message);
        assertTrue(content.contains("plain body"), content);
        assertTrue(content.contains("<p>html body</p>"), content);
        final Part attachment = findAttachment((Multipart) message.getContent());
        assertNotNull(attachment, content);
        assertEquals("report.txt", attachment.getFileName());
        assertTrue(attachment.getContentType().startsWith("text/plain"), attachment.getContentType());
        assertEquals("the report", new String(attachment.getInputStream().readAllBytes()));
    }

    @Test
    public void test_login() {
        greenMail.setUser("seller@localhost", "seller", "secret");
        final Mail mail = Mail.builder("seller@localhost")
                              .addRecipient("customer@localhost")
                              .title("with login")
                              .message("body")
                              .build();
        try (final MailService mailService = new MailService(localServer().login("seller", "secret").build())) {
            assertNotNull(awaitValue(mailService.send(mail)));
        }
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        try (final MailService mailService = new MailService(localServer().login("seller", "wrong").build())) {
            final Future<String> future = mailService.send(mail);
            await(future);
            assertTrue(future.failed());
            assertInstanceOf(MailException.class, future.cause());
        }
    }

    @Test
    public void test_invalid() {
        try (final MailService mailService = new MailService(localServer().build())) {
            final Future<String> future = mailService.send(Mail.builder("seller@localhost")
                                                               .title("nobody")
                                                               .build());
            await(future);
            assertTrue(future.failed());
            assertInstanceOf(MailException.class, future.cause());
        }
        assertThrows(MailException.class, () -> Mail.builder("seller@localhost")
                                                    .addFile(Path.of("/no/such/file.pdf")));
        assertEquals(0, greenMail.getReceivedMessages().length);
    }

    @Test
    public void test_server_down() {
        greenMail.stop();
        try (final MailService mailService = new MailService(localServer().build())) {
            final Future<String> future = mailService.send(Mail.builder("seller@localhost")
                                                               .addRecipient("customer@localhost")
                                                               .build());
            await(future);
            assertTrue(future.failed());
            assertInstanceOf(MailException.class, future.cause());
        }
    }

    @Test
    public void test_config() {
        final MailConfig mailConfig = new MailConfig("smtp.example.com", "seller", "secret");
        assertEquals(587, mailConfig.port());
        assertEquals(Security.STARTTLS, mailConfig.security());
        assertEquals("seller", mailConfig.userName());
        // the password never ends up in the logs
        assertFalse(mailConfig.toString().contains("secret"), mailConfig.toString());
    }

    /** @return the first attached file of a multipart mail, searched in depth */
    private static Part findAttachment(final Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            final Part part = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                return part;
            }
            if (part.getContent() instanceof Multipart nested) {
                final Part result = findAttachment(nested);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

}
