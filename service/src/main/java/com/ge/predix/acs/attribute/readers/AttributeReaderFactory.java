package com.ge.predix.acs.attribute.readers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.ge.predix.acs.attribute.cache.AttributeCacheFactory;
import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.zone.resolver.SpringSecurityZoneResolver;

// CHECKSTYLE:OFF: FinalClass
@Component
public class AttributeReaderFactory {

    // Temporary - this will eventually come from some zone level configuration
    private long maxCacheMegaBytes = 100;

    @Value("${ADAPTER_TIMEOUT_MILLIS:3000}")
    private int adapterTimeoutMillis;

    @Autowired
    private AttributeConnectorService connectorService;

    @Autowired
    private PrivilegeServiceResourceAttributeReader privilegeServiceResourceAttributeReader;

    @Autowired
    private PrivilegeServiceSubjectAttributeReader privilegeServiceSubjectAttributeReader;

    // Caches that use the multiton design pattern (keyed off the zone name)
    private final Map<String, ExternalResourceAttributeReader> externalResourceAttributeReaderCache = new ConcurrentReferenceHashMap<>();
    private final Map<String, ExternalSubjectAttributeReader> externalSubjectAttributeReaderCache = new ConcurrentReferenceHashMap<>();

    public ResourceAttributeReader getResourceAttributeReader() {
        if (!connectorService.isResourceAttributeConnectorConfigured()) {
            return this.privilegeServiceResourceAttributeReader;
        }

        String zoneName = SpringSecurityZoneResolver.getZoneName();
        ExternalResourceAttributeReader externalResourceAttributeReader = this.externalResourceAttributeReaderCache
                .get(zoneName);
        if (externalResourceAttributeReader != null) {
            return externalResourceAttributeReader;
        }

        externalResourceAttributeReader = new ExternalResourceAttributeReader(connectorService,
                AttributeCacheFactory.createResourceAttributeCache(this.maxCacheMegaBytes), adapterTimeoutMillis);
        this.externalResourceAttributeReaderCache.put(zoneName, externalResourceAttributeReader);
        return externalResourceAttributeReader;
    }

    public SubjectAttributeReader getSubjectAttributeReader() {
        if (!connectorService.isSubjectAttributeConnectorConfigured()) {
            return this.privilegeServiceSubjectAttributeReader;
        }

        String zoneName = SpringSecurityZoneResolver.getZoneName();
        ExternalSubjectAttributeReader externalSubjectAttributeReader = this.externalSubjectAttributeReaderCache
                .get(zoneName);
        if (externalSubjectAttributeReader != null) {
            return externalSubjectAttributeReader;
        }

        externalSubjectAttributeReader = new ExternalSubjectAttributeReader(connectorService,
                AttributeCacheFactory.createSubjectAttributeCache(this.maxCacheMegaBytes), adapterTimeoutMillis);
        this.externalSubjectAttributeReaderCache.put(zoneName, externalSubjectAttributeReader);
        return externalSubjectAttributeReader;
    }

    public void removeResourceReader(final String zoneName) {
        this.externalResourceAttributeReaderCache.remove(zoneName);
    }

    public void removeSubjectReader(final String zoneName) {
        this.externalSubjectAttributeReaderCache.remove(zoneName);
    }
}
// CHECKSTYLE:ON: FinalClass
