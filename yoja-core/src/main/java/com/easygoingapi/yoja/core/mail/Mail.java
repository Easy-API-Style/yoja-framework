package com.easygoingapi.yoja.core.mail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Strings;

public class Mail {

    private String from;
    private Set<String> recipients;
    private String title;
    private String message;
    private Set<Path> attachFiles;
    
    public String from() {
        return from;
    }

    public Set<String> recipients() {
        return recipients;
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }
    
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
        if (!Strings.isNullOrEmpty(title)) {
            result.append(", title=");
            result.append(title);
        }
        if (!Strings.isNullOrEmpty(message)) {
            result.append(", messsage=");
            result.append(message);
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
    public static Builder builder(final String from) {
        return new Builder(from);
    }
    
    public static class Builder {
        
        private final Mail mail = new Mail();
        
        private Builder(final String from) {
            super();
            Objects.requireNonNull(from, "need email");
            mail.recipients = new HashSet<>();
            mail.attachFiles = new HashSet<>();
            mail.from = from;
        }

        public Builder addRecipient(final String recipient) {
            mail.recipients.add(recipient);
            return this;
        }

        public Builder title(final String title) {
            mail.title = title;
            return this;
        }

        public Builder messsage(final String messsage) {
            mail.message = messsage;
            return this;
        }
        
        public Builder addFile(final Path file) {
            if (!Files.isDirectory(file) 
                    && Files.exists(file)) {
                mail.attachFiles.add(file);
            }
            return this;
        }
        
        public Mail build() {
            return mail;
        }
        
    }
    
}
