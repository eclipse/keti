package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication;

public class ExternalAttributeReader implements AttributeReader {

    private final AttributeCache attributeCache;

    public ExternalAttributeReader(final AttributeCache attributeCache) {
        this.attributeCache = attributeCache;
    }

    public Set<Attribute> getAttributes(final String identifier) {
        Set<Attribute> attributes;

        if (this.attributeCache == null) {
            attributes = this.getAttributesFromAdapters(identifier);
        } else {
            attributes = this.attributeCache.getAttributes(identifier);
            if (CollectionUtils.isEmpty(attributes)) {
                attributes = this.getAttributesFromAdapters(identifier);
                this.attributeCache.setAttributes(identifier, attributes);

                // TODO: Reset the TTL for the policy evaluation cache key concerning this resource to:
                // total customer TTL - this.attributeCache.getConfiguredTtlInSeconds()
            }
        }

        return attributes;
    }

    Set<Attribute> getAttributesFromAdapters(final String identifier) {
        // TODO: Add an inline call to the Asset Adapter based on configuration (call this.getZoneId() to get the zone)
        return Collections.emptySet();
    }

    String getZoneId() {
        return ((ZoneOAuth2Authentication) SecurityContextHolder.getContext().getAuthentication()).getZoneId();
    }
}
