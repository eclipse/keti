package com.ge.predix.acs.encryption;

@SuppressWarnings("serial")
public class SymmetricKeyValidationException extends RuntimeException {
    
    public SymmetricKeyValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
