package com.ge.predix.acs.encryption;

@SuppressWarnings("serial")
public class EncryptionFailureException extends RuntimeException {

    public EncryptionFailureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
