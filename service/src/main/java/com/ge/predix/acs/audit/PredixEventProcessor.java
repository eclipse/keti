package com.ge.predix.acs.audit;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.AuditEventProcessor;
import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEventV2;

public class PredixEventProcessor implements AuditEventProcessor {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PredixEventProcessor.class);

    @Autowired
    private AuditClient auditClient;

    @Autowired
    private PredixEventMapper eventMapper;

    @Override
    public boolean process(final AuditEvent auditEvent) {
        AuditEventV2 predixEvent = eventMapper.map(auditEvent);
        try {
            this.auditClient.audit(predixEvent);
        } catch (AuditException e) {
            LOGGER.warn("Audit failed on process with event: " + predixEvent.toString());
            return false;
        }
        return true;
    }

}