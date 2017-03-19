package com.ge.predix.acs.encryption;

@SuppressWarnings("serial")
public class EncryptionFailureException extends RuntimeException {

    public EncryptionFailureException(final Throwable cause) {
        super(cause);
    }
}
