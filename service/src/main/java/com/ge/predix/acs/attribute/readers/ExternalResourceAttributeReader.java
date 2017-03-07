package com.ge.predix.acs.attribute.readers;

import com.ge.predix.acs.attribute.cache.AttributeCache;

public class ExternalResourceAttributeReader extends ExternalAttributeReader implements ResourceAttributeReader {

    public ExternalResourceAttributeReader(final AttributeCache resourceAttributeCache) {
        super(resourceAttributeCache);
    }
}
