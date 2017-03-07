package com.ge.predix.acs.audit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.commons.web.AcsApiUriTemplates;
import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.AuditEventProcessor;
import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.message.AuditEventV2;

@Component
@Profile("predixAudit")
public class PredixEventProcessor implements AuditEventProcessor {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PredixEventProcessor.class);

    // Remove this and use configuration eventually
    private static final Set<String> EXCLUSIONS = Collections
            .singleton(AcsApiUriTemplates.V1 + AcsApiUriTemplates.CONNECTOR_URL);

    @Autowired
    private PredixEventMapper eventMapper;

    @Autowired
    private AuditClient auditClient;

    @Override
    public boolean process(final AuditEvent auditEvent) {
        if (null == this.auditClient || isExclusion(auditEvent)) {
            return false;
        }
        AuditEventV2 predixEvent = this.eventMapper.map(auditEvent);
        try {
            this.auditClient.audit(predixEvent);
        } catch (Exception e) {
            LOGGER.warn("Audit failed on process with event:" + predixEvent.toString(), e);
            return false;
        }
        return true;
    }

    boolean isExclusion(final AuditEvent auditEvent) {
        URI requestUri;
        try {
            requestUri = new URI(auditEvent.getRequestUri());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Request has malformed URI", e);
        }
        for (String exclusion : EXCLUSIONS) {
            if (requestUri.normalize().getPath().startsWith(exclusion)) {
                return true;
            }
        }
        return false;
    }

}