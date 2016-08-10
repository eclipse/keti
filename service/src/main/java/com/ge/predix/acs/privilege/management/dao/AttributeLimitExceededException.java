package com.ge.predix.acs.privilege.management.dao;

public class AttributeLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = -9111322155771470957L;

    public AttributeLimitExceededException() {
        super();
    }

    public AttributeLimitExceededException(final String message) {
        super(message);
    }
}
