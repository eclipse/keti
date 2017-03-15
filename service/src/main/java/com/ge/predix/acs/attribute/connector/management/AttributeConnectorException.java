package com.ge.predix.acs.attribute.connector.management;

public class AttributeConnectorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AttributeConnectorException() {
        super();
    }

    public AttributeConnectorException(final String message) {
        super(message);
    }

    public AttributeConnectorException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
