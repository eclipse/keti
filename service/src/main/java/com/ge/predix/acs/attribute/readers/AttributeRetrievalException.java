package com.ge.predix.acs.attribute.readers;

import java.text.MessageFormat;

public class AttributeRetrievalException extends RuntimeException {

    public AttributeRetrievalException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public AttributeRetrievalException(final String message) {
        super(message);
    }

    static String getAdapterErrorMessage(final String adapterEndpoint) {
        return MessageFormat.format("Couldn''t get attributes from the adapter with endpoint ''{0}''", adapterEndpoint);
    }

    static String getStorageErrorMessage(final String id) {
        return String.format("Total size of attributes or number of attributes too large for id: '%s'", id);
    }
}
