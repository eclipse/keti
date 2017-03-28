package com.ge.predix.acs.encryption;

@SuppressWarnings("serial")
public class DecryptionFailureException extends RuntimeException {

    public DecryptionFailureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
