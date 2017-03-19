package com.ge.predix.acs.encryption;

@SuppressWarnings("serial")
public class DecryptionFailureException extends RuntimeException {

    public DecryptionFailureException(final Throwable cause) {
        super(cause);
    }
}
