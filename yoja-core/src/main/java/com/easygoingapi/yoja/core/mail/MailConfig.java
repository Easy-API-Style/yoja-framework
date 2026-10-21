package com.easygoingapi.yoja.core.mail;

import java.util.Properties;

public class MailConfig {

    private final String mailSmtpHost;
    private final String userName;
    private final String password;
    
    public MailConfig(final String mailSmtpHost, 
                      final String userName, 
                      final String password) {
        super();
        this.mailSmtpHost = mailSmtpHost;
        this.userName = userName;
        this.password = password;
    }

    public String userName() {
        return userName;
    }

    public String password() {
        return password;
    }

    public Properties properties() {
        final Properties mailProperties = new Properties();
        mailProperties.put("mail.smtp.auth", true);
        mailProperties.put("mail.smtp.starttls.enable", true);
        mailProperties.put("mail.smtp.ssl.trust", mailSmtpHost);
        mailProperties.put("mail.smtp.host", mailSmtpHost);
        mailProperties.put("mail.smtp.port", "587");
        return mailProperties;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(MailConfig.class.getSimpleName());
        result.append(" [");
        if (mailSmtpHost != null) {
            result.append("mailSmtpHost=");
            result.append(mailSmtpHost);
            result.append(", ");
        }
        if (userName != null) {
            result.append("userName=");
            result.append(userName);
            result.append(", ");
        }
        if (password != null) {
            result.append("password=");
            result.append(password);
        }
        result.append("]");
        return result.toString();
    }

}
