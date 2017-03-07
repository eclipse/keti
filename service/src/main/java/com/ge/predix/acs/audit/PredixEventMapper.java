package com.ge.predix.acs.audit;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.sdk.message.AuditEnums.CategoryType;
import com.ge.predix.audit.sdk.message.AuditEnums.Classifier;
import com.ge.predix.audit.sdk.message.AuditEnums.EventType;
import com.ge.predix.audit.sdk.message.AuditEnums.PublisherType;
import com.ge.predix.audit.sdk.message.AuditEventV2;
import com.ge.predix.audit.sdk.message.AuditEventV2.AuditEventV2Builder;

@Component
@Profile("predixAudit")
public class PredixEventMapper {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PredixEventProcessor.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AuditEventV2 map(final AuditEvent auditEvent) {
        Map<String, String> auditPayload = new HashMap<>();
        auditPayload.put("httpMethod", auditEvent.getMethod());
        auditPayload.put("requestBody", auditEvent.getRequestBody());
        auditPayload.put("responseBody", auditEvent.getResponseBody());

        String payload = "";
        try {
            payload = OBJECT_MAPPER.writeValueAsString(auditPayload);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to convert audit payload to json: " + auditPayload.toString());
        }

        AuditEventV2Builder auditEventBuilder = AuditEventV2.builder().timestamp(auditEvent.getTime().toEpochMilli())
                .correlationId(auditEvent.getCorrelationId()).tenantUuid(auditEvent.getZoneId())
                .publisherType(PublisherType.APP_SERVICE).categoryType(CategoryType.API_CALLS).payload(payload);

        if (isSuccessful(auditEvent)) {
            auditEventBuilder = auditEventBuilder.eventType(EventType.SUCCESS_API_REQUEST)
                    .classifier(Classifier.SUCCESS);
        } else {
            auditEventBuilder = auditEventBuilder.eventType(EventType.FAILURE_API_REQUEST)
                    .classifier(Classifier.FAILURE);
        }

        return auditEventBuilder.build();
    }

    private boolean isSuccessful(final AuditEvent auditEvent) {
        return HttpStatus.valueOf(auditEvent.getStatus()).is2xxSuccessful();
    }

}