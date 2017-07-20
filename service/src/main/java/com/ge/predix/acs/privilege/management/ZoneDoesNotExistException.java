package com.ge.predix.acs.privilege.management;

public class ZoneDoesNotExistException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ZoneDoesNotExistException(final String message) {
        super(message);
    }
}