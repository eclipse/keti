package com.ge.predix.acs.request.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.request.context.AcsRequestContext.ACSRequestContextAttribute;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication;

/**
 * A ThreadLocal store for the ACS Request Context.
 * 
 * 
 * @author 212408019
 *
 */
@Component
public final class AcsRequestContextHolder implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcsRequestContextHolder.class);

    // Hide Constructor
    private AcsRequestContextHolder() {
    }

    private static final InheritableThreadLocal<AcsRequestContext> ACS_REQUEST_CONTEXT_STORE = 
            new InheritableThreadLocal<AcsRequestContext>();

    // Can only be called by the filter...
    // Filter calls it to create the ACSRequestContext for a request
    static void initialize() {
        ACS_REQUEST_CONTEXT_STORE.set(new AcsRequestContextBuilder().zoneEntityOrFail().build());
    }

    // Can only be called by the filter...
    static void clear() {
        ACS_REQUEST_CONTEXT_STORE.remove();
    }

    // Only public interface to be used by the clients to access the AcsRequestContext specific to the
    // Request...
    public static AcsRequestContext getAcsRequestContext() {
        return ACS_REQUEST_CONTEXT_STORE.get();
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        LOGGER.trace("AcsRequestContextHolder Initialized");
        AcsRequestContextBuilder.initAcsRequestContextBuilderRepos(applicationContext);
    }

    /**
     * 
     * The Fluent implementation to create the {@link AcsRequestContext} for the current AcsRequest...
     * 
     * @author 212408019
     *
     */
    private static final class AcsRequestContextBuilder {
        private static ZoneRepository zoneRepository;
        private static final Logger LOGGER = LoggerFactory.getLogger(AcsRequestContextBuilder.class);

        static void initAcsRequestContextBuilderRepos(final ApplicationContext applicationContext) {
            zoneRepository = applicationContext.getBean(ZoneRepository.class);
            LOGGER.info("AcsRequestContextBuilder JPA Repositories Initialized");
        }

        private Map<ACSRequestContextAttribute, Object> requestContextMap;

        AcsRequestContextBuilder() {
            this.requestContextMap = new HashMap<ACSRequestContextAttribute, Object>();
        }

        AcsRequestContextBuilder zoneEntityOrFail() {
            ZoneOAuth2Authentication zoneAuth = (ZoneOAuth2Authentication) SecurityContextHolder.getContext()
                    .getAuthentication();
            this.requestContextMap.put(ACSRequestContextAttribute.ZONE_ENTITY,
                    zoneRepository.getBySubdomain(zoneAuth.getZoneId()));
            return this;
        }

        AcsRequestContext build() {
            return new AcsRequestContext(Collections.unmodifiableMap(this.requestContextMap));
        }
    }
}
