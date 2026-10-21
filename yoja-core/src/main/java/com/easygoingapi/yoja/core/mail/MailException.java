package com.easygoingapi.yoja.core.mail;

public class MailException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MailException() {
        super();
    }
    
    public MailException(final String message, 
                         final Throwable cause) {
        super(message, cause);
    }

    public MailException(final String message) {
        super(message);
    }

    public MailException(final Throwable cause) {
        super(cause);
    }
    
}
