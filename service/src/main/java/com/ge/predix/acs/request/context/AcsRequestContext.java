package com.ge.predix.acs.request.context;

import java.util.Map;

public class AcsRequestContext {

    private final Map<ACSRequestContextAttribute, Object> unModifiableRequestContextMap;

    public enum ACSRequestContextAttribute {
        ZONE_ENTITY;
    }

    // Hide Constructor
    AcsRequestContext(final Map<ACSRequestContextAttribute, Object> unModifiableRequestContextMap) {
        this.unModifiableRequestContextMap = unModifiableRequestContextMap;
    }

    public Object get(final ACSRequestContextAttribute acsRequestContextEnum) {
        return this.unModifiableRequestContextMap.get(acsRequestContextEnum);
    }

}
